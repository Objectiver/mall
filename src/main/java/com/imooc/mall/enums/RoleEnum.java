package com.imooc.mall.enums;

import lombok.Getter;

/**
 * @author zhangzhao
 * @Date 2023/2/4
 * @description
 */
@Getter
public enum RoleEnum {
    ADMIN(0),

    CUSTOMER(1),

    ;

    Integer code;

    RoleEnum(Integer code) {
        this.code = code;
    }
}
