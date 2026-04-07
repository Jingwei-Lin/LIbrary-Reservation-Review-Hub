package com.comp5619.libraryreservationreviewhub.tools;

import cn.hutool.json.JSONUtil;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试：UserInfoTool
 * 目标：覆盖所有分支，包括 userId=null, user=null, 有无借阅、异常捕获。
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class UserInfoToolIntegrationTest {

    @Autowired
    private UserInfoTool userInfoTool;

    @Autowired
    private UserService userService;

    /**
     *  场景 1：userId 为 null（未登录）
     */
    @Test
    void testGetUserInfo_NullUserId_ReturnsNotLoggedIn() {
        String result = userInfoTool.getUserInfo(null);
        System.out.println("Result for null userId: " + result);
        assertThat(result).contains("not_logged_in");
    }

    /**
     *  场景 2：user 不存在
     */
    @Test
    void testGetUserInfo_UserNotExist_ReturnsNotLoggedIn() {
        String result = userInfoTool.getUserInfo(99999L);
        System.out.println("Result for non-existent user: " + result);
        assertThat(result).contains("not_logged_in");
    }

    /**
     *  场景 3：正常用户（存在但无借阅记录）
     */
    @Test
    void testGetUserInfo_ValidUser_NoReservations() {
        long id = userService.userRegister(
                "no_resv_user@example.com", "password123", "password123", "Tom", "Lee", "0412345678"
        );

        String result = userInfoTool.getUserInfo(id);
        System.out.println("Result for valid user with no reservations: " + result);

        Map<String, Object> json = JSONUtil.toBean(result, Map.class);
        assertThat(json.get("status")).isEqualTo("logged_in");
        assertThat(result).contains("Tom").contains("Lee");
    }

    /**
     *  场景 4：捕获异常（模拟 service 抛出异常）
     */
    @Test
    void testGetUserInfo_ExceptionPath_ReturnsError() {
        UserInfoTool brokenTool = new UserInfoTool();
        // 没注入任何 service，会抛出 NullPointerException
        String result = brokenTool.getUserInfo(1L);
        System.out.println("Result for exception path: " + result);
        assertThat(result).contains("status").contains("error");
    }
}
