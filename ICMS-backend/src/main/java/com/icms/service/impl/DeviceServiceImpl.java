package com.icms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icms.entity.Device;
import com.icms.mapper.DeviceMapper;
import com.icms.service.DeviceService;
import org.springframework.stereotype.Service;

@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {
}
