package com.primihub.biz.service.data.component.impl;

import com.primihub.biz.config.base.BaseConfiguration;
import com.primihub.biz.entity.base.BaseResultEntity;
import com.primihub.biz.entity.data.po.DataModel;
import com.primihub.biz.entity.data.req.ComponentTaskReq;
import com.primihub.biz.entity.data.req.DataComponentReq;
import com.primihub.biz.service.data.component.ComponentTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("exceptionComponentTaskServiceImpl")
public class ExceptionComponentTaskServiceImpl extends BaseComponentServiceImpl implements ComponentTaskService {
    @Autowired
    private BaseConfiguration baseConfiguration;

    @Override
    public BaseResultEntity check(DataComponentReq req,  ComponentTaskReq taskReq) {
        return componentTypeVerification(req,baseConfiguration.getModelComponents(),taskReq);
    }

    @Override
    public BaseResultEntity runTask(DataComponentReq req, ComponentTaskReq taskReq) {
        return BaseResultEntity.success();
    }
}
