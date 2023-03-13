package com.imooc.mall.exception;

import com.imooc.mall.enums.ResponseEnum;
import com.imooc.mall.vo.ResponseVo;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Objects;

import static com.imooc.mall.enums.ResponseEnum.ERROR;

/**
 * Created by ZhangZhao
 */
@ControllerAdvice  //捕获运行异常，不然抛异常后前端只会获得页面报错，希望报错之后能够是包含具体错误信息的json数据
public class RuntimeExceptionHandler {

	@ExceptionHandler(RuntimeException.class)//运行时异常
	@ResponseBody
//	@ResponseStatus(HttpStatus.FORBIDDEN)
	public ResponseVo handle(RuntimeException e) {
		return ResponseVo.error(ERROR, e.getMessage());
	}

	@ExceptionHandler(UserLoginException.class)//登录异常
	@ResponseBody
	public ResponseVo userLoginHandle() {
		return ResponseVo.error(ResponseEnum.NEED_LOGIN);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseBody
	public ResponseVo notValidExceptionHandle(MethodArgumentNotValidException e) {
		BindingResult bindingResult = e.getBindingResult();
		Objects.requireNonNull(bindingResult.getFieldError());
		return ResponseVo.error(ResponseEnum.PARAM_ERROR,
				bindingResult.getFieldError().getField() + " " + bindingResult.getFieldError().getDefaultMessage());
	}
}
