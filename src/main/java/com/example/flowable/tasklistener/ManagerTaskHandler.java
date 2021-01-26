package com.example.flowable.tasklistener;


import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

@Slf4j
public class ManagerTaskHandler implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        delegateTask.setAssignee("经理");
        log.info("TaskListener notify，对象：经理");
    }

}
