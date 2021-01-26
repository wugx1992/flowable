package com.example.flowable.vo;

import lombok.Data;

/**
 * @Author: gx.wu
 * @Date: 2021/1/20 18:02
 * @Description: code something to describe this module what it is
 */
@Data
public class ResultVo {
    private boolean success;
    private String message;

    ResultVo (boolean success, String message){
        this.success = success;
        this.message = message;
    }

    public static ResultVo ok(String message){
        return new ResultVo(true, message);
    }

    public static ResultVo error(String message){
        return new ResultVo(false, message);
    }
}
