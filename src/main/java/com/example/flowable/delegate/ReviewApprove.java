package com.example.flowable.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @Author: gx.wu
 * @Date: 2021/1/21 10:54
 * @Description: code something to describe this module what it is
 */
@Slf4j
public class ReviewApprove implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) {
        //可以发送消息给某人
        log.info("通过，userId是：{}",delegateExecution.getVariable("userId"));
    }
}
