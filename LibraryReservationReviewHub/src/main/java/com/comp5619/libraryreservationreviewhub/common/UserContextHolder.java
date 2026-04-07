package com.comp5619.libraryreservationreviewhub.common;

import com.comp5619.libraryreservationreviewhub.model.entity.User;
import lombok.extern.slf4j.Slf4j;

/**
 * 使用 ThreadLocal 存储当前会话的用户信息
 * 解决异步 SSE 场景下 HttpServletRequest 丢失的问题
 */
@Slf4j
public class UserContextHolder {

    private static final ThreadLocal<User> USER_CONTEXT = new ThreadLocal<>();

    // 设置当前线程的用户
    public static void set(User user) {
        USER_CONTEXT.set(user);

    }

    // 获取当前线程的用户
    public static User get() {

        return USER_CONTEXT.get();
    }

    // 清理 ThreadLocal，防止内存泄漏
    public static void clear() {
        USER_CONTEXT.remove();
    }
}
