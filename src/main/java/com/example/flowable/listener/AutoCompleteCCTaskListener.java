//package com.example.flowable.listener;
//
//import com.example.flowable.util.SpringUtil;
//import org.flowable.bpmn.model.BpmnModel;
//import org.flowable.bpmn.model.FlowElement;
//import org.flowable.bpmn.model.StartEvent;
//import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
//import org.flowable.common.engine.api.delegate.event.FlowableEvent;
//import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
//import org.flowable.common.engine.impl.event.FlowableEntityEventImpl;
//import org.flowable.engine.RepositoryService;
//import org.flowable.engine.impl.context.Context;
//import org.flowable.task.service.impl.persistence.entity.TaskEntity;
//
///**
// * @Author: gx.wu
// * @Date: 2021/1/28 15:16
// * @Description: 自动设置抄送用户任务节点完成监听器
// */
//public class AutoCompleteCCTaskListener implements FlowableEventListener {
//    /**
//     * Called when an event has been fired
//     *
//     * @param event the event
//     */
//    @Override
//    public void onEvent(FlowableEvent event) {
//        if (!(event instanceof FlowableEntityEventImpl)) {
//            return;
//        }
//
//        FlowableEntityEventImpl entityEvent = (FlowableEntityEventImpl) event;
//
//        Object entity = entityEvent.getEntity();
//
//        //是否是任务实体类
//        if (!(entity instanceof TaskEntity)) {
//            return;
//        }
//
//        TaskEntity taskEntity = (TaskEntity) entity;
//
//        //是否是在任务节点创建时
//        if (FlowableEngineEventType.TASK_CREATED.equals(event.getType())) {
//            //找到流程第一个userTask节点
//            FlowElement ccElement = this.findCCFlowElement(taskEntity);
//
//            //对比节点是否相同,因为有可能有子流程的节点进来
//            if (ccElement != null && taskEntity.getTaskDefinitionKey().equals(ccElement.getId())) {
//                Context.getProcessEngineConfiguration().getTaskService().complete(taskEntity.getId());
//            }
//        }
//
//    }
//
//    /**
//     * 查找流程抄送userTask
//     */
//    private FlowElement findCCFlowElement(TaskEntity taskEntity) {
//        RepositoryService repositoryService = SpringUtil.getBean(RepositoryService.class);
//        BpmnModel bpmnModel = repositoryService.getBpmnModel(taskEntity.getProcessDefinitionId());
//        for (FlowElement flowElement : bpmnModel.getProcesses().get(0).getFlowElements()) {
//            if (flowElement instanceof StartEvent) {
//                return bpmnModel.getFlowElement(((StartEvent) flowElement).getOutgoingFlows().get(0).getTargetRef());
//            }
//        }
//        return null;
//    }
//
//
//    /**
//     * @return whether or not the current operation should fail when this listeners execution throws an exception.
//     */
//    @Override
//    public boolean isFailOnException() {
//        return false;
//    }
//
//    /**
//     * @return Returns whether this event listener fires immediately when the event occurs or
//     * on a transaction lifecycle event (before/after commit or rollback).
//     */
//    @Override
//    public boolean isFireOnTransactionLifecycleEvent() {
//        return false;
//    }
//
//    /**
//     * @return if non-null, indicates the point in the lifecycle of the current transaction when the event should be fired.
//     */
//    @Override
//    public String getOnTransaction() {
//        return null;
//    }
//}
