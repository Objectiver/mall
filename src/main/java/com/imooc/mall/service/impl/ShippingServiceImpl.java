package com.imooc.mall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.imooc.mall.dao.ShippingMapper;
import com.imooc.mall.enums.ResponseEnum;
import com.imooc.mall.form.ShippingForm;
import com.imooc.mall.pojo.Shipping;
import com.imooc.mall.service.IShippingService;
import com.imooc.mall.vo.ResponseVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ZhangZhao
 */
@Service
public class ShippingServiceImpl implements IShippingService {

	@Autowired
	private ShippingMapper shippingMapper;

	@Override//insert后直接返回自增id，xml文档insertSelective方法需要配置useGeneratedKeys = "true" keyProperty="id"
	public ResponseVo<Map<String, Integer>> add(Integer uid, ShippingForm form) {
		Shipping shipping = new Shipping();
		BeanUtils.copyProperties(form, shipping);
		shipping.setUserId(uid);
		//写入数据库，写入成功返回1，写入不成功返回0
		int row = shippingMapper.insertSelective(shipping);
		if (row == 0) {
			return ResponseVo.error(ResponseEnum.ERROR);
		}
		//返回自增的shippingId,字段少直接返回map，返回字段键值对多可以建立一个新对象返回
		Map<String, Integer> map = new HashMap<>();
		map.put("shippingId", shipping.getId());

		return ResponseVo.success(map);
	}

	@Override
	public ResponseVo delete(Integer uid, Integer shippingId) {
		int row = shippingMapper.deleteByIdAndUid(uid, shippingId);
		if (row == 0) {
			return ResponseVo.error(ResponseEnum.DELETE_SHIPPING_FAIL);
		}

		return ResponseVo.success();
	}

	@Override
	public ResponseVo update(Integer uid, Integer shippingId, ShippingForm form) {
		Shipping shipping = new Shipping();
		BeanUtils.copyProperties(form, shipping);
		shipping.setUserId(uid);
		shipping.setId(shippingId);
		int row = shippingMapper.updateByPrimaryKeySelective(shipping);
		if (row == 0) {
			return ResponseVo.error(ResponseEnum.ERROR);
		}
		return ResponseVo.success();
	}

	@Override
	public ResponseVo<PageInfo> list(Integer uid, Integer pageNum, Integer pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		List<Shipping> shippings = shippingMapper.selectByUid(uid);
		PageInfo pageInfo = new PageInfo(shippings);
		return ResponseVo.success(pageInfo);
	}
}
