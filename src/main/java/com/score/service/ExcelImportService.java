package com.score.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.score.entity.Score;
import com.score.entity.Student;
import com.score.entity.vo.ImportColumnVo;
import com.score.entity.vo.ImportMappingVo;
import com.score.entity.vo.ImportPreviewRowVo;
import com.score.entity.vo.ImportPreviewVo;
import com.score.entity.vo.ImportResultVo;
import com.score.entity.vo.ImportRowErrorVo;
import com.score.mapper.ScoreMapper;
import com.score.mapper.StudentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Excel 导入：宽松识别表头、预览校验、确认后写入。
 */
@Slf4j
@Service
public class ExcelImportService {

    private static final int MAX_ROWS = 20000;
    private static final int PREVIEW_ROW_LIMIT = 200;
    private static final int PREVIEW_ERROR_LIMIT = 100;
    private static final BigDecimal MAX_SCORE = new BigDecimal("9999.99");
    private static final Pattern ID_CARD = Pattern.compile("^\\d{17}[\\dX]$");

    private static final Set<String> NAME_HEADERS = Set.of(
            "姓名", "学生姓名", "名字", "学生", "学生名", "name", "studentname", "student");
    private static final Set<String> ID_HEADERS = Set.of(
            "身份证", "身份证号", "身份证号码", "证件号", "证件号码", "idcard", "id");
    private static final Set<String> CLASS_HEADERS = Set.of(
            "班级", "班别", "班", "class", "classname");
    private static final Set<String> RESERVED_HEADERS = Set.of(
            "序号", "学号", "总分", "总成绩", "平均分", "备注", "排名", "名次", "等级",
            "年级", "考试", "考试名称", "考场", "座位号", "性别", "电话", "联系方式",
            "班级", "班别", "班", "class", "classname",
            "身份证", "身份证号", "身份证号码", "证件号", "证件号码", "idcard", "id",
            "姓名", "学生姓名", "名字", "学生", "学生名", "name", "studentname", "student");

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private ScoreMapper scoreMapper;

    @Autowired
    private AiColumnMapper aiColumnMapper;

