package com.example.flowable.controller;

import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Controller
@RequestMapping(value = "leave")
public class LeaveController {

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessEngine processEngine;

    /**
     * @description 启动流程
     */
    @RequestMapping(value = "startLeaveProcess")
    @ResponseBody
    public String startLeaveProcess(String userId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Leave", map);
        StringBuilder sb = new StringBuilder();
        sb.append("创建请假流程 processId：" + processInstance.getId());
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
        for (Task task : tasks) {
            sb.append("任务taskId:" + task.getId());
        }
        log.info("userId：{}，processId：{}，name：{}", userId, processInstance.getId(), processInstance.getName());
        return sb.toString();
    }

    /**
     * @param taskId
     * @description 批准
     */
    @RequestMapping(value = "applyTask")
    @ResponseBody
    public String applyTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("流程不存在");
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("checkResult", "通过");
        taskService.complete(taskId, map);
        return "申请审核通过~";
    }

    /**
     * @param taskId
     * @description 驳回
     */
    @ResponseBody
    @RequestMapping(value = "rejectTask")
    public String rejectTask(String taskId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("checkResult", "驳回");
        taskService.complete(taskId, map);
        return "申请审核驳回~";
    }
}
