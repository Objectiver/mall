package com.imooc.mall.service.impl;

import com.google.gson.Gson;
import com.imooc.mall.dao.ProductMapper;
import com.imooc.mall.enums.ProductStatusEnum;
import com.imooc.mall.enums.ResponseEnum;
import com.imooc.mall.form.CartAddForm;
import com.imooc.mall.form.CartUpdateForm;
import com.imooc.mall.pojo.Cart;
import com.imooc.mall.pojo.Product;
import com.imooc.mall.service.ICartService;
import com.imooc.mall.vo.CartProductVo;
import com.imooc.mall.vo.CartVo;
import com.imooc.mall.vo.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by 廖师兄
 */
@Service   //在购物车里面，数据如果存成list格式，需要更改某产品的数量，那么需要遍历商品list,根据productId查找到商品，然后更改
	//如果是Map的话，可以Map.get(productId) 不需要遍历，高性能购物车；Redis中具有这样的map结构，java中叫Hash结构
public class CartServiceImpl implements ICartService {

	private final static String CART_REDIS_KEY_TEMPLATE = "cart_%d";

	@Autowired
	private ProductMapper productMapper;

	@Autowired
	private StringRedisTemplate redisTemplate;

	private Gson gson = new Gson();

	@Override
	public ResponseVo<CartVo> add(Integer uid, CartAddForm form) {
		Integer quantity = 1;

		Product product = productMapper.selectByPrimaryKey(form.getProductId());

		//商品是否存在
		if (product == null) {
			return ResponseVo.error(ResponseEnum.PRODUCT_NOT_EXIST);
		}

		//商品是否正常在售
		if (!product.getStatus().equals(ProductStatusEnum.ON_SALE.getCode())) {
			return ResponseVo.error(ResponseEnum.PRODUCT_OFF_SALE_OR_DELETE);
		}

		//商品库存是否充足
		if (product.getStock() <= 0) {
			return ResponseVo.error(ResponseEnum.PROODUCT_STOCK_ERROR);
		}

		//写入到redis，商品id 、 添加（购买）数量、是否选中  存入redis  ,
		// 其余像价格等信息直接从数据库读取，保证数据实时更新
		//key: cart_1
		HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
		//接口里添加购物车永远累加1，这里先读出redis里的数据  CART_REDIS_KEY_TEMPLATE  =  "cart_%d"
		String redisKey  = String.format(CART_REDIS_KEY_TEMPLATE, uid);

		Cart cart;
		String value = opsForHash.get(redisKey, String.valueOf(product.getId()));
		//如果是空的
		if (StringUtils.isEmpty(value)) {
			//没有该商品, 新增
			cart = new Cart(product.getId(), quantity, form.getSelected());
		}else {
			//已经有了，数量+1
			cart = gson.fromJson(value, Cart.class);//反序列化
			cart.setQuantity(cart.getQuantity() + quantity);
		}
		//将购物车加入     ，这里一个购物车只代表一件商品。即是包含购买数量  是否选中等信息的商品
		opsForHash.put(redisKey,
				String.valueOf(product.getId()),
				gson.toJson(cart));

		return list(uid);
	}

	@Override
	//根据用户id获取列表
	public ResponseVo<CartVo> list(Integer uid) {
		HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
		String redisKey  = String.format(CART_REDIS_KEY_TEMPLATE, uid);
		//用map只能获取条数据，而entries可以获取所有数据，opsForHash.entries获取购物车数据
		Map<String, String> entries = opsForHash.entries(redisKey);

		boolean selectAll = true;
		Integer cartTotalQuantity = 0;
		BigDecimal cartTotalPrice = BigDecimal.ZERO;
		CartVo cartVo = new CartVo();
		List<CartProductVo> cartProductVoList = new ArrayList<>();
		//需要遍历列表，将数据库的cart对象转换为cartVo对象，因为返回的是cartVo对象
		for (Map.Entry<String, String> entry : entries.entrySet()) {
			//获得购物车该栏产品id
			Integer productId = Integer.valueOf(entry.getKey());
			//获取集合里cart 的值为字符串  转换为对象
			Cart cart = gson.fromJson(entry.getValue(), Cart.class);

			//TODO 需要优化，使用mysql里的in
			//根据productId从数据库中获取product，需要优化，使用mysql里的in
			Product product = productMapper.selectByPrimaryKey(productId);
			if (product != null) {
				CartProductVo cartProductVo = new CartProductVo(productId,
						cart.getQuantity(),
						product.getName(),
						product.getSubtitle(),
						product.getMainImage(),
						product.getPrice(),
						product.getStatus(),
						product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())),
						product.getStock(),
						cart.getProductSelected()
				);
				cartProductVoList.add(cartProductVo);

				if (!cart.getProductSelected()) {
					selectAll = false;
				}

				//计算总价(只计算选中的)
				if (cart.getProductSelected()) {
					//Bigdicemal.add()
					cartTotalPrice = cartTotalPrice.add(cartProductVo.getProductTotalPrice());
				}
			}

