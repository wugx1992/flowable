package com.example.flowable.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.variable.api.persistence.entity.VariableInstance;

import java.util.Map;

/**
 * @Author: gx.wu
 * @Date: 2021/1/26 15:08
 * @Description: code something to describe this module what it is
 */
@Slf4j
public class ApproveHandleDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, VariableInstance> variableInstances = execution.getVariableInstances();
            Map<String, Object> variables = execution.getVariables();
            log.info("variableInstances：{}", objectMapper.writeValueAsString(variableInstances));
            log.info("variables：{}", objectMapper.writeValueAsString(variables));

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
