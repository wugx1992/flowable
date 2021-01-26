package com.example.flowable.controller;


import com.example.flowable.vo.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @Author: gx.wu
 * @Date: 2021/1/20 17:59
 * @Description: code something to describe this module what it is
 */
@Slf4j
@RestController
@RequestMapping("/flowable")
public class FlowableController {
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;

    @Autowired
    private ProcessEngine processEngine;

    /**
     * .提交采购订单的审批请求
     *
     * @param userId 用户id
     */
    @PostMapping("/start/{userId}/{purchaseOrderId}")
    public ResultVo startFlow(@PathVariable String userId, @PathVariable String purchaseOrderId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("purchaseOrderId", purchaseOrderId);
        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey("OrderApproval", map);
        String processId = processInstance.getId();
        String name = processInstance.getName();
        log.info("userId：{}，purchaseOrderId：{}，processId：{}，name：{}", userId, purchaseOrderId, processId, name);
        ResultVo result = ResultVo.ok(processId + ":" + name);
        log.info("返回：{}", result);
        return result;
    }

    /**
     * 获取用户的任务
     *
     * @param userId 用户id
     */
    @GetMapping("/getTasks/{userId}")
    public ResultVo getTasks(@PathVariable String userId) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
        log.info("userId：{}，tasks：{}", userId, tasks);
        ResultVo result = ResultVo.ok(tasks.toString());
        log.info("返回：{}", result);
        return result;
    }

    /**
     * 审批通过
     */
    @PostMapping("/success/{taskId}")
    public ResultVo success(@PathVariable String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return ResultVo.error("流程不存在");
        }
        //通过审核
        HashMap<String, Object> map = new HashMap<>();
        map.put("approved", true);
        taskService.complete(taskId, map);
        ResultVo result = ResultVo.ok("流程审核通过！");
        log.info("返回：{}", result);
        return result;
    }

    /**
     * .审批不通过
     */
    @PostMapping("/faile/{taskId}")
    public ResultVo faile(@PathVariable String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return ResultVo.error("流程不存在");
        }
        //通过审核
        HashMap<String, Object> map = new HashMap<>();
        map.put("approved", false);
        taskService.complete(taskId, map);
        ResultVo result = ResultVo.ok("审批不通过！");
        log.info("返回：{}", result);
        return result;
    }

    @RequestMapping(value = "processDiagram")
    public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();

        //流程走完的不显示图
        if (pi == null) {
            log.error("流程走完的不显示图");
            return;
        }
        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        //使用流程实例ID，查询正在执行的执行对象表，返回流程实例对象
        String InstanceId = task.getProcessInstanceId();
        List<Execution> executions = runtimeService
                .createExecutionQuery()
                .processInstanceId(InstanceId)
                .list();

        //得到正在执行的Activity的Id
        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (Execution exe : executions) {
            List<String> ids = runtimeService.getActiveActivityIds(exe.getId());
            activityIds.addAll(ids);
        }

        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows, engconf.getActivityFontName(),
                engconf.getLabelFontName(), engconf.getAnnotationFontName(), engconf.getClassLoader(), 1.0, false);
        OutputStream out = null;
        byte[] buf = new byte[1024];
        int legth = 0;
        try {
            out = httpServletResponse.getOutputStream();
            while ((legth = in.read(buf)) != -1) {
                out.write(buf, 0, legth);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
