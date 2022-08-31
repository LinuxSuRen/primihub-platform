package com.primihub.biz.service.data.component.impl;

import com.primihub.biz.config.base.BaseConfiguration;
import com.primihub.biz.convert.DataModelConvert;
import com.primihub.biz.entity.base.BaseResultEntity;
import com.primihub.biz.entity.base.BaseResultEnum;
import com.primihub.biz.entity.data.po.DataModel;
import com.primihub.biz.entity.data.po.DataModelComponent;
import com.primihub.biz.entity.data.req.ComponentTaskReq;
import com.primihub.biz.entity.data.req.DataComponentRelationReq;
import com.primihub.biz.entity.data.req.DataComponentReq;
import com.primihub.biz.service.data.component.ComponentTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("startComponentTaskServiceImpl")
@Slf4j
public class StartComponentTaskServiceImpl extends BaseComponentServiceImpl implements ComponentTaskService {
    @Autowired
    private BaseConfiguration baseConfiguration;

    @Override
    public BaseResultEntity check(DataComponentReq req,  ComponentTaskReq taskReq) {
        return componentTypeVerification(req,baseConfiguration.getModelComponents(),taskReq);
    }

    @Override
    public BaseResultEntity runTask(DataComponentReq req, ComponentTaskReq taskReq) {
        // Direct return without execution
        return BaseResultEntity.success();
    }
}
