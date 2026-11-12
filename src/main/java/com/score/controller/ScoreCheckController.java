package com.score.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.score.common.ClientIpResolver;
import com.score.common.ResultVo;
import com.score.entity.Admin;
import com.score.entity.bo.AdminBo;
import com.score.entity.bo.StudentQueryScoreBo;
import com.score.entity.vo.ImportMappingVo;
import com.score.service.AdminGuardService;
import com.score.service.ExcelImportService;
import com.score.service.IScoreService;
import com.score.service.ImportType;
import com.score.service.LoginGuardService;
import com.score.service.TermInfo;
import com.score.service.TermService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/scoreCheck")
@Slf4j
public class ScoreCheckController {

    @Autowired
    private IScoreService scoreService;
    @Autowired
    private ExcelImportService excelImportService;
    @Autowired
    private LoginGuardService loginGuardService;
    @Autowired
    private TermService termService;
    @Autowired
    private AdminGuardService adminGuardService;
    @Autowired
    private ObjectMapper objectMapper;

    /** 当前登录账号信息（角色以数据库为准）。 */
    @PostMapping("/updateStudent")
    public ResultVo<?> updateStudent(@RequestBody com.score.entity.bo.StudentContactBo bo) {
        return scoreService.updateStudentContact(bo);
    }

    @GetMapping("/me")
    public ResultVo<?> me() {
        Admin admin = adminGuardService.currentUser();
        Map<String, Object> data = new HashMap<>();
        data.put("userName", admin.getUserName());
        data.put("className", admin.getClassName());
        data.put("role", AdminGuardService.roleOf(admin));
        return ResultVo.success(data);
    }

    /** 学期列表：学生查分页和教师端都需要，无需登录。 */
    @GetMapping("/terms")
    public ResultVo<?> terms() {
        return ResultVo.success(termService.listView());
    }

    @PostMapping("/StudentCheck")
    public ResultVo<?> studentQuery(@Valid @RequestBody StudentQueryScoreBo bo) {
        log.info("学生：{}{}|查询成绩",bo.getStudentName(),bo.getClassName());
        return scoreService.queryScore(bo);
    }

    @PostMapping("/login")
    public ResultVo<?> login(@Valid @RequestBody AdminBo bo){
        log.info("教师：{}|登录系统", bo.getUserName());
        return scoreService.login(bo);
    }

    /**
     * 登录前查询是否需要滑块验证或已被限速。
     */
    @GetMapping("/loginPolicy")
    public ResultVo<?> loginPolicy(@RequestParam(required = false) String userName,
                                   HttpServletRequest request) {
        String ip = ClientIpResolver.resolve(request);
        Map<String, Object> data = new HashMap<>();
        data.put("rateLimited", loginGuardService.isRateLimited(ip));
        data.put("captchaRequired", loginGuardService.isCaptchaRequired(userName, ip));
        return ResultVo.success(data);
    }

    @PostMapping("/checkList")
    public ResultVo<?> checkList(@Valid @RequestBody StudentQueryScoreBo searchBo){
        log.info("教师：{} | 查询成绩", StpUtil.getSession().get("userName"));
        return scoreService.checkList(searchBo);
    }

    @PostMapping("/importStudent")
    public ResultVo<?> importStudent(@RequestParam("file") MultipartFile file,@RequestParam("className") String className){
        log.info("导入班级：{}|学生信息",StpUtil.getSession().get("userName"), className);
        return scoreService.importStudent(file,className);
    }

    @PostMapping("/importScore")
    public ResultVo<?> importScore(@RequestParam("file") MultipartFile file, @RequestParam("examName") String examName, @RequestParam("className") String className){
        log.info("导入考试：{}|成绩数据", StpUtil.getSession().get("userName"), examName);
        return scoreService.importScores(file,examName,className);
    }

    @PostMapping("/import/student/preview")
    public ResultVo<?> previewStudent(@RequestParam("file") MultipartFile file,
                                      @RequestParam("className") String className,
                                      @RequestParam(value = "academicYear", required = false) String academicYear,
                                      @RequestParam(value = "semester", required = false) String semester,
                                      @RequestParam(value = "mapping", required = false) String mappingJson) {
        requireText(className, "请填写班级");
        TermInfo term = termService.resolve(academicYear, semester);
        log.info("预览学生导入：{}|{}", className, TermService.displayName(term.academicYear(), term.semester()));
        return ResultVo.success(excelImportService.preview(
                file, ImportType.STUDENT, className, null, parseMapping(mappingJson), term));
    }

