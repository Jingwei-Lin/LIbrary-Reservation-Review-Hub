package com.comp5619.libraryreservationreviewhub.model.Enum;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 评论状态枚举
 */
@Getter
public enum ReviewStatusEnum {

    PUBLISHED("已发布", "published"),
    PENDING("待审核", "pending"),
    REJECTED("已拒绝", "rejected"),
    DELETED("已删除", "deleted");

    private final String text;
    private final String value;

    ReviewStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static ReviewStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ReviewStatusEnum reviewStatusEnum : ReviewStatusEnum.values()) {
            if (reviewStatusEnum.value.equals(value)) {
                return reviewStatusEnum;
            }
        }
        return null;
    }
}