package com.icms.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.icms.entity.*;
import com.icms.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Slf4j
public class DatabaseInitializer implements ApplicationRunner {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private CampusService campusService;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private CourseScheduleService courseScheduleService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            seedUsers();
            seedCampuses();
            seedBuildings();
            seedClassrooms();
            seedDevices();
            seedCourseSchedules();
            seedNotices();
            log.info("[ICMS Backend] Database seeding finished successfully!");
        } catch (Exception e) {
            log.error("[ICMS Backend] Database seeding failed (check database connection)", e);
        }
    }

    private void seedUsers() {
        if (sysUserService.count() == 0) {
            log.info("[ICMS Backend] Seeding users table...");
            String pwd = passwordEncoder.encode("123456");

            createUser("admin", pwd, "系统管理员", "admin", "13800000001", "admin@icms.edu.cn");
            createUser("jw_admin", pwd, "教务管理员", "jw", "13800000002", "jw@icms.edu.cn");
            createUser("teacher_wang", pwd, "王教师", "teacher", "13800000003", "wang@icms.edu.cn");
            createUser("student_li", pwd, "李同学", "student", "13800000004", "li@icms.edu.cn");
            createUser("repairer_zhang", pwd, "张师傅", "repairer", "13800000005", "zhang@icms.edu.cn");
        }
    }

    private void createUser(String username, String pwd, String realName, String type, String phone, String email) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(pwd);
        user.setRealName(realName);
        user.setUserType(type);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        sysUserService.save(user);
    }

    private void seedCampuses() {
        if (campusService.count() == 0) {
            log.info("[ICMS Backend] Seeding campuses table...");
            createCampus(1L, "东校区", "华盛大道100号");
            createCampus(2L, "西校区", "科技路250号");
        }
    }

    private void createCampus(Long id, String name, String address) {
        Campus campus = new Campus();
        campus.setId(id);
        campus.setCampusName(name);
        campus.setAddress(address);
        campus.setStatus(1);
        campus.setCreatedAt(LocalDateTime.now());
        campusService.save(campus);
    }

    private void seedBuildings() {
        if (buildingService.count() == 0) {
            log.info("[ICMS Backend] Seeding buildings table...");
            createBuilding(1L, 1L, "第一教学楼", 5);
            createBuilding(2L, 1L, "实验大楼", 6);
            createBuilding(3L, 2L, "第二教学楼", 4);
        }
    }

    private void createBuilding(Long id, Long campusId, String name, Integer floors) {
        Building b = new Building();
        b.setId(id);
        b.setCampusId(campusId);
        b.setBuildingName(name);
        b.setFloorCount(floors);
        b.setStatus(1);
        b.setCreatedAt(LocalDateTime.now());
        buildingService.save(b);
    }

    private void seedClassrooms() {
        if (classroomService.count() == 0) {
            log.info("[ICMS Backend] Seeding classrooms table...");
            createClassroom(1L, 1L, 1L, 1, "A101", "第一教学楼A101", 80, "multimedia", "available", "08:00-22:00", "配置高清投影与环绕声系统。");
            createClassroom(2L, 1L, 1L, 2, "A203", "第一教学楼A203", 120, "multimedia", "available", "08:00-22:00", "大型多媒体阶梯教室。");
            createClassroom(3L, 1L, 2L, 3, "B305", "实验大楼B305", 50, "lab", "available", "08:30-21:30", "计算机软件开发实验室。");
            createClassroom(4L, 2L, 3L, 1, "C102", "第二教学楼C102", 60, "normal", "available", "08:00-21:30", "普通多功能教室。");
            createClassroom(5L, 1L, 1L, 3, "A301", "第一教学楼A301", 45, "normal", "maintenance", "08:00-22:00", "内部设备升级中，暂停使用。");
        }
    }

    private void createClassroom(Long id, Long campusId, Long buildingId, Integer floor, String roomNo, String name, Integer capacity, String type, String status, String openTime, String desc) {
        Classroom c = new Classroom();
        c.setId(id);
        c.setCampusId(campusId);
        c.setBuildingId(buildingId);
        c.setFloorNo(floor);
        c.setRoomNo(roomNo);
        c.setName(name);
        c.setCapacity(capacity);
        c.setRoomType(type);
        c.setStatus(status);
        c.setOpenTime(openTime);
        c.setDescription(desc);
        c.setCreatedAt(LocalDateTime.now());
        classroomService.save(c);
    }

    private void seedDevices() {
        if (deviceService.count() == 0) {
            log.info("[ICMS Backend] Seeding devices table...");
            createDevice(1L, 1L, "DEV-A101-001", "激光投影仪", "projector", "Epson", "CB-L530U", "normal");
            createDevice(2L, 1L, "DEV-A101-002", "壁挂空调", "air_conditioner", "Gree", "KFR-72LW", "normal");
            createDevice(3L, 2L, "DEV-A203-001", "交互电子白板", "whiteboard", "MAXHUB", "V6-Classic", "normal");
            createDevice(4L, 2L, "DEV-A203-002", "高清云台摄像头", "camera", "Hikvision", "DS-2CD2146G2", "normal");
            createDevice(5L, 3L, "DEV-B305-001", "中央控制台", "controller", "Crestron", "MC4", "normal");
            createDevice(6L, 3L, "DEV-B305-002", "红外防盗门禁", "lock", "ZKTeco", "F22", "fault");
        }
    }

    private void createDevice(Long id, Long roomId, String code, String name, String type, String brand, String model, String status) {
        Device d = new Device();
        d.setId(id);
        d.setClassroomId(roomId);
        d.setDeviceCode(code);
        d.setDeviceName(name);
        d.setDeviceType(type);
        d.setBrand(brand);
        d.setModel(model);
        d.setStatus(status);
        d.setPurchaseDate(LocalDate.now().minusYears(1));
        d.setLastCheckTime(LocalDateTime.now().minusDays(5));
        d.setCreatedAt(LocalDateTime.now());
        deviceService.save(d);
    }

    private void seedCourseSchedules() {
        if (courseScheduleService.count() == 0) {
            log.info("[ICMS Backend] Seeding course schedules table...");
            createSchedule(1L, "2025-2026-2", 1, 1, 1, 2, 1L, "MATH101", "高等数学(下)", 3L, "软件工程2401", 65);
            createSchedule(2L, "2025-2026-2", 1, 1, 3, 4, 2L, "ENG201", "大学英语(II)", 3L, "物联网2402", 90);
            createSchedule(3L, "2025-2026-2", 1, 2, 5, 6, 3L, "CS302", "操作系统实验", 3L, "计科2303", 42);
            createSchedule(4L, "2025-2026-2", 1, 3, 7, 8, 4L, "HIS105", "中国近现代史纲要", 3L, "智能科学2501", 55);
        }
    }

    private void createSchedule(Long id, String sem, Integer week, Integer weekday, Integer start, Integer end, Long roomId, String code, String name, Long teacherId, String className, Integer count) {
        CourseSchedule s = new CourseSchedule();
        s.setId(id);
        s.setSemester(sem);
        s.setWeekNo(week);
        s.setWeekday(weekday);
        s.setStartSection(start);
        s.setEndSection(end);
        s.setClassroomId(roomId);
        s.setCourseCode(code);
        s.setCourseName(name);
        s.setTeacherId(teacherId);
        s.setClassName(className);
        s.setStudentCount(count);
        s.setStatus("published");
        s.setCreatedAt(LocalDateTime.now());
        courseScheduleService.save(s);
    }

    private void seedNotices() {
        if (noticeService.count() == 0) {
            log.info("[ICMS Backend] Seeding notices table...");
            createNotice("关于2026年端午节期间教学楼开放时间调整的通知", "端午节放假期间（6月20日-6月22日），除第一教学楼正常开放（08:00-18:00）外，其他教学楼闭楼维护。特此通知。", "system", null);
            createNotice("实验室大功率设备使用安全警告", "请各教研室、班级在实验大楼B区使用大功率设备前，务必检查空气开关及地线。下课时请务必断电。", "warning", "teacher");
        }
    }

    private void createNotice(String title, String content, String type, String target) {
        Notice n = new Notice();
        n.setTitle(title);
        n.setContent(content);
        n.setNoticeType(type);
        n.setTargetRole(target);
        n.setPublisherId(1L);
        n.setStatus("published");
        n.setCreatedAt(LocalDateTime.now());
        noticeService.save(n);
    }
}
