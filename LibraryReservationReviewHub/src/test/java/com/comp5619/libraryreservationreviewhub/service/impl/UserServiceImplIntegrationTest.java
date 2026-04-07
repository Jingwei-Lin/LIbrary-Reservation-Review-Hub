package com.comp5619.libraryreservationreviewhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.model.dto.user.UserQueryRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.user.UserUpdateRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.LoginUserVO;
import com.comp5619.libraryreservationreviewhub.model.vo.UserVO;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test for UserServiceImpl
 * 自动加载 Spring Boot + MyBatis + 事务自动回滚
 */
@SpringBootTest
@Transactional
class UserServiceImplIntegrationTest {

    @Autowired
    private UserService userService;

    // ============================================================
    // A. userRegister() 用户注册测试
    // ============================================================

    @Test
    void testUserRegister_Success() {
        long id = userService.userRegister(
                "register_success@example.com", "password123", "password123", "John", "Smith", "0400000000");
        assertTrue(id > 0);
    }

    @Test
    void testUserRegister_AccountTooShort_ThrowsException() {
        assertThrows(BusinessException.class, () ->
                userService.userRegister("abc", "password123", "password123", "A", "B", "0400000000"));
    }

    @Test
    void testUserRegister_AccountExists_ThrowsException() {
        userService.userRegister("dup@example.com", "password123", "password123", "A", "B", "0400000000");
        assertThrows(BusinessException.class, () ->
                userService.userRegister("dup@example.com", "password123", "password123", "A", "B", "0400000000"));
    }

    @Test
    void testUserRegister_PasswordMismatch() {
        assertThrows(BusinessException.class, () ->
                userService.userRegister("user@example.com", "password123", "wrong", "T", "U", "0400000000"));
    }

    @Test
    void testUserRegister_BlankParameters_ThrowsException() {
        assertThrows(BusinessException.class, () ->
                userService.userRegister("", "", "", "", "", ""));
    }

    @Test
    void testUserRegister_ShortPassword_ThrowsException() {
        assertThrows(BusinessException.class, () ->
                userService.userRegister("short@example.com", "123", "123", "A", "B", "0400000000"));
    }

    @Test
    void testUserRegister_InvalidEmailFormat_ThrowsException() {
        assertThrows(BusinessException.class, () ->
                userService.userRegister("notAnEmail", "password123", "password123", "Tom", "Lee", "0400000000"));
    }

    @Test
    void testUserRegister_InvalidPhoneFormat_ThrowsException() {
        assertThrows(BusinessException.class, () ->
                userService.userRegister("safe@example.com", "password123", "password123", "Tom", "Lee", "04xx000"));
    }

    @Test
    void testUserRegister_SQLInjectionDetected() {
        String[] badInputs = {
                "user@example.com' OR '1'='1",
                "robert'); --",
                "union select * from Users",
                "admin'/*",
                "drop table Users;"
        };
        for (String bad : badInputs) {
            assertThrows(BusinessException.class, () ->
                    userService.userRegister(bad, "password123", "password123", "A", "B", "0400000000"));
        }
    }


    // ============================================================
    // B. userLogin() 登录测试
    // ============================================================

