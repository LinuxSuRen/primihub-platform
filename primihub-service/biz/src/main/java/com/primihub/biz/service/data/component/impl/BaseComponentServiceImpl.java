package com.primihub.biz.service.data.component.impl;

import com.primihub.biz.convert.DataModelConvert;
import com.primihub.biz.entity.base.BaseResultEntity;
import com.primihub.biz.entity.base.BaseResultEnum;
import com.primihub.biz.entity.data.po.DataModelComponent;
import com.primihub.biz.entity.data.req.ComponentTaskReq;
import com.primihub.biz.entity.data.req.DataComponentRelationReq;
import com.primihub.biz.entity.data.req.DataComponentReq;
import com.primihub.biz.entity.data.req.DataComponentValue;
import com.primihub.biz.entity.data.vo.ModelComponent;
import com.primihub.biz.entity.data.vo.ModelComponentType;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BaseComponentServiceImpl {
    /**
     * Unified input parameter verification
     * @param req
     * @param modelComponents
     * @return
     */
    public BaseResultEntity componentTypeVerification(DataComponentReq req, List<ModelComponent> modelComponents, ComponentTaskReq taskReq){
        if (req==null || req.getComponentValues()==null || req.getComponentValues().isEmpty())
            return BaseResultEntity.failure(BaseResultEnum.DATA_RUN_TASK_FAIL,req.getComponentName()+"无入参数信息");
        ModelComponent modelComponent = modelComponents.stream().filter(mc -> mc.getComponentCode().equals(req.getComponentCode())).findFirst().orElse(null);
        if (modelComponent==null)
            return BaseResultEntity.failure(BaseResultEnum.DATA_RUN_TASK_FAIL,"无法查找到"+req.getComponentName()+"组件信息");
        Map<String, String> valueMap = getComponentVals(req.getComponentValues());
        for (ModelComponentType mct : modelComponent.getComponentTypes()) {
            if (mct.getIsRequired()!=null&&mct.getIsRequired()==1){
                String value = valueMap.get(mct.getTypeCode());
                if (StringUtils.isBlank(value)){
                    return BaseResultEntity.failure(BaseResultEnum.DATA_RUN_TASK_FAIL,"组件["+req.getComponentName()+"]参数["+mct.getTypeName()+"]不可以为空");
                }else {
                    if (mct.getInputValues()!=null && !mct.getInputValues().isEmpty()){
                        if (mct.getInputValues().stream().noneMatch(v -> v.getKey().equals(value))){
                            return BaseResultEntity.failure(BaseResultEnum.DATA_RUN_TASK_FAIL,"组件["+req.getComponentName()+"]参数["+mct.getTypeName()+"]值异常");
                        }
                    }
                }
            }
        }
        List<DataComponentRelationReq> input = req.getInput();
        List<DataComponentRelationReq> output = req.getOutput();
        if (input.size()==0&&output.size()==0){
            return BaseResultEntity.failure(BaseResultEnum.DATA_RUN_TASK_FAIL,req.getComponentName()+"流程不合规");
        }
        if (input.size()>0&&output.size()>0){
            for (DataComponentRelationReq inputReq : input) {
                for (DataComponentRelationReq outReq : output) {
                    taskReq.getDataModelComponents().add(new DataModelComponent(inputReq.getComponentCode(),outReq.getComponentCode(),outReq.getPointType(),outReq.getPointJson()));
                }
            }
        }else if(input.size()>0&&output.size()==0) {
            for (DataComponentRelationReq inputReq : input) {
                taskReq.getDataModelComponents().add(new DataModelComponent(inputReq.getComponentCode(),"",inputReq.getPointType(),inputReq.getPointJson()));
            }
        }else if(input.size()==0&&output.size()>0) {
            for (DataComponentRelationReq outputReq : output) {
                taskReq.getDataModelComponents().add(new DataModelComponent("",outputReq.getComponentCode(),outputReq.getPointType(),outputReq.getPointJson()));
            }
        }
        taskReq.getDataComponents().add(DataModelConvert.dataModelReqConvertDataComponentPo(req));
        return BaseResultEntity.success(valueMap);
    }

    public Map<String,String> getComponentVals(List<DataComponentValue> componentValues){
        return componentValues.stream().collect(Collectors.toMap(DataComponentValue::getKey, DataComponentValue::getVal, (key1, key2) -> key2));
    }
}
