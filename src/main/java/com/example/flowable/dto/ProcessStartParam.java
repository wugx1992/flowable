package com.example.flowable.dto;

import lombok.Data;

import java.util.Map;

/**
 * @Author: gx.wu
 * @Date: 2021/1/21 12:19
 * @Description: code something to describe this module what it is
 */
@Data
public class ProcessStartParam {
    private String processInstanceKey;
    private Map<String,Object> variables;
    private String createUserId;
}
