package com.score.service.impl;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.score.common.ClientIpResolver;
import com.score.common.ResultVo;
import com.score.entity.Admin;
import com.score.entity.Score;
import com.score.entity.Student;
import com.score.entity.bo.AdminBo;
import com.score.entity.bo.StudentQueryScoreBo;
import com.score.entity.vo.ImportResultVo;
import com.score.mapper.AdminMapper;
import com.score.mapper.ScoreMapper;
import com.score.mapper.StudentMapper;
import com.score.service.AdminGuardService;
import com.score.service.ExcelImportService;
import com.score.service.IScoreService;
import com.score.service.ImportType;
import com.score.service.LoginGuardService;
import com.score.service.TermInfo;
import com.score.service.TermService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author imyiwen
 * @data 2026/4/22 9:36
 */

@Slf4j
@Service
public class ScoreServiceImpl implements IScoreService {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private ScoreMapper scoreMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private ImageCaptchaApplication imageCaptchaApplication;
    @Autowired
    private ExcelImportService excelImportService;
    @Autowired
    private LoginGuardService loginGuardService;
    @Autowired
    private TermService termService;

    @Override
    public ResultVo<?> queryScore(StudentQueryScoreBo bo) {
        if (bo == null || !StringUtils.hasText(bo.getStudentName())) {
            return ResultVo.error("查询失败：姓名不能为空");
        }
        // 校验身份证号格式已由 @Pattern 注解处理
        TermInfo term = termService.resolve(bo.getAcademicYear(), bo.getSemester());
        //身份验证
        if(studentMapper.checkStudent(bo.getStudentName(),bo.getIdCard(),
                term.academicYear(), term.semester())==0){
            return ResultVo.error("姓名或身份证号错误，请检查！");
        }
        LambdaQueryWrapper<Score> lqw = buildScoreQueryWrapper(bo, true, term);
        List<Score> scores = scoreMapper.studentQuery(lqw);
        BigDecimal totalScore = scores.stream().map(s->s.getScore() !=null?s.getScore():BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String,Object> result=new HashMap<>();
        result.put("score",scores);
        result.put("totalScore",totalScore);
        result.put("subjectCount",scores.size());
        result.put("academicYear", term.academicYear());
        result.put("semester", term.semester());
        result.put("termName", TermService.displayName(term.academicYear(), term.semester()));
        return ResultVo.success(result);
    }

    @Override
    public ResultVo<?> checkList(StudentQueryScoreBo bo) {
        // 多租户隔离由拦截器自动注入上下文，插件自动拦截
        TermInfo term = termService.resolve(bo == null ? null : bo.getAcademicYear(),
                bo == null ? null : bo.getSemester());
        LambdaQueryWrapper<Score> lqw =buildScoreQueryWrapper(bo, false, term);
        List<Score> rawList = scoreMapper.selectList(lqw);
        Map<String,Map<String,Object>> groupMap =new LinkedHashMap<>();
        for(Score s:rawList){
            // 用学生姓名+考试名作为分组键
            String rowKey = s.getStudentName() + "|" + s.getExamName();
            Map<String,Object> row =groupMap.computeIfAbsent(rowKey,k->{Map<String,Object> map=new HashMap<>();
            map.put("studentName",s.getStudentName());
            map.put("examName",s.getExamName());
            map.put("className",s.getClassName());
            map.put("createTime",s.getCreateTime());
            map.put("subjects",new HashMap<String, BigDecimal>());
            map.put("totalScore",BigDecimal.ZERO);
            return map;});
            Map<String,BigDecimal> subjects=(Map<String,BigDecimal>)row.get("subjects");
            BigDecimal score = s.getScore() != null ? s.getScore() : BigDecimal.ZERO;
            subjects.put(s.getSubject(), score);
            BigDecimal currentTotal=(BigDecimal)row.get("totalScore");
            row.put("totalScore",currentTotal.add(score));
        }
        return ResultVo.success(new ArrayList<>(groupMap.values()));
    }

    @Override
    public ResultVo<?> login(AdminBo bo) {

        if (bo == null || !StringUtils.hasText(bo.getUserName())||!StringUtils.hasText(bo.getPassword())) {
            return ResultVo.error("登录失败：用户名或密码不能为空");
        }
        ServletRequestAttributes attributes=(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes==null){
            return ResultVo.error("系统繁忙，请稍后再试");
        }
        HttpServletRequest request = attributes.getRequest();
        String clientIp = ClientIpResolver.resolve(request);
        if (loginGuardService.isRateLimited(clientIp)) {
            return ResultVo.error("尝试过于频繁，请稍后再试");
        }
        loginGuardService.recordAttempt(clientIp);

        // 连续失败后才要求滑块验证，首次登录不打扰用户
        if (loginGuardService.isCaptchaRequired(bo.getUserName(), clientIp)) {
            String captchaId = bo.getCaptchaId();
            if (!StringUtils.hasText(captchaId)) {
                return ResultVo.error("需要安全验证，请完成滑块验证");
            }
            Boolean verified = (Boolean) request.getSession().getAttribute("captcha_verified_" + captchaId);
            if (verified == null || !verified) {
                return ResultVo.error("需要安全验证，请完成滑块验证");
            }
            request.getSession().removeAttribute("captcha_verified_" + captchaId);
            log.info("验证码校验成功");
        }

        LambdaQueryWrapper<Admin> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Admin::getUserName, bo.getUserName()).eq(Admin::getDelFlag, "0");
        Admin admin = adminMapper.selectOne(lqw);

        if (admin == null || !BCrypt.checkpw(bo.getPassword(), admin.getPassword())) {
            loginGuardService.recordFailure(bo.getUserName(), clientIp);
            return ResultVo.error("登录失败：用户名或密码错误");
        }
        loginGuardService.recordSuccess(bo.getUserName(), clientIp);

        // 4. 登录成功
        StpUtil.login(admin.getUserName());
        StpUtil.getSession().set("className", admin.getClassName());
        StpUtil.getSession().set("userName",admin.getUserName());
        StpUtil.getSession().set("role", AdminGuardService.roleOf(admin));
        String token = StpUtil.getTokenValue();
        Map<String,Object>result=new HashMap<>();
        result.put("token",token);
        result.put("role", AdminGuardService.roleOf(admin));
        result.put("className", admin.getClassName());
        return ResultVo.success(result);
    }

