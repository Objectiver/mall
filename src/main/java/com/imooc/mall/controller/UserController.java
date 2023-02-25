package com.imooc.mall.controller;

import com.imooc.mall.consts.MallConst;
import com.imooc.mall.form.UserLoginForm;
import com.imooc.mall.form.UserRegisterForm;
import com.imooc.mall.pojo.User;
import com.imooc.mall.service.IUserService;
import com.imooc.mall.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

/**
 * Created by ZhangZhao
 */
@RestController
@Slf4j
//方法都有判断登录状态的事务，可以提到拦截器中执行
public class UserController {

	@Autowired
	private IUserService userService;

	@PostMapping("/user/register")
	public ResponseVo<User> register(@Valid @RequestBody UserRegisterForm userForm) {// @RequestBody使json格式传入实体对象  也可以是map集合
		User user = new User();
		BeanUtils.copyProperties(userForm, user);
		//dto
		return userService.register(user);
	}

	@PostMapping("/user/login")
	public ResponseVo<User> login(@Valid @RequestBody UserLoginForm userLoginForm,
								  HttpSession session) {
		ResponseVo<User> userResponseVo = userService.login(userLoginForm.getUsername(), userLoginForm.getPassword());

		//设置Session
		//session.setAttribute("currentUser", userResponseVo.getData());带字符串，硬编码不易维护
		session.setAttribute(MallConst.CURRENT_USER, userResponseVo.getData());
		log.info("/login sessionId={}", session.getId());

		return userResponseVo;
	}

	//session保存在内存里，(电脑重启服务器重启就没了) 改进版：token+redis
	@GetMapping("/user")
	public ResponseVo<User> userInfo(HttpSession session) {
		log.info("/user sessionId={}", session.getId());//拿到sessionId换个浏览器进行网页更改，网页未登录状态能变成登录状态，所以
		//session没那么安全，cookie安全，虽然sessionId也是放入cookie中
		User user = (User) session.getAttribute(MallConst.CURRENT_USER);
		return ResponseVo.success(user);
	}

	/**
	 * {@link TomcatServletWebServerFactory} getSessionTimeoutInMinutes（源码设置最低1分钟）
	 */
	@PostMapping("/user/logout")
	public ResponseVo logout(HttpSession session) {
		log.info("/user/logout sessionId={}", session.getId());
		session.removeAttribute(MallConst.CURRENT_USER);
		return ResponseVo.success();
	}
}
