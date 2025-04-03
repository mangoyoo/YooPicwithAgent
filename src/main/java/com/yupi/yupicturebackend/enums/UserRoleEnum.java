package com.yupi.yupicturebackend.enums;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum UserRoleEnum {
    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static UserRoleEnum getEnumByValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Map<String, UserRoleEnum> userRoleEnumMap = Arrays.stream(UserRoleEnum.values()).
                collect(Collectors.toMap(UserRoleEnum::getValue, userRoleEnum -> userRoleEnum));
        return userRoleEnumMap.getOrDefault(value, null);
    }
}