    @Override
    public ResultVo<?> importStudent(MultipartFile file, String className) {
        ImportResultVo result = excelImportService.commit(
                file, ImportType.STUDENT, className, null, null, false, termService.current());
        return ResultVo.success(result.getMessage());
    }

    @Override
    public ResultVo<?> importScores(MultipartFile file, String examName, String className) {
        ImportResultVo result = excelImportService.commit(
                file, ImportType.SCORE, className, examName, null, false, termService.current());
        return ResultVo.success(result.getMessage());
    }

    @Override
    public ResultVo<?> creatAdmin(AdminBo bo) {
        if(bo==null||!StringUtils.hasText(bo.getUserName())||!StringUtils.hasText(bo.getPassword())||!StringUtils.hasText(bo.getClassName())){
            return ResultVo.error("创建失败：用户名或密码或班级不能为空！");
        }
        LambdaQueryWrapper<Admin> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Admin::getUserName, bo.getUserName()).last("limit 1");
        Admin existing = adminMapper.selectOne(lqw);
        String hashPw = BCrypt.hashpw(bo.getPassword(), BCrypt.gensalt());
        if (existing != null) {
            if ("0".equals(existing.getDelFlag())) {
                return ResultVo.error("创建失败：用户名：【"+bo.getUserName()+"】，已存在！");
            }
            // 用户名唯一索引也覆盖已删除账号，这里直接复用并重置
            existing.setPassword(hashPw);
            existing.setClassName(bo.getClassName());
            existing.setRole(AdminGuardService.ROLE_TEACHER);
            existing.setCreateTime(new Date());
            existing.setDelFlag("0");
            int rows = adminMapper.updateById(existing);
            return rows > 0 ? ResultVo.success("创建成功！") : ResultVo.error("创建失败");
        }
        Admin admin = new Admin();
        admin.setUserName(bo.getUserName());
        admin.setPassword(hashPw);
        admin.setClassName(bo.getClassName());
        admin.setRole(AdminGuardService.ROLE_TEACHER);
        admin.setCreateTime(new Date());
        admin.setDelFlag("0");
        int rows = adminMapper.insert(admin);
        return rows > 0 ? ResultVo.success("创建成功！") : ResultVo.error("创建失败");
    }

    @Override
    public ResultVo<?> getUserList(Integer pageNum, Integer pageSize) {
        Page<Admin> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Admin> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Admin::getDelFlag, "0").orderByDesc(Admin::getCreateTime);
        return ResultVo.success(adminMapper.selectPage(page, lqw));
    }

    @Override
    public ResultVo<?> updateUser(AdminBo bo) {
        if (bo == null || !StringUtils.hasText(bo.getUserName())) {
            return ResultVo.error("更新失败：用户名不能为空");
        }
        String userName = bo.getUserName().trim();
        String className = bo.getClassName() == null ? "" : bo.getClassName().trim();
        if (!StringUtils.hasText(className)) {
            return ResultVo.error("更新失败：班级不能为空");
        }
        if (className.length() > 64) {
            return ResultVo.error("更新失败：班级名称不能超过 64 个字符");
        }

        LambdaQueryWrapper<Admin> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Admin::getUserName, userName).eq(Admin::getDelFlag, "0");
        Admin admin = adminMapper.selectOne(lqw);
        if (admin == null) {
            return ResultVo.error("更新失败：用户不存在");
        }
        admin.setClassName(className);
        int rows = adminMapper.updateById(admin);
        if (rows <= 0) {
            return ResultVo.error("更新失败");
        }
        // 编辑的是当前登录账户时，同步会话中的班级，避免查询仍按旧班级过滤
        Object currentUser = StpUtil.getSession().get("userName");
        if (currentUser != null && userName.equals(currentUser.toString())) {
            StpUtil.getSession().set("className", className);
        }
        return ResultVo.success("更新成功");
    }

    @Override
    public ResultVo<?> deleteUser(String userName) {
        if (!StringUtils.hasText(userName)) {
            return ResultVo.error("删除失败：用户名不能为空");
        }
        LambdaQueryWrapper<Admin> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Admin::getUserName, userName).eq(Admin::getDelFlag, "0");
        Admin admin = adminMapper.selectOne(lqw);
        if (admin == null) {
            return ResultVo.error("删除失败：用户不存在");
        }
        admin.setDelFlag("1");
        int rows = adminMapper.updateById(admin);
        return rows > 0 ? ResultVo.success("删除成功") : ResultVo.error("删除失败");
    }


    private LambdaQueryWrapper<Score> buildScoreQueryWrapper(StudentQueryScoreBo bo, boolean isExact, TermInfo term) {
        LambdaQueryWrapper<Score> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Score::getAcademicYear, term.academicYear())
                .eq(Score::getSemester, term.semester());
        if (bo != null) {
            // 注意：className 的过滤现在由多租户插件自动完成，无需手动拼接
            if (isExact) {
                lqw.eq(StringUtils.hasText(bo.getStudentName()), Score::getStudentName, bo.getStudentName());
                lqw.eq(StringUtils.hasText(bo.getIdCard()), Score::getIdCard, bo.getIdCard());
            } else {
                lqw.like(StringUtils.hasText(bo.getStudentName()), Score::getStudentName, bo.getStudentName());
                lqw.like(StringUtils.hasText(bo.getExamName()), Score::getExamName, bo.getExamName());
            }
        }
        lqw.eq(Score::getDelFlag, "0").orderByDesc(Score::getCreateTime);
        return lqw;
    }

    @Override
    public ResultVo<?> getStudent(AdminBo adminBo, Integer pageNum, Integer pageSize){
        TermInfo term = termService.resolve(
                adminBo == null ? null : adminBo.getAcademicYear(),
                adminBo == null ? null : adminBo.getSemester());
        // 从 session 获取当前教师的班级（多租户隔离）
        String className = (String) StpUtil.getSession().get("className");
        String filterClass = adminBo == null ? null : adminBo.getClassName();

        Page<Student> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Student> lqw = new LambdaQueryWrapper<>();
        lqw.eq(StringUtils.hasText(className), Student::getClassName, className)
           .eq(StringUtils.hasText(filterClass), Student::getClassName, filterClass)
           .eq(Student::getAcademicYear, term.academicYear())
           .eq(Student::getSemester, term.semester())
           .eq(Student::getDelFlag, "0")
           .orderByDesc(Student::getCreateTime);
        return ResultVo.success(studentMapper.selectPage(page, lqw));
    }

    @Override
    public ResultVo<?> updateStudentContact(com.score.entity.bo.StudentContactBo bo) {
        if (bo == null || bo.getId() == null) {
            return ResultVo.error("缺少学生 ID");
        }
        Student student = studentMapper.selectById(bo.getId());
        if (student == null || !"0".equals(student.getDelFlag())) {
            return ResultVo.error("学生不存在或不在本班");
        }
        if (!validPhone(bo.getPhone()) || !validPhone(bo.getParentPhone())) {
            return ResultVo.error("手机号格式不正确");
        }
        if (bo.getPhone() != null) {
            student.setPhone(bo.getPhone().trim());
        }
        if (bo.getParentName() != null) {
            student.setParentName(bo.getParentName().trim());
        }
        if (bo.getParentPhone() != null) {
            student.setParentPhone(bo.getParentPhone().trim());
        }
        if (bo.getParentRelation() != null) {
            student.setParentRelation(bo.getParentRelation().trim());
        }
        int rows = studentMapper.updateById(student);
        return rows > 0 ? ResultVo.success("保存成功") : ResultVo.error("保存失败");
    }

    private boolean validPhone(String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        String phone = value.trim();
        return phone.length() >= 5 && phone.length() <= 20 && phone.matches("[0-9+\\-\\s]+");
    }
}
