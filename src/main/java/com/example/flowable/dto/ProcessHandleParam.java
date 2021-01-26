package com.example.flowable.dto;

import lombok.Data;

import java.util.Map;

/**
 * @Author: gx.wu
 * @Date: 2021/1/21 12:25
 * @Description: code something to describe this module what it is
 */
@Data
public class ProcessHandleParam {
    private String taskId;
    private Map<String,Object> variables;
}
