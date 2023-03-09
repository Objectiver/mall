package com.imooc.mall.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.imooc.mall.enums.ResponseEnum;
import lombok.Data;
import org.springframework.validation.BindingResult;

import java.util.Objects;

/**
 * Created by ZhangZhao
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)//@JsonInclude注解的作用是指定实体类在序列化时的策略
/*
ALWAYS：表示总是序列化所有属性
NON_NULL：表示序列化非null属性
NON_ABSENT：表示序列化非null或者引用类型缺省值，例如java8的Optional类，这个选中通常与Optional一起使用
NON_EMPTY：表示序列化非Empty的属性，例如空的集合不会被序列化
NON_DEFAULT：仅包含与POJO属性默认值不同的值
CUSTOM：由{@link JsonInclude＃valueFilter}指定值本身，或由{@link JsonInclude＃contentFilter}指定结构化类型的内容，由过滤器对象的equals方法进行序列化，返回true则会被排除，返回false会被序列化
USE_DEFAULTS：使用默认值
 */
public class ResponseVo<T> {

	private Integer status;

	private String msg;

	private T data;

	private ResponseVo(Integer status, String msg) {
		this.status = status;
		this.msg = msg;
	}

	private ResponseVo(Integer status, T data) {
		this.status = status;
		this.data = data;
	}

	public static <T> ResponseVo<T> successByMsg(String msg) {
		return new ResponseVo<>(ResponseEnum.SUCCESS.getCode(), msg);
	}

	public static <T> ResponseVo<T> success(T data) {
		return new ResponseVo<>(ResponseEnum.SUCCESS.getCode(), data);
	}

	public static <T> ResponseVo<T> success() {
		return new ResponseVo<>(ResponseEnum.SUCCESS.getCode(), ResponseEnum.SUCCESS.getDesc());
	}

	public static <T> ResponseVo<T> error(ResponseEnum responseEnum) {
		return new ResponseVo<>(responseEnum.getCode(), responseEnum.getDesc());
	}

	public static <T> ResponseVo<T> error(ResponseEnum responseEnum, String msg) {
		return new ResponseVo<>(responseEnum.getCode(), msg);
	}

	public static <T> ResponseVo<T> error(ResponseEnum responseEnum, BindingResult bindingResult) {
		return new ResponseVo<>(responseEnum.getCode(),
				Objects.requireNonNull(bindingResult.getFieldError()).getField() + " " + bindingResult.getFieldError().getDefaultMessage());
	}
}
