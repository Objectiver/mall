package com.imooc.mall.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Created by ZhangZhao
 */
@Data
public class UserLoginForm {

	@NotBlank
	private String username;

	@NotBlank
	private String password;
}
