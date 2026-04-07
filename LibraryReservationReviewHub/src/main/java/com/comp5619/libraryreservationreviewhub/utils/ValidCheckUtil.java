package com.comp5619.libraryreservationreviewhub.utils;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * 防护工具类
 */
public class ValidCheckUtil {

    public static boolean isValidEmail(String email) {
        if (StrUtil.isBlank(email)) return false;
        // 简单邮箱正则：xxxxx@yyyy.zz
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(emailRegex, email);
    }

    public static boolean isValidPhone(String phone) {
        if (StrUtil.isBlank(phone)) return false;
        // 只允许数字，长度 8 到 15
        String phoneRegex = "^\\d{8,15}$";
        return Pattern.matches(phoneRegex, phone);
    }


}
