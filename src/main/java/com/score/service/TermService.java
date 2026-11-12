package com.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.score.entity.AcademicTerm;
import com.score.mapper.AcademicTermMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 学年学期：当前学期、学期列表和选择校验。
 */
@Service
public class TermService {

    private static final Pattern YEAR_PATTERN = Pattern.compile("^\\d{4}-\\d{4}$");

    @Autowired
    private AcademicTermMapper termMapper;

    /** 当前学期；未配置时取最早的一个学期。 */
    public TermInfo current() {
        AcademicTerm term = termMapper.selectOne(new LambdaQueryWrapper<AcademicTerm>()
                .eq(AcademicTerm::getDelFlag, "0")
                .eq(AcademicTerm::getIsCurrent, true)
                .orderByDesc(AcademicTerm::getId)
                .last("limit 1"));
        if (term == null) {
            List<AcademicTerm> all = listEntities();
            term = all.isEmpty() ? null : all.get(0);
        }
        if (term == null) {
            throw new IllegalStateException("尚未配置学年学期，请先在「设置」中创建");
        }
        return new TermInfo(term.getAcademicYear(), term.getSemester());
    }

    /** 解析请求里的学年学期；为空时使用当前学期，不存在时拒绝。 */
    public TermInfo resolve(String academicYear, String semester) {
        if (!StringUtils.hasText(academicYear) && !StringUtils.hasText(semester)) {
            return current();
        }
        String year = normalizeYear(academicYear);
        String sem = normalizeSemester(semester);
        AcademicTerm term = find(year, sem);
        if (term == null) {
            throw new IllegalArgumentException("学年学期不存在：" + displayName(year, sem));
        }
        return new TermInfo(year, sem);
    }

    public List<Map<String, Object>> listView() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AcademicTerm term : listEntities()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", term.getId());
            item.put("academicYear", term.getAcademicYear());
            item.put("semester", term.getSemester());
            item.put("name", displayName(term.getAcademicYear(), term.getSemester()));
            item.put("current", Boolean.TRUE.equals(term.getIsCurrent()));
            result.add(item);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(String academicYear, String semester, boolean makeCurrent) {
        String year = normalizeYear(academicYear);
        String sem = normalizeSemester(semester);
        if (find(year, sem) != null) {
            throw new IllegalArgumentException("该学期已存在：" + displayName(year, sem));
        }
        AcademicTerm term = new AcademicTerm();
        term.setAcademicYear(year);
        term.setSemester(sem);
        term.setIsCurrent(false);
        term.setCreateTime(new Date());
        term.setDelFlag("0");
        termMapper.insert(term);
        if (makeCurrent || listEntities().size() == 1) {
            setCurrent(term.getId());
        }
        return termView(termMapper.selectById(term.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void setCurrent(Long id) {
        AcademicTerm term = termMapper.selectById(id);
        if (term == null || !"0".equals(term.getDelFlag())) {
            throw new IllegalArgumentException("学期不存在");
        }
        termMapper.update(null, new LambdaUpdateWrapper<AcademicTerm>()
                .eq(AcademicTerm::getDelFlag, "0")
                .set(AcademicTerm::getIsCurrent, false));
        termMapper.update(null, new LambdaUpdateWrapper<AcademicTerm>()
                .eq(AcademicTerm::getId, id)
                .set(AcademicTerm::getIsCurrent, true));
    }

    public static String displayName(String academicYear, String semester) {
        String sem = "2".equals(semester) ? "第二学期" : "第一学期";
        return academicYear + " " + sem;
    }

    private AcademicTerm find(String academicYear, String semester) {
        return termMapper.selectOne(new LambdaQueryWrapper<AcademicTerm>()
                .eq(AcademicTerm::getAcademicYear, academicYear)
                .eq(AcademicTerm::getSemester, semester)
                .eq(AcademicTerm::getDelFlag, "0")
                .last("limit 1"));
    }

    private List<AcademicTerm> listEntities() {
        return termMapper.selectList(new LambdaQueryWrapper<AcademicTerm>()
                .eq(AcademicTerm::getDelFlag, "0")
                .orderByDesc(AcademicTerm::getAcademicYear)
                .orderByDesc(AcademicTerm::getSemester)
                .orderByDesc(AcademicTerm::getId));
    }

    private Map<String, Object> termView(AcademicTerm term) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", term.getId());
        item.put("academicYear", term.getAcademicYear());
        item.put("semester", term.getSemester());
        item.put("name", displayName(term.getAcademicYear(), term.getSemester()));
        item.put("current", Boolean.TRUE.equals(term.getIsCurrent()));
        return item;
    }

    private String normalizeYear(String value) {
        String year = value == null ? "" : value.trim();
        if (!YEAR_PATTERN.matcher(year).matches()) {
            throw new IllegalArgumentException("学年格式应为 2026-2027");
        }
        int start = Integer.parseInt(year.substring(0, 4));
        int end = Integer.parseInt(year.substring(5));
        if (end != start + 1) {
            throw new IllegalArgumentException("学年起止年份应连续，例如 2026-2027");
        }
        return year;
    }

    private String normalizeSemester(String value) {
        String sem = value == null ? "" : value.trim();
        if (!"1".equals(sem) && !"2".equals(sem)) {
            throw new IllegalArgumentException("学期只能是第一学期或第二学期");
        }
        return sem;
    }
}
