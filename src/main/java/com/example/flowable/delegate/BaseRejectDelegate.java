package com.example.flowable.delegate;

import com.example.flowable.util.AuditStatus;
import com.example.flowable.util.FlowUtil;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

@Slf4j
public class BaseRejectDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("{}已被拒绝", execution.getVariables());
        FlowUtil.end(execution, AuditStatus.REJECT_AUDIT);
    }
}