			cartTotalQuantity += cart.getQuantity();
		}

		//有一个没有选中，就不叫全选
		cartVo.setSelectedAll(selectAll);
		cartVo.setCartTotalQuantity(cartTotalQuantity);
		cartVo.setCartTotalPrice(cartTotalPrice);
		cartVo.setCartProductVoList(cartProductVoList);
		return ResponseVo.success(cartVo);
	}

	@Override
	public ResponseVo<CartVo> update(Integer uid, Integer productId, CartUpdateForm form) {
		HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
		String redisKey  = String.format(CART_REDIS_KEY_TEMPLATE, uid);

		String value = opsForHash.get(redisKey, String.valueOf(productId));
		if (StringUtils.isEmpty(value)) {
			//没有该商品, 报错
			return ResponseVo.error(ResponseEnum.CART_PRODUCT_NOT_EXIST);
		}

		//已经有了，修改内容
		Cart cart = gson.fromJson(value, Cart.class);
		if (form.getQuantity() != null
				&& form.getQuantity() >= 0) {
			cart.setQuantity(form.getQuantity());
		}
		if (form.getSelected() != null) {
			cart.setProductSelected(form.getSelected());
		}

		opsForHash.put(redisKey, String.valueOf(productId), gson.toJson(cart));
		return list(uid);
	}

	@Override
	public ResponseVo<CartVo> delete(Integer uid, Integer productId) {
		HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
		String redisKey  = String.format(CART_REDIS_KEY_TEMPLATE, uid);

		String value = opsForHash.get(redisKey, String.valueOf(productId));
		if (StringUtils.isEmpty(value)) {
			//没有该商品, 报错
			return ResponseVo.error(ResponseEnum.CART_PRODUCT_NOT_EXIST);
		}

		opsForHash.delete(redisKey, String.valueOf(productId));
		return list(uid);
	}

	@Override
	public ResponseVo<CartVo> selectAll(Integer uid) {
		HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
		String redisKey  = String.format(CART_REDIS_KEY_TEMPLATE, uid);

		for (Cart cart : listForCart(uid)) {
			cart.setProductSelected(true);
			opsForHash.put(redisKey,
					String.valueOf(cart.getProductId()),
					gson.toJson(cart));
		}

		return list(uid);
	}

	@Override
	public ResponseVo<CartVo> unSelectAll(Integer uid) {
		HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
		String redisKey  = String.format(CART_REDIS_KEY_TEMPLATE, uid);

		for (Cart cart : listForCart(uid)) {
			cart.setProductSelected(false);
			opsForHash.put(redisKey,
					String.valueOf(cart.getProductId()),
					gson.toJson(cart));
		}

		return list(uid);
	}

	@Override
	public ResponseVo<Integer> sum(Integer uid) {
		Integer sum = listForCart(uid).stream()
				.map(Cart::getQuantity)
				.reduce(0, Integer::sum);
		return ResponseVo.success(sum);
	}
	//生成购物车列表，购物车 属性  一个商品， Integer productId/Integer quantity/Boolean productSelected
	public List<Cart> listForCart(Integer uid) {
		HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
		//声明redis  key  = cart_uid
		String redisKey  = String.format(CART_REDIS_KEY_TEMPLATE, uid);
		//redis根据uid获取键值对
		Map<String, String> entries = opsForHash.entries(redisKey);

		List<Cart> cartList = new ArrayList<>();
		for (Map.Entry<String, String> entry : entries.entrySet()) {
			cartList.add(gson.fromJson(entry.getValue(), Cart.class));
		}

		return cartList;
	}

}
