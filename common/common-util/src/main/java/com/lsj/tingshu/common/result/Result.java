package com.lsj.tingshu.common.result;

import lombok.Data;

/**
 * 全局统一返回结果类
 */
@Data
public class Result<T> {

    //返回码
    private Integer code;

    //返回消息
    private String message;

    //返回数据
    private T data;

    // 构造方法
    public Result() {
    }

    // 返回结果对象（携带数据【有、无】不携带状态码和状态的消息提示）
    protected static <T> Result<T> build(T data) {
        Result<T> result = new Result<T>();
        if (data != null)
            result.setData(data);
        return result;
    }

    // 返回结果对象（携带数据【有、无】 状态码和状态的消息提示）
    public static <T> Result<T> build(T body, Integer code, String message) {
        Result<T> result = build(body);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    // 返回结果对象（鞋带数据【有、无】 状态码和状态消息的枚举对象）

    public static <T> Result<T> build(T body, ResultCodeEnum resultCodeEnum) {
        Result<T> result = build(body);
        result.setCode(resultCodeEnum.getCode());
        result.setMessage(resultCodeEnum.getMessage());
        return result;
    }

    public static <T> Result<T> ok() {
        return Result.ok(null);
    }

    // 返回结果对象 （携带数据【有、无】 成功状态）
    public static <T> Result<T> ok(T data) {
        return build(data, ResultCodeEnum.SUCCESS);
    }


    // 返回结果对象 （携带数据【有、无】 失败状态）
    public static <T> Result<T> fail(T data) {
        return build(data, ResultCodeEnum.FAIL);
    }


    public static <T> Result<T> fail() {
        return Result.fail(null);
    }
}
