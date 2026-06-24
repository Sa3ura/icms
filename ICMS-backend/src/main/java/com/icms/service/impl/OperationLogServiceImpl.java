package com.icms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.icms.entity.OperationLog;
import com.icms.mapper.OperationLogMapper;
import com.icms.service.OperationLogService;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {
}
