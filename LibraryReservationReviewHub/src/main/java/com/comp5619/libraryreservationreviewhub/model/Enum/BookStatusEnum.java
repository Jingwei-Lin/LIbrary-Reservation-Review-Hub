package com.comp5619.libraryreservationreviewhub.model.Enum;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 图书状态枚举
 */
@Getter
public enum BookStatusEnum {

    AVAILABLE("可借阅", "available"),
    UNAVAILABLE("不可借阅", "unavailable"),
    MAINTENANCE("维护中", "maintenance"),
    DELETED("已删除", "deleted");

    private final String text;
    private final String value;

    BookStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static BookStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (BookStatusEnum bookStatusEnum : BookStatusEnum.values()) {
            if (bookStatusEnum.value.equals(value)) {
                return bookStatusEnum;
            }
        }
        return null;
    }
}