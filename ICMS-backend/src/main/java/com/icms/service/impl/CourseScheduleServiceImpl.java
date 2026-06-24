package com.icms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icms.entity.CourseSchedule;
import com.icms.mapper.CourseScheduleMapper;
import com.icms.service.CourseScheduleService;
import org.springframework.stereotype.Service;

@Service
public class CourseScheduleServiceImpl extends ServiceImpl<CourseScheduleMapper, CourseSchedule> implements CourseScheduleService {
}
