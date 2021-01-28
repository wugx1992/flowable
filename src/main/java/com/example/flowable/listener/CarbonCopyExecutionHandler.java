package com.example.flowable.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Author: gx.wu
 * @Date: 2021/1/27 14:58
 * @Description: 抄送
 */

@Slf4j
public class CarbonCopyExecutionHandler implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        Map<String,Object> variables = execution.getVariables();
        log.info("variables：{}", variables);
        String eventName = execution.getEventName();
        //start（流程开始）、end（流程结束）、take（连线监听器）
        log.info("executionListener eventName：{}", eventName);

//        //通过设置流程变量_FLOWABLE_SKIP_EXPRESSION_ENABLED为true启动skipExpression属性,
//        //必须是true而非字符串"true",若不启动是不生效的
//        execution.setVariable("_FLOWABLE_SKIP_EXPRESSION_ENABLED",true);
//        UserTask userTask = (UserTask)execution.getCurrentFlowElement();
//        userTask.setSkipExpression("${1==1}");
    }
}
