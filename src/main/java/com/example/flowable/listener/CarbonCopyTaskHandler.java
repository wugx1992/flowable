package com.example.flowable.listener;

import com.example.flowable.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.api.Task;
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
public class CarbonCopyTaskHandler implements TaskListener {

    public static final String CC_USERS = "carbonCopyUsers";
    public static final String CC_GROUPS = "carbonCopyGroups";

    /**
     * 任务节点的监听
     * TaskListener类实现
     * 说明: usertask节点配置了处理人所以触发assignment事件监听，
     * 当任务运转到usertask的时候触发了create事件。 这里我们也可以得出一个结论：assignment事件比create先执行。
     * 任务完成的时候，触发complete事件，因为任务完成之后，要从ACT_RU_TASK中删除这条记录，所以触发delete事件
     * @param delegateTask
     */
    @Override
    public void notify(DelegateTask delegateTask) {
        Map<String,Object> variables = delegateTask.getVariables();
        log.info("variables：{}", variables);

        String eventName = delegateTask.getEventName();
        //create、assignment、complete、delete
        log.info("task eventName：{}，id：{}, name：{}", eventName, delegateTask.getId(), delegateTask.getName());

        //创建完之后，从spring容器中获取到taskService将任务设置为完成
        if("create".equals(eventName)) {
            if(variables.containsKey(CC_USERS) && !StringUtils.isEmpty(variables.get(CC_USERS))){
                String[] values = ((String) variables.get(CC_USERS)).split(",");
                if(values.length==1){
                    delegateTask.setAssignee(values[0]);
                }else{
                    List<String> valueList = Arrays.asList(values);
                    delegateTask.addCandidateUsers(valueList);
                }
            }
            if(variables.containsKey(CC_GROUPS) && !StringUtils.isEmpty(variables.get(CC_GROUPS))){
                String[] values = ((String) variables.get(CC_GROUPS)).split(",");
                List<String> valueList = Arrays.asList(values);
                delegateTask.addCandidateGroups(valueList);
            }

            TaskService taskService = SpringUtil.getBean(TaskService.class);
            taskService.complete(delegateTask.getId());
        }
    }
}
