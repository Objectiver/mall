package com.imooc.mall.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Created by ZhangZhao
 */
@Data
public class OrderCreateForm {

	@NotNull
	private Integer shippingId;
}