    public ImportPreviewVo preview(MultipartFile file, ImportType type, String className,
                                   String examName, ImportMappingVo requestedMapping, TermInfo term) {
        className = trim(className);
        examName = trim(examName);
        if (!StringUtils.hasText(className)) {
            throw new IllegalArgumentException("请填写班级");
        }
        if (type == ImportType.SCORE && !StringUtils.hasText(examName)) {
            throw new IllegalArgumentException("请填写考试名称");
        }
        SheetData sheet = readSheet(file);
        ImportMappingVo mapping;
        String source;
        if (requestedMapping != null) {
            mapping = sanitizeMapping(requestedMapping, sheet.headers);
            source = "manual";
        } else {
            mapping = suggestMapping(sheet.headers, type);
            source = "rule";
        }

        boolean aiEnabled = aiColumnMapper.isEnabled();
        if (aiEnabled && requestedMapping == null
                && (mapping.getNameColumn() == null
                || (type == ImportType.STUDENT && mapping.getIdColumn() == null)
                || (type == ImportType.SCORE && mapping.getSubjectColumns().isEmpty()))) {
            Optional<ImportMappingVo> aiMapping = aiColumnMapper.suggest(
                    type, new ArrayList<>(sheet.headers.values()), sampleRows(sheet));
            if (aiMapping.isPresent()) {
                mapping = sanitizeMapping(aiMapping.get(), sheet.headers);
                source = "ai";
            }
        }

        ImportPreviewVo vo = new ImportPreviewVo();
        vo.setColumns(columns(sheet));
        vo.setMapping(mapping);
        vo.setMappingSource(source);
        vo.setAiEnabled(aiEnabled);
        vo.getHeaderErrors().addAll(headerErrors(sheet.headers, mapping, type));
        if (!vo.getHeaderErrors().isEmpty()) {
            return vo;
        }

        ValidationOutcome outcome = validateRows(sheet, mapping, type, className, term);
        vo.setTotalRows(outcome.totalRows);
        vo.setValidRows(outcome.validRows);
        vo.setInvalidRows(outcome.invalidRows);
        for (int i = 0; i < outcome.rows.size() && i < PREVIEW_ROW_LIMIT; i++) {
            vo.getRows().add(toPreviewRow(outcome.rows.get(i)));
        }
        int errorCount = 0;
        for (ParsedRow row : outcome.rows) {
            if (!row.valid) {
                if (errorCount++ >= PREVIEW_ERROR_LIMIT) {
                    break;
                }
                vo.getErrors().add(new ImportRowErrorVo(row.rowNumber, row.message));
            }
        }
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportResultVo commit(MultipartFile file, ImportType type, String className,
                                 String examName, ImportMappingVo requestedMapping, boolean ignoreErrors,
                                 TermInfo term) {
        className = trim(className);
        examName = trim(examName);
        if (!StringUtils.hasText(className)) {
            throw new IllegalArgumentException("请填写班级");
        }
        if (type == ImportType.SCORE && !StringUtils.hasText(examName)) {
            throw new IllegalArgumentException("请填写考试名称");
        }
        SheetData sheet = readSheet(file);
        ImportMappingVo mapping = requestedMapping == null
                ? suggestMapping(sheet.headers, type)
                : sanitizeMapping(requestedMapping, sheet.headers);

        List<String> headerErrors = headerErrors(sheet.headers, mapping, type);
        if (!headerErrors.isEmpty()) {
            throw new IllegalArgumentException(String.join("；", headerErrors));
        }

        ValidationOutcome outcome = validateRows(sheet, mapping, type, className, term);
        if (outcome.invalidRows > 0 && !ignoreErrors) {
            throw new IllegalArgumentException("有 " + outcome.invalidRows
                    + " 行存在问题，未导入。请修正后重试，或选择只导入有效行。");
        }

        int inserted = 0;
        int updated = 0;
        if (type == ImportType.STUDENT) {
            for (ParsedRow row : outcome.rows) {
                if (!row.valid) {
                    continue;
                }
                Student existing = findByStudentIdCard(row.idCard, term);
                if (existing != null) {
                    existing.setStudentName(row.studentName);
                    existing.setClassName(className);
                    existing.setAcademicYear(term.academicYear());
                    existing.setSemester(term.semester());
                    existing.setDelFlag("0");
                    studentMapper.updateById(existing);
                    updated++;
                } else {
                    Student student = new Student();
                    student.setStudentName(row.studentName);
                    student.setIdCard(row.idCard);
                    student.setClassName(className);
                    student.setAcademicYear(term.academicYear());
                    student.setSemester(term.semester());
                    student.setDelFlag("0");
                    student.setCreateTime(new Date());
                    studentMapper.insert(student);
                    inserted++;
                }
            }
        } else {
            for (ParsedRow row : outcome.rows) {
                if (!row.valid) {
                    continue;
                }
                for (Map.Entry<String, BigDecimal> entry : row.scores.entrySet()) {
                    Score existing = findScore(examName, className, row.idCard, entry.getKey(), term);
                    if (existing != null) {
                        existing.setStudentName(row.studentName);
                        existing.setScore(entry.getValue());
                        existing.setAcademicYear(term.academicYear());
                        existing.setSemester(term.semester());
                        existing.setCreateTime(new Date());
                        scoreMapper.updateById(existing);
                        updated++;
                    } else {
                        Score score = new Score();
                        score.setStudentName(row.studentName);
                        score.setIdCard(row.idCard);
                        score.setClassName(className);
                        score.setAcademicYear(term.academicYear());
                        score.setSemester(term.semester());
                        score.setExamName(examName);
                        score.setSubject(entry.getKey());
                        score.setScore(entry.getValue());
                        score.setDelFlag("0");
                        score.setCreateTime(new Date());
                        scoreMapper.insert(score);
                        inserted++;
                    }
                }
            }
        }

        ImportResultVo result = new ImportResultVo();
        result.setInserted(inserted);
        result.setUpdated(updated);
        result.setSkipped(outcome.invalidRows);
        String unit = type == ImportType.STUDENT ? "名学生" : "条成绩";
        StringBuilder message = new StringBuilder("导入完成：新增 ").append(inserted)
                .append(" ").append(unit).append("，更新 ").append(updated).append(" ").append(unit);
        if (outcome.invalidRows > 0) {
            message.append("，跳过 ").append(outcome.invalidRows).append(" 行问题数据");
        }
        result.setMessage(message.toString());
        return result;
    }

    private SheetData readSheet(MultipartFile file) {
        if (!isExcelFile(file)) {
            throw new IllegalArgumentException("请上传 Excel 文件（.xlsx/.xls）");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("读取文件失败，请重新选择文件");
        }
        if (bytes.length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小不能超过 10MB");
        }

        List<Map<Integer, String>> rows;
        try {
            rows = EasyExcel.read(new ByteArrayInputStream(bytes))
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (Exception e) {
            throw new IllegalArgumentException("解析 Excel 失败，请确认文件格式正确");
        }
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Excel 文件为空");
        }
        if (rows.size() > MAX_ROWS + 10) {
            throw new IllegalArgumentException("文件行数过多，最多支持 " + MAX_ROWS + " 行");
        }

        int headerIndex = detectHeaderRow(rows);
        Map<Integer, String> rawHeaders = rows.get(headerIndex);
        Map<Integer, String> headers = new TreeMap<>();
        for (Map.Entry<Integer, String> entry : rawHeaders.entrySet()) {
            headers.put(entry.getKey(), trim(entry.getValue()));
        }
        if (headers.isEmpty()) {
            throw new IllegalArgumentException("未找到表头，请确认第一行是列名");
        }

        SheetData sheet = new SheetData();
        sheet.headerIndex = headerIndex;
        sheet.headers = new LinkedHashMap<>(headers);
        sheet.dataRows = rows.subList(headerIndex + 1, rows.size());
        return sheet;
    }

    private int detectHeaderRow(List<Map<Integer, String>> rows) {
        int best = 0;
        int bestScore = -1;
        int limit = Math.min(10, rows.size());
        for (int i = 0; i < limit; i++) {
            int score = 0;
            for (String value : rows.get(i).values()) {
                String normalized = normalize(value);
                if (normalized.isEmpty()) {
                    continue;
                }
                if (NAME_HEADERS.contains(normalized)) {
                    score += 6;
                } else if (ID_HEADERS.contains(normalized)) {
                    score += 5;
                } else if (CLASS_HEADERS.contains(normalized)) {
                    score += 3;
                } else {
                    score += 1;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private ImportMappingVo suggestMapping(Map<Integer, String> headers, ImportType type) {
        ImportMappingVo mapping = new ImportMappingVo();
        mapping.setSubjectColumns(new ArrayList<>());
        mapping.setIgnoredColumns(new ArrayList<>());
        for (Map.Entry<Integer, String> entry : headers.entrySet()) {
            int index = entry.getKey();
            String normalized = normalize(entry.getValue());
            if (normalized.isEmpty()) {
                mapping.getIgnoredColumns().add(index);
            } else if (NAME_HEADERS.contains(normalized) && mapping.getNameColumn() == null) {
                mapping.setNameColumn(index);
            } else if (ID_HEADERS.contains(normalized) && mapping.getIdColumn() == null) {
                mapping.setIdColumn(index);
            } else if (type == ImportType.SCORE && !RESERVED_HEADERS.contains(normalized)) {
                mapping.getSubjectColumns().add(index);
            } else {
                mapping.getIgnoredColumns().add(index);
            }
        }
        return mapping;
    }

    private ImportMappingVo sanitizeMapping(ImportMappingVo source, Map<Integer, String> headers) {
        ImportMappingVo mapping = new ImportMappingVo();
        mapping.setSubjectColumns(new ArrayList<>());
        mapping.setIgnoredColumns(new ArrayList<>());
        mapping.setNameColumn(validIndex(source.getNameColumn(), headers));
        mapping.setIdColumn(validIndex(source.getIdColumn(), headers));

        Set<Integer> subjects = new HashSet<>();
        if (source.getSubjectColumns() != null) {
            for (Integer index : source.getSubjectColumns()) {
                Integer valid = validIndex(index, headers);
                if (valid != null
                        && !valid.equals(mapping.getNameColumn())
                        && !valid.equals(mapping.getIdColumn())) {
                    subjects.add(valid);
                }
            }
        }
        mapping.getSubjectColumns().addAll(subjects);
        mapping.getSubjectColumns().sort(Integer::compareTo);

        for (Integer index : headers.keySet()) {
            if (!index.equals(mapping.getNameColumn())
                    && !index.equals(mapping.getIdColumn())
                    && !subjects.contains(index)) {
                mapping.getIgnoredColumns().add(index);
            }
        }
        return mapping;
    }

    private Integer validIndex(Integer index, Map<Integer, String> headers) {
        return index != null && headers.containsKey(index) ? index : null;
    }

    private List<String> headerErrors(Map<Integer, String> headers, ImportMappingVo mapping, ImportType type) {
        List<String> errors = new ArrayList<>();
        if (mapping.getNameColumn() == null) {
            errors.add("未识别到姓名列，请手动选择");
        }
        if (type == ImportType.STUDENT && mapping.getIdColumn() == null) {
            errors.add("未识别到身份证列，请手动选择");
        }
        if (type == ImportType.SCORE && mapping.getSubjectColumns().isEmpty()) {
            errors.add("未识别到科目列，请手动选择");
        }

        int nameColumns = 0;
        int idColumns = 0;
        Map<String, Integer> subjectCount = new HashMap<>();
        for (Map.Entry<Integer, String> entry : headers.entrySet()) {
            String normalized = normalize(entry.getValue());
            if (NAME_HEADERS.contains(normalized)) {
                nameColumns++;
            }
            if (ID_HEADERS.contains(normalized)) {
                idColumns++;
            }
            if (type == ImportType.SCORE && mapping.getSubjectColumns().contains(entry.getKey())) {
                String header = entry.getValue();
                if (StringUtils.hasText(header)) {
                    subjectCount.merge(header, 1, Integer::sum);
                }
            }
        }
        if (nameColumns > 1) {
            errors.add("姓名列不唯一，请保留一列");
        }
        if (idColumns > 1) {
            errors.add("身份证列不唯一，请保留一列");
        }
        subjectCount.forEach((header, count) -> {
            if (count > 1) {
                errors.add("科目列重复：" + header);
            }
        });
        return errors;
    }

    private ValidationOutcome validateRows(SheetData sheet, ImportMappingVo mapping,
                                           ImportType type, String className, TermInfo term) {
        ValidationOutcome outcome = new ValidationOutcome();
        Map<String, Student> studentsById = new HashMap<>();
        Map<String, List<Student>> studentsByName = new HashMap<>();
        if (type == ImportType.SCORE) {
            loadStudents(className, term, studentsById, studentsByName);
        }

        Set<String> seenIds = new HashSet<>();
        for (int i = 0; i < sheet.dataRows.size(); i++) {
            Map<Integer, String> raw = sheet.dataRows.get(i);
            if (isBlankRow(raw)) {
                continue;
            }
            ParsedRow row = new ParsedRow();
            row.rowNumber = sheet.headerIndex + i + 2;
            row.studentName = trim(raw.get(mapping.getNameColumn()));
            row.idCard = mapping.getIdColumn() == null ? "" : trim(raw.get(mapping.getIdColumn()));
            row.className = className;

            List<String> errors = new ArrayList<>();
            if (!StringUtils.hasText(row.studentName)) {
                errors.add("姓名为空");
            } else if (row.studentName.length() > 64) {
                errors.add("姓名超过 64 个字符");
            }

            if (type == ImportType.STUDENT) {
                if (!StringUtils.hasText(row.idCard)) {
                    errors.add("身份证号为空");
                } else {
                    row.idCard = normalizeIdCard(row.idCard);
                    if (!ID_CARD.matcher(row.idCard).matches()) {
                        errors.add("身份证号应为 18 位（最后一位可为 X），请将身份证列设为文本");
                    } else if (!seenIds.add(row.idCard)) {
                        errors.add("身份证号在文件中重复");
                    }
                }
            } else {
                Student student = resolveStudent(row, errors, studentsById, studentsByName);
                if (student != null) {
                    row.idCard = student.getIdCard();
                    row.className = student.getClassName();
                }
                for (Integer subjectIndex : mapping.getSubjectColumns()) {
                    String subject = trim(sheet.headers.get(subjectIndex));
                    String value = trim(raw.get(subjectIndex));
                    if (!StringUtils.hasText(value)) {
                        continue;
                    }
                    if (!StringUtils.hasText(subject)) {
                        errors.add("第 " + (subjectIndex + 1) + " 列缺少科目名");
                        continue;
                    }
                    if (subject.length() > 64) {
                        errors.add("科目名超过 64 个字符：" + subject);
                        continue;
                    }
                    try {
                        BigDecimal score = new BigDecimal(value);
                        if (score.compareTo(BigDecimal.ZERO) < 0) {
                            errors.add(subject + " 分数不能为负数");
                        } else if (score.compareTo(MAX_SCORE) > 0) {
                            errors.add(subject + " 分数超出允许范围");
                        } else {
                            row.scores.put(subject, score);
                            row.subjects.put(subject, value);
                        }
                    } catch (NumberFormatException e) {
                        errors.add(subject + " 分数不是有效数字：" + value);
                    }
                }
                if (row.scores.isEmpty() && errors.isEmpty()) {
                    errors.add("没有有效成绩");
                }
            }

            row.valid = errors.isEmpty();
            row.message = String.join("；", errors);
            outcome.rows.add(row);
            outcome.totalRows++;
            if (row.valid) {
                outcome.validRows++;
            } else {
                outcome.invalidRows++;
            }
        }
        return outcome;
    }

    private Student resolveStudent(ParsedRow row, List<String> errors,
                                   Map<String, Student> studentsById,
                                   Map<String, List<Student>> studentsByName) {
        if (StringUtils.hasText(row.idCard)) {
            row.idCard = normalizeIdCard(row.idCard);
            if (!ID_CARD.matcher(row.idCard).matches()) {
                errors.add("身份证号应为 18 位（最后一位可为 X）");
                return null;
            }
            Student student = studentsById.get(row.idCard);
            if (student == null) {
                errors.add("学生不在当前班级档案中");
                return null;
            }
            if (!student.getStudentName().equals(row.studentName)) {
                errors.add("姓名与身份证号不匹配");
                return null;
            }
            return student;
        }

        List<Student> matches = studentsByName.get(row.studentName);
        if (matches == null || matches.isEmpty()) {
            errors.add("学生不在当前班级档案中，请先导入学生");
            return null;
        }
        if (matches.size() > 1) {
            errors.add("班级内存在同名学生，请提供身份证列");
            return null;
        }
        return matches.get(0);
    }

    private void loadStudents(String className, TermInfo term, Map<String, Student> byId,
                              Map<String, List<Student>> byName) {
        LambdaQueryWrapper<Student> query = new LambdaQueryWrapper<>();
        query.eq(Student::getClassName, className)
                .eq(Student::getAcademicYear, term.academicYear())
                .eq(Student::getSemester, term.semester())
                .eq(Student::getDelFlag, "0");
        List<Student> students = studentMapper.selectList(query);
        for (Student student : students) {
            byId.put(student.getIdCard(), student);
            byName.computeIfAbsent(student.getStudentName(), key -> new ArrayList<>()).add(student);
        }
    }

    private Student findByStudentIdCard(String idCard, TermInfo term) {
        LambdaQueryWrapper<Student> query = new LambdaQueryWrapper<>();
        query.eq(Student::getIdCard, idCard)
                .eq(Student::getAcademicYear, term.academicYear())
                .eq(Student::getSemester, term.semester())
                .eq(Student::getDelFlag, "0");
        List<Student> students = studentMapper.selectList(query);
        return students.isEmpty() ? null : students.get(0);
    }

    private Score findScore(String examName, String className, String idCard, String subject, TermInfo term) {
        LambdaQueryWrapper<Score> query = new LambdaQueryWrapper<>();
        query.eq(Score::getExamName, examName)
                .eq(Score::getClassName, className)
                .eq(Score::getIdCard, idCard)
                .eq(Score::getSubject, subject)
                .eq(Score::getAcademicYear, term.academicYear())
                .eq(Score::getSemester, term.semester())
                .eq(Score::getDelFlag, "0");
        List<Score> scores = scoreMapper.selectList(query);
        if (scores.size() > 1) {
            log.warn("发现重复成绩记录：考试={}，班级={}，科目={}，数量={}",
                    examName, className, subject, scores.size());
        }
        return scores.isEmpty() ? null : scores.get(0);
    }

    private List<ImportColumnVo> columns(SheetData sheet) {
        List<ImportColumnVo> columns = new ArrayList<>();
        Map<Integer, String> firstRow = firstDataRow(sheet);
        for (Map.Entry<Integer, String> entry : sheet.headers.entrySet()) {
            ImportColumnVo column = new ImportColumnVo();
            column.setIndex(entry.getKey());
            column.setHeader(entry.getValue());
            column.setSample(maskValue(firstRow.get(entry.getKey())));
            columns.add(column);
        }
        return columns;
    }

    private List<List<String>> sampleRows(SheetData sheet) {
        List<List<String>> samples = new ArrayList<>();
        int count = 0;
        for (Map<Integer, String> row : sheet.dataRows) {
            if (isBlankRow(row)) {
                continue;
            }
            List<String> values = new ArrayList<>();
            for (Integer index : sheet.headers.keySet()) {
                values.add(maskValue(row.get(index)));
            }
            samples.add(values);
            if (++count >= 3) {
                break;
            }
        }
        return samples;
    }

    private Map<Integer, String> firstDataRow(SheetData sheet) {
        for (Map<Integer, String> row : sheet.dataRows) {
            if (!isBlankRow(row)) {
                return row;
            }
        }
        return new HashMap<>();
    }

    private ImportPreviewRowVo toPreviewRow(ParsedRow row) {
        ImportPreviewRowVo vo = new ImportPreviewRowVo();
        vo.setRowNumber(row.rowNumber);
        vo.setStudentName(row.studentName);
        vo.setIdCard(maskId(row.idCard));
        vo.setClassName(row.className);
        vo.setSubjects(new LinkedHashMap<>(row.subjects));
        vo.setValid(row.valid);
        vo.setMessage(row.message);
        return vo;
    }

    private boolean isBlankRow(Map<Integer, String> row) {
        if (row == null) {
            return true;
        }
        for (String value : row.values()) {
            if (StringUtils.hasText(value)) {
                return false;
            }
        }
        return true;
    }

    private boolean isExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                return true;
            }
        }
        String contentType = file.getContentType();
        return contentType != null
                && (contentType.contains("excel") || contentType.contains("spreadsheetml"));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s\\u3000\\*＊:：]", "").toLowerCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeIdCard(String value) {
        return value == null ? "" : value.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
    }

    private String maskId(String idCard) {
        if (!StringUtils.hasText(idCard)) {
            return "";
        }
        if (idCard.length() >= 15) {
            return idCard.substring(0, 3) + "*************" + idCard.substring(idCard.length() - 2);
        }
        return maskValue(idCard);
    }

    private String maskValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String text = value.trim();
        if (ID_CARD.matcher(text).matches()) {
            return maskId(text);
        }
        if (text.length() <= 1) {
            return "*";
        }
        if (text.length() <= 4) {
            return text.charAt(0) + "**";
        }
        return text.charAt(0) + "***" + text.charAt(text.length() - 1);
    }

    private static class SheetData {
        private int headerIndex;
        private Map<Integer, String> headers;
        private List<Map<Integer, String>> dataRows;
    }

    private static class ParsedRow {
        private int rowNumber;
        private String studentName;
        private String idCard;
        private String className;
        private final Map<String, String> subjects = new LinkedHashMap<>();
        private final Map<String, BigDecimal> scores = new LinkedHashMap<>();
        private boolean valid;
        private String message;
    }

    private static class ValidationOutcome {
        private final List<ParsedRow> rows = new ArrayList<>();
        private int totalRows;
        private int validRows;
        private int invalidRows;
    }
}