    @PostMapping("/import/score/preview")
    public ResultVo<?> previewScore(@RequestParam("file") MultipartFile file,
                                    @RequestParam("examName") String examName,
                                    @RequestParam("className") String className,
                                    @RequestParam(value = "academicYear", required = false) String academicYear,
                                    @RequestParam(value = "semester", required = false) String semester,
                                    @RequestParam(value = "mapping", required = false) String mappingJson) {
        requireText(examName, "请填写考试名称");
        requireText(className, "请填写班级");
        TermInfo term = termService.resolve(academicYear, semester);
        log.info("预览成绩导入：{}|{}|{}", examName, className,
                TermService.displayName(term.academicYear(), term.semester()));
        return ResultVo.success(excelImportService.preview(
                file, ImportType.SCORE, className, examName, parseMapping(mappingJson), term));
    }

    @PostMapping("/import/student/commit")
    public ResultVo<?> commitStudent(@RequestParam("file") MultipartFile file,
                                     @RequestParam("className") String className,
                                     @RequestParam(value = "academicYear", required = false) String academicYear,
                                     @RequestParam(value = "semester", required = false) String semester,
                                     @RequestParam(value = "mapping", required = false) String mappingJson,
                                     @RequestParam(value = "ignoreErrors", defaultValue = "false") boolean ignoreErrors) {
        requireText(className, "请填写班级");
        TermInfo term = termService.resolve(academicYear, semester);
        log.info("提交学生导入：{}|{}|忽略问题行={}", className,
                TermService.displayName(term.academicYear(), term.semester()), ignoreErrors);
        return ResultVo.success(excelImportService.commit(
                file, ImportType.STUDENT, className, null, parseMapping(mappingJson), ignoreErrors, term));
    }

    @PostMapping("/import/score/commit")
    public ResultVo<?> commitScore(@RequestParam("file") MultipartFile file,
                                   @RequestParam("examName") String examName,
                                   @RequestParam("className") String className,
                                   @RequestParam(value = "academicYear", required = false) String academicYear,
                                   @RequestParam(value = "semester", required = false) String semester,
                                   @RequestParam(value = "mapping", required = false) String mappingJson,
                                   @RequestParam(value = "ignoreErrors", defaultValue = "false") boolean ignoreErrors) {
        requireText(examName, "请填写考试名称");
        requireText(className, "请填写班级");
        TermInfo term = termService.resolve(academicYear, semester);
        log.info("提交成绩导入：{}|{}|{}|忽略问题行={}", examName, className,
                TermService.displayName(term.academicYear(), term.semester()), ignoreErrors);
        return ResultVo.success(excelImportService.commit(
                file, ImportType.SCORE, className, examName, parseMapping(mappingJson), ignoreErrors, term));
    }

    private ImportMappingVo parseMapping(String mappingJson) {
        if (mappingJson == null || mappingJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(mappingJson, ImportMappingVo.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("列映射格式不正确，请重新解析文件");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    @PostMapping("/creatAdmin")
    public ResultVo<?> creatAdmin(@Valid @RequestBody AdminBo adminBo){
        log.info("教师：{}|创建用户:{}",StpUtil.getSession().get("userName"), adminBo.getUserName());
        return scoreService.creatAdmin(adminBo);
    }

    @GetMapping("/getUserList")
    public ResultVo<?> getUserList(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return scoreService.getUserList(pageNum, pageSize);
    }

    @PostMapping("/updateUser")
    public ResultVo<?> updateUser(@RequestBody AdminBo adminBo) {
        return scoreService.updateUser(adminBo);
    }

    @DeleteMapping("/{userName}")
    public ResultVo<?> deleteUser(@PathVariable String userName) {
        return scoreService.deleteUser(userName);
    }

    @PostMapping("/logout")
    public ResultVo<?> logout() {
        StpUtil.logout();
        return ResultVo.success("退出登录成功");
    }

    /**
     * 学生列表分页查询
     */
    @PostMapping("/getStudent")
    public ResultVo<?> getStudent(
            @RequestBody(required = false) AdminBo adminBo,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("教师:{}|查询学生列表", adminBo != null ? adminBo.getUserName() : "unknown");
        return scoreService.getStudent(adminBo, pageNum, pageSize);
    }
}
