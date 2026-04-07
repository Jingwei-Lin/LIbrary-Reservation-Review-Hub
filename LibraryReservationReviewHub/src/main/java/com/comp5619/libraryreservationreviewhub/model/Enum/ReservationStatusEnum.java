package com.comp5619.libraryreservationreviewhub.model.Enum;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 预订状态枚举
 */
@Getter
public enum ReservationStatusEnum {

    RESERVED("已预订", "reserved"),
    BORROWED("已借出", "borrowed"),
    RETURNED("已归还", "returned"),
    OVERDUE("逾期", "overdue"),
    CANCELLED("已取消", "cancelled");

    private final String text;
    private final String value;

    ReservationStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static ReservationStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ReservationStatusEnum reservationStatusEnum : ReservationStatusEnum.values()) {
            if (reservationStatusEnum.value.equals(value)) {
                return reservationStatusEnum;
            }
        }
        return null;
    }
}