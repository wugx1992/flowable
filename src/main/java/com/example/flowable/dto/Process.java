package com.example.flowable.dto;

import com.alibaba.fastjson.JSON;
import com.example.flowable.util.AuditStatus;
import com.example.flowable.util.FlowUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.task.Attachment;
import org.flowable.task.api.Task;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class Process implements Serializable {
    private static final long serialVersionUID = 8416239698142485806L;

    private String processInstanceId;

    private AuditStatus auditStatus;

    private Date startTime;

    private Date endTime;

    private String no;

    private String type;

    private String fromUserName;

    private String toUserName;

    private String taskId;

    private String taskName;

    private String deleteReason;

    private Map<String, Object> params;

    private List<BaseBean> files;

    public Process(HistoricProcessInstance process){
        this.no = process.getBusinessKey();
        this.startTime = process.getStartTime();
        this.endTime = process.getEndTime();
        this.deleteReason = process.getDeleteReason();
        this.processInstanceId = process.getId();
//        System.out.println("process.getProcessVariables："+process.getProcessVariables());
    }

    public Process withTask(Task task){
        this.taskId = task.getId();
        this.taskName = task.getName();
//        System.out.println("task.getProcessVariables："+task.getProcessVariables());
        return this;
    }

    public Process withVariables(Map<String, Object> variables){
        System.out.println("withVariables："+variables);
        params = variables;
//        this.params = JSON.parseObject((String) variables.get(FlowUtil.AUDIT_PARAMS_KEY));
//        this.fromUserName = FlowUtil.getFromUserName(variables);
//        Optional<CandidateParam> candidate = FlowUtil.getCandidate(variables);
//        if(candidate.isPresent()){
//            this.toUserName = candidate.get().getName();
//        }
//        this.type = (String) variables.get(FlowUtil.AUDIT_TYPE_KEY);
//        String auditState = (String) variables.get(FlowUtil.AUDIT_STATUS_KEY);
//        if(!StringUtils.isEmpty(auditState)){
//            this.auditStatus = AuditStatus.valueOf(auditState);
//        }
        return this;
    }

    public Process withFiles(List<Attachment> attachments){
        if(attachments  != null && !attachments.isEmpty()){
            this.files = attachments.stream().map(a -> new BaseBean(a.getUrl(), a.getName())).collect(Collectors.toList());
        }
        return this;
    }

    public Process withActivity(HistoricActivityInstance activity){
        this.taskId = activity.getTaskId();
        this.taskName = activity.getActivityName();
        return this;
    }
}
