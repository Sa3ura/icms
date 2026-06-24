package com.icms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icms.entity.Campus;
import com.icms.mapper.CampusMapper;
import com.icms.service.CampusService;
import org.springframework.stereotype.Service;

@Service
public class CampusServiceImpl extends ServiceImpl<CampusMapper, Campus> implements CampusService {
}
