package com.example.flowable.controller;

import com.example.flowable.dao.SystemDao;
import com.example.flowable.dto.ProcessHandleByInstanceIdParam;
import com.example.flowable.dto.ProcessHandleParam;
import com.example.flowable.dto.ProcessStartParam;
import com.example.flowable.vo.ResultVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.flowable.dto.Process;

/**
 * @Author: gx.wu
 * @Date: 2021/1/21 12:16
 * @Description: code something to describe this module what it is
 */
@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

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

    @Autowired
    ManagementService managementService;

    @Autowired
    IdentityService identityService;

    @Autowired
    SystemDao systemDao;

    /**
     * 创建审批
     *
     * @param param
     * @return
     */
    @RequestMapping("/start")
    public ResultVo startFlow(@RequestBody ProcessStartParam param) {
        log.info("创建审批参数：{}", param);
        Authentication.setAuthenticatedUserId(param.getCreateUserId());
        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey(param.getProcessInstanceKey(), param.getVariables());
        String processId = processInstance.getId();
        String name = processInstance.getProcessDefinitionName();

        Task task = taskService.createTaskQuery().processInstanceId(processId).singleResult();
        taskService.complete(task.getId());

        log.info("processId：{}，name：{}", processId, name);
        ResultVo result = ResultVo.ok(processId + ":" + name);
        log.info("返回：{}", result);
        return result;
    }

    /**
     * 获取用户的任务
     *
     * @param userId 用户id
     */
    @RequestMapping("/getTasks")
    public ResultVo getTasks(String userId, String groupId) {
        TaskQuery query = taskService.createTaskQuery();
        if(!StringUtils.isEmpty(userId) && StringUtils.isEmpty(groupId)){
            query.taskAssignee(userId);
        }else if(StringUtils.isEmpty(userId) && !StringUtils.isEmpty(groupId)){
            query.taskCandidateGroup(groupId);
        }else if(!StringUtils.isEmpty(userId) && !StringUtils.isEmpty(groupId)){
            query.or().taskAssignee(userId).taskCandidateGroup(groupId);
        }

        List<Task> tasks = query.orderByTaskCreateTime().desc().list();
        for(Task task : tasks){
            log.info("结果 tasks：{}", task);
        }
        ResultVo result = ResultVo.ok(tasks.toString());
        log.info("返回：{}", result);
        return result;
    }

    /**
     * 移除任务
     *
     * @param taskId
     * @return
     */
    @RequestMapping("/deleteTask")
    public ResultVo deleteTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "关闭");
        taskService.deleteTask(taskId);
        log.info("移除任务，tasks：{}，instanceId：{}", taskId, task.getProcessInstanceId());
        ResultVo result = ResultVo.ok(taskId);
        log.info("返回：{}", result);
        return result;
    }


    /**
     * 提交审批
     */
    @RequestMapping("/handle")
    public ResultVo handle(@RequestBody ProcessHandleParam param) {
        log.info("提交审批参数：{}", param);
        //判断是否有权限
//        boolean hadPermission = isAssigneeOrCandidate(userId, param.getTaskId());
        Task task = taskService.createTaskQuery().taskId(param.getTaskId()).singleResult();
        if (task == null) {
            return ResultVo.error("流程不存在");
        }
        taskService.complete(param.getTaskId(), param.getVariables());
        ResultVo result = ResultVo.ok("提交成功！");
        log.info("返回：{}", result);
        return result;
    }

    /**
     * 提交审批
     */
    @RequestMapping("/handleByInstanceId")
    public ResultVo handleByInstanceId(@RequestBody ProcessHandleByInstanceIdParam param) {
        log.info("提交审批参数：{}", param);
        Task task = taskService.createTaskQuery().processInstanceId(param.getInstanceId()).singleResult();
        if (task == null) {
            return ResultVo.error("流程不存在");
        }
        taskService.complete(task.getId(), param.getVariables());
        ResultVo result = ResultVo.ok("提交成功！");
        log.info("返回：{}", result);
        return result;
    }

    /**
     * 生成流程图
     *
     * @param httpServletResponse
     * @param processId
     * @throws Exception
     */
    @RequestMapping(value = "/processDiagram")
    public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception {
        byte[] buffer = createImage(processId);
        OutputStream out = null;
        try {
            out = httpServletResponse.getOutputStream();
            out.write(buffer);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public byte[] createImage(String processInstanceId) {
        //1.获取当前的流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        String processDefinitionId = null;
        List<String> activeActivityIds = new ArrayList<>();
        List<String> highLightedFlows = new ArrayList<>();
        //3. 获取流程定义id和高亮的节点id
        if (processInstance != null) {
            //2.获取所有的历史轨迹线对象
            List<HistoricActivityInstance> historicSquenceFlows = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId).activityType(BpmnXMLConstants.ELEMENT_SEQUENCE_FLOW).list();
            historicSquenceFlows.forEach(historicActivityInstance -> highLightedFlows.add(historicActivityInstance.getActivityId()));
            //3.1. 正在运行的流程实例
            processDefinitionId = processInstance.getProcessDefinitionId();
            activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
        } else {
            //3.2. 已经结束的流程实例
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            processDefinitionId = historicProcessInstance.getProcessDefinitionId();
            //3.3. 获取结束节点列表
            List<HistoricActivityInstance> historicEnds = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId).activityType(BpmnXMLConstants.ELEMENT_EVENT_END).list();
            List<String> finalActiveActivityIds = activeActivityIds;
            historicEnds.forEach(historicActivityInstance -> finalActiveActivityIds.add(historicActivityInstance.getActivityId()));
        }
        //4. 获取bpmnModel对象
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        //5. 生成图片流
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activeActivityIds, highLightedFlows, engconf.getActivityFontName(),
                engconf.getLabelFontName(), engconf.getAnnotationFontName(), engconf.getClassLoader(), 1.0, false);
        //6. 转化成byte便于网络传输
        byte[] datas = IoUtil.readInputStream(in, "image inputStream name");
        return datas;
    }

    /**
     * 测试多数据源
     */
    @RequestMapping("/multipleDataSourceTest")
    public void multipleDataSourceTest() {
        log.info("测试：{}", systemDao.count());
    }


    /**
     * 查看待我审批的任务
     */
    @RequestMapping("/queryPendingTask")
    public void queryPendingTask(String userId) throws JsonProcessingException {
        TaskQuery query = taskService.createTaskQuery()
                // or() 和 endOr()就像是左括号和右括号，中间用or连接条件
                // 指定是我审批的任务或者所在的组别审批的任务
                // 实在太复杂的情况，建议不使用flowable的查询
                .or()
                .taskAssignee(userId)
//                .taskCandidateUser(user.getId())
//                .taskCandidateGroup(user.getGroup())
                .endOr();
        // 查询自定义字段
//        if (StringUtils.isNotEmpty(auditType)) {
//            query.processVariableValueEquals(AUDIT_TYPE_KEY, auditType);
//        }
//        if(auditStatus != null){
//            query.processVariableValueEquals(AUDIT_STATUS_KEY, auditStatus.toString());
//        }
        // 根据创建时间倒序
        List<Process> list = query.orderByTaskCreateTime().desc()
                // 分页
                .listPage(0, 10)
                .stream().map(t -> {
                    // 拿到这个任务的流程实例，用于显示流程开始时间、结束时间、业务编号
                    HistoricProcessInstance p = historyService.createHistoricProcessInstanceQuery().processInstanceId(t.getProcessInstanceId()).singleResult();

                    return new Process(p).withTask(t) // 拿到任务编号和任务名称
                            // 拿到创建时和中途加入的自定义参数
                            .withVariables(taskService.getVariables(t.getId()))
                            .withFiles(taskService.getProcessInstanceAttachments(p.getId()));
                }).collect(Collectors.toList());
        ObjectMapper objectMapper = new ObjectMapper();
        for (Process item : list) {
            log.info("任务：{}", objectMapper.writeValueAsString(item));
        }
    }


    /**
     * 查看我已审批的任务
     */
    @RequestMapping("/queryFinishTask")
    public void queryFinishTask(String userId, String amount) throws JsonProcessingException {
        List<HistoricActivityInstance> activeList;
        // 如果不需要筛选自定义参数
        if (StringUtils.isEmpty(amount)) {
            activeList = historyService.createHistoricActivityInstanceQuery()
                    // 我审批的
                    .taskAssignee(userId)
                    // 按照结束时间倒序
                    .orderByHistoricActivityInstanceEndTime().desc()
                    // 已结束的（其实就是判断有没有结束时间）
                    .finished()
                    // 分页
                    .listPage(0, 10);
        } else {
            // 否则需要自定义sql
            // managementService.getTableName是用来获取表名的（加上上一篇提到的liquibase，估计flowable作者对数据表命名很纠结）
            // 这里从HistoricVariableInstance对应的表里找到自定义参数
            // 筛选对象类型不支持二进制，存储的时候尽量使用字符串、数字、布尔值、时间，用来比较的值也有很多限制，例如null不能用like比较。
            String sql = "SELECT DISTINCT RES.* " +
                    "FROM " + managementService.getTableName(HistoricActivityInstance.class) + " RES " +
                    "INNER JOIN " + managementService.getTableName(HistoricVariableInstance.class) + " var " +
                    "ON var.PROC_INST_ID_ = res.PROC_INST_ID_  " +
                    "WHERE RES.ASSIGNEE_ = #{assignee} " +
                    "AND RES.END_TIME_ IS NOT NULL ";
            sql += "AND var.name_ = #{amountKey} AND var.TEXT_ = #{amountValue}";
            sql += " ORDER BY RES.END_TIME_ DESC";
            activeList = historyService.createNativeHistoricActivityInstanceQuery().sql(sql)
                    // 参数用#{assignee}占位后，再调用parameter("assignee", assignee)填入值
                    // 参数值可以多出来没用到的，比hibernate好多了
                    .parameter("assignee", userId)
                    .parameter("amountKey", "amount")
                    .parameter("amountValue", amount)
                    .listPage(0, 10);
        }
        log.info("查得结果：{}", activeList.size());
        List<Process> list = activeList.stream().map(a -> {
            // 同上面的拿到这个任务的流程实例
            HistoricProcessInstance p = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(a.getProcessInstanceId())
                    .singleResult();
            // 因为任务已结束（我看到有提到删除任务TaskHelper#completeTask），所以只能从历史里获取
            Map<String, Object> params = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(a.getProcessInstanceId()).list()
                    // 拿到的是HistoricVariableInstance对象，需要转成原来存储的方式
                    .stream().collect(Collectors.toMap(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue));
            return new Process(p).withActivity(a).withVariables(params);
        }).collect(Collectors.toList());

        ObjectMapper objectMapper = new ObjectMapper();
        for (Process item : list) {
            log.info("任务：{}", objectMapper.writeValueAsString(item));
        }
    }


    /**
     * 查看我创建的任务
     */
    @RequestMapping("/queryCreateTask")
    public void queryCreateTask(String userId, String amount) throws JsonProcessingException {// startedBy：创建任务时设置的发起人
        HistoricProcessInstanceQuery instanceQuery = historyService.createHistoricProcessInstanceQuery().startedBy(userId);
        // 自定义参数筛选
        if (StringUtils.isNotEmpty(amount)) {
            instanceQuery.variableValueEquals("amount", amount);
        }
        List<Process> list = instanceQuery.orderByProcessInstanceStartTime().desc()
                .listPage(0, 10).stream()
                //  获取其中的详细和自定义参数
                .map(this::convertHistoryProcess)
                .collect(Collectors.toList());

        log.info("查得结果：{}", list.size());
        ObjectMapper objectMapper = new ObjectMapper();
        for (Process item : list) {
            log.info("任务：{}", objectMapper.writeValueAsString(item));
        }
    }



    /**
     * 查看流程所有用户任务
     */
    @RequestMapping("/queryInstanceTask")
    public void queryInstanceTask(String instanceId) throws JsonProcessingException {
        List<Task> list = taskService.createTaskQuery().processInstanceId(instanceId).list();
        log.info("查得结果：{}", list.size());
        ObjectMapper objectMapper = new ObjectMapper();
        for (Task item : list) {
            log.info("任务：{}", objectMapper.writeValueAsString(item));
        }
    }


    /**
     * 创建用户组
     */
    @RequestMapping("/createUserAndGroup")
    public void createUserAndGroup(String userId, String userName, String groupId, String groupName) throws JsonProcessingException {

        User user;
        Group group;
        List<User> checkUsers = identityService.createUserQuery().userId(userId).list();
        if(checkUsers.size()==0){
            user = identityService.newUser(userId);
        }else{
            log.info("用户已存在：{}", checkUsers);
            user = checkUsers.get(0);
        }

        user.setDisplayName(userName);
        identityService.saveUser(user);

        List<Group> checkGroups = identityService.createGroupQuery().groupId(groupId).list();
        if(checkGroups.size()==0){
            group = identityService.newGroup(groupId);
        }else {
            log.info("组已存在：{}", checkGroups);
            group = checkGroups.get(0);
        }
        group.setName(groupName);
        group.setType("department01");
        identityService.saveGroup(group);
        User checkUser = identityService.createUserQuery().memberOfGroup(groupId).userId(userId).singleResult();
        if(checkUser==null){
            identityService.createMembership(user.getId(), group.getId());
        }else{
            log.info("关系已存在：{}", checkUser);
        }

        List<User> list = identityService.createUserQuery().memberOfGroup(groupId).list();
        log.info("查得结果：{}", list.size());
        ObjectMapper objectMapper = new ObjectMapper();
        for (User item : list) {
            log.info("任务：{}", objectMapper.writeValueAsString(item));
        }
    }

    private Process convertHistoryProcess(HistoricProcessInstance p) {
        // 不管流程是否结束，到历史里查，最方便
        Map<String, Object> params = historyService.createHistoricVariableInstanceQuery().processInstanceId(p.getId()).list()
                .stream().collect(Collectors.toMap(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue));
        // 获取最新的一个userTask，也就是任务活动纪录
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(p.getId())
                .orderByHistoricActivityInstanceStartTime().desc()
                .orderByHistoricActivityInstanceEndTime().asc().
                        listPage(0, 1);
        Process data = new Process(p);
        if (!activities.isEmpty()) {
            data.withActivity(activities.get(0));
        }
        return data.withVariables(params);
    }

    public boolean isAssigneeOrCandidate(String userId, String taskId){
        long count = taskService.createTaskQuery()
                .taskId(taskId)
                .or()
                .taskAssignee(userId)
//                .taskCandidateUser(user.getId())
//                .taskCandidateGroup(user.getGroup())
                .endOr().count();
        return count > 0;
    }

}