    @Test
    void testUserLogin_Success() {
        userService.userRegister("login@example.com", "password123", "password123", "Jane", "Lee", "0400000000");
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginUserVO vo = userService.userLogin("login@example.com", "password123", request);

        assertEquals("Jane Lee", vo.getUserName());
        assertEquals("user", vo.getUserRole());
        assertNotNull(request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE));
    }

    @Test
    void testUserLogin_BlankParameters_ThrowsException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BusinessException.class, () -> userService.userLogin("", "", request));
    }

    @Test
    void testUserLogin_UserNotExist_ThrowsException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BusinessException.class, () ->
                userService.userLogin("nonexist@example.com", "password123", request));
    }

    @Test
    void testUserLogin_WrongPassword_ThrowsException() {
        userService.userRegister("wrongpass@example.com", "password123", "password123", "X", "Y", "0400000000");
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BusinessException.class, () ->
                userService.userLogin("wrongpass@example.com", "wrong", request));
    }

    @Test
    void testUserLogin_UserBanned_ThrowsException() {
        long id = userService.userRegister("ban@example.com", "password123", "password123", "Ban", "User", "0400000000");
        User user = userService.getById(id);
        user.setStatus(0);
        userService.updateById(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BusinessException.class, () ->
                userService.userLogin("ban@example.com", "password123", request));
    }

    // ============================================================
    // C. getLoginUser() 测试
    // ============================================================

    @Test
    void testGetLoginUser_Success() {
        long id = userService.userRegister("getlogin@example.com", "password123", "password123", "Jane", "Lee", "0400000000");
        MockHttpServletRequest request = new MockHttpServletRequest();
        userService.userLogin("getlogin@example.com", "password123", request);

        User current = userService.getLoginUser(request);
        assertEquals(id, current.getId());
    }

    @Test
    void testGetLoginUser_NotLoggedIn_ThrowsException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BusinessException.class, () -> userService.getLoginUser(request));
    }

    @Test
    void testGetLoginUser_UserDeleted_ThrowsException() {
        long id = userService.userRegister("deleted@example.com", "password123", "password123", "Del", "User", "0400000000");
        MockHttpServletRequest request = new MockHttpServletRequest();
        userService.userLogin("deleted@example.com", "password123", request);
        userService.removeById(id);

        assertThrows(BusinessException.class, () -> userService.getLoginUser(request));
    }

    @Test
    void testGetLoginUser_UserBanned_ThrowsException() {
        long id = userService.userRegister("ban_get@example.com", "password123", "password123", "Ban", "Get", "0400000000");
        MockHttpServletRequest request = new MockHttpServletRequest();
        userService.userLogin("ban_get@example.com", "password123", request);
        User banned = userService.getById(id);
        banned.setStatus(0);
        userService.updateById(banned);

        assertThrows(BusinessException.class, () -> userService.getLoginUser(request));
    }

    // ============================================================
    // D. userLogout() 测试
    // ============================================================

    @Test
    void testUserLogout_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(UserConstant.USER_LOGIN_STATE, new User());
        request.setSession(session);
        assertTrue(userService.userLogout(request));
    }

    @Test
    void testUserLogout_NotLoggedIn_ThrowsException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BusinessException.class, () -> userService.userLogout(request));
    }

    // ============================================================
    // E. updateUserProfile() 测试
    // ============================================================

    @Test
    void testUpdateUserProfile_Success() {
        long id = userService.userRegister("update@example.com", "password123", "password123", "Old", "Name", "0400000000");
        User user = userService.getById(id);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("New");
        req.setLastName("Name");
        req.setEmail("update@example.com");
        req.setPhone("0412345678");

        boolean result = userService.updateUserProfile(req, user);
        assertTrue(result);
    }

    @Test
    void testUpdateUserProfile_PasswordChanged_Success() {
        long id = userService.userRegister("changepw@example.com", "password123", "password123", "Tom", "Lee", "0400000000");
        User user = userService.getById(id);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("Tom");
        req.setLastName("Lee");
        req.setEmail("changepw@example.com");
        req.setPassword("newPassword123");
        req.setConfirmPassword("newPassword123");

        assertTrue(userService.updateUserProfile(req, user));
    }

    @Test
    void testUpdateUserProfile_EmailExists_ThrowsException() {
        userService.userRegister("a@example.com", "password123", "password123", "A", "B", "0400000000");
        long id2 = userService.userRegister("b@example.com", "password123", "password123", "C", "D", "0400000002");
        User user2 = userService.getById(id2);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("X");
        req.setLastName("Y");
        req.setEmail("a@example.com");

        assertThrows(BusinessException.class, () -> userService.updateUserProfile(req, user2));
    }

    @Test
    void testUpdateUserProfile_PasswordMismatch_ThrowsException() {
        long id = userService.userRegister("pwmm@example.com", "password123", "password123", "Old", "User", "0400000000");
        User user = userService.getById(id);
        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("New");
        req.setLastName("User");
        req.setEmail("pwmm@example.com");
        req.setPassword("pass1234");
        req.setConfirmPassword("different");

        assertThrows(BusinessException.class, () -> userService.updateUserProfile(req, user));
    }

    // ============================================================
    // F. getQueryWrapper() 测试
    // ============================================================

    @Test
    void testGetQueryWrapper_Success() {
        UserQueryRequest req = new UserQueryRequest();
        req.setId(1L);
        req.setUserAccount("abc@example.com");
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setIsAdmin(true);
        req.setSortField("id");
        req.setSortOrder("ascend");
        QueryWrapper<User> wrapper = userService.getQueryWrapper(req);
        assertNotNull(wrapper);
    }

    @Test
    void testGetQueryWrapper_NullRequest_ThrowsException() {
        assertThrows(BusinessException.class, () -> userService.getQueryWrapper(null));
    }

    // ============================================================
    // G. 其他方法测试
    // ============================================================

    @Test
    void testIsAdmin() {
        User admin = new User();
        admin.setIsAdmin(true);
        assertTrue(userService.isAdmin(admin));

        User normal = new User();
        normal.setIsAdmin(false);
        assertFalse(userService.isAdmin(normal));
    }

    @Test
    void testGetLoginUserVO_Null_ReturnsNull() {
        assertNull(userService.getLoginUserVO(null));
    }

    @Test
    void testGetUserVO_Null_ReturnsNull() {
        assertNull(userService.getUserVO(null));
    }

    @Test
    void testGetUserVOList_Empty_ReturnsEmptyList() {
        List<UserVO> list = userService.getUserVOList(new ArrayList<>());
        assertTrue(list.isEmpty());
    }

    @Test
    void testGetUserVOList_NonEmpty() {
        User u1 = new User();
        u1.setFirstName("Alice");
        u1.setLastName("Smith");
        u1.setIsAdmin(false);
        List<UserVO> list = userService.getUserVOList(List.of(u1));
        assertEquals("Alice Smith", list.get(0).getUserName());
    }

    @Test
    void testGetUserNameById() {
        long id = userService.userRegister("getname@example.com", "password123", "password123", "Tom", "Lee", "0400000000");
        String name = userService.getUserNameById(id);
        assertEquals("Tom Lee", name);
    }

    @Test
    void testGetEncryptPassword_Consistency() {
        String encrypted1 = userService.getEncryptPassword("password123");
        String encrypted2 = userService.getEncryptPassword("password123");
        assertEquals(encrypted1, encrypted2);
    }
}
