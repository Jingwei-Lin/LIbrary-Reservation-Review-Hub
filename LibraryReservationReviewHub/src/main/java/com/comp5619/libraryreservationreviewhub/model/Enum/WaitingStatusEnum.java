package com.comp5619.libraryreservationreviewhub.model.Enum;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum WaitingStatusEnum {
    WAITING("waiting"),
    NOTIFIED("notified"),
    CANCELLED("cancelled"),
    FULFILLED("fulfilled");

    @EnumValue
    @JsonValue
    private final String value;

    WaitingStatusEnum(String value) {
        this.value = value;
    }
}
