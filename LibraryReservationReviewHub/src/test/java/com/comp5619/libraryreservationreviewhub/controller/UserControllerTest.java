package com.comp5619.libraryreservationreviewhub.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comp5619.libraryreservationreviewhub.common.PageResult;
import com.comp5619.libraryreservationreviewhub.model.dto.user.UserLoginRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.user.UserQueryRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.user.UserRegisterRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.user.UserUpdateRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.LoginUserVO;
import com.comp5619.libraryreservationreviewhub.model.vo.UserVO;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.nullValue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 单元测试（Mockito 版本）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void userRegister_success() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setUserAccount("john@example.com");
        req.setUserPassword("password123");
        req.setCheckPassword("password123");
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setPhone("0400000000");

        when(userService.userRegister(
                eq("john@example.com"),
                eq("password123"),
                eq("password123"),
                eq("John"),
                eq("Doe"),
                eq("0400000000")
        )).thenReturn(123L);

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(123)))
                .andExpect(jsonPath("$.code", anyOf(is(0), nullValue())));
    }

    @Test
    void userLogin_success() throws Exception {
        UserLoginRequest req = new UserLoginRequest();
        req.setUserAccount("john@example.com");
        req.setUserPassword("password123");

        LoginUserVO vo = new LoginUserVO();
        vo.setId(1L);
        vo.setUserName("John Doe");
        vo.setUserRole("user");

        when(userService.userLogin(
                eq("john@example.com"),
                eq("password123"),
                any(HttpServletRequest.class)
        )).thenReturn(vo);

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName", is("John Doe")))
                .andExpect(jsonPath("$.data.userRole", is("user")))
                .andExpect(jsonPath("$.code", anyOf(is(0), nullValue())));
    }

    @Test
    void getLoginUser_success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");

        LoginUserVO vo = new LoginUserVO();
        vo.setId(1L);
        vo.setUserName("John Doe");
        vo.setUserRole("user");

        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(user);
        when(userService.getLoginUserVO(any(User.class))).thenReturn(vo);

        mockMvc.perform(get("/user/get/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName", is("John Doe")))
                .andExpect(jsonPath("$.code", anyOf(is(0), nullValue())));
    }

    @Test
    void userLogout_success() throws Exception {
        when(userService.userLogout(any(HttpServletRequest.class))).thenReturn(true);

        mockMvc.perform(post("/user/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(true)))
                .andExpect(jsonPath("$.code", anyOf(is(0), nullValue())));
    }

    @Test
    void getUserProfile_success() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setFirstName("Alice");
        user.setLastName("Smith");

        UserVO vo = new UserVO();
        vo.setId(2L);
        vo.setUserName("Alice Smith");
        vo.setUserRole("user");

        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(user);
        when(userService.getUserVO(any(User.class))).thenReturn(vo);

        mockMvc.perform(get("/user/getProfile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName", is("Alice Smith")))
                .andExpect(jsonPath("$.code", anyOf(is(0), nullValue())));
    }

    @Test
    void updateUserProfile_success() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setFirstName("Bob");
        user.setLastName("Lee");

        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("Bob");
        req.setLastName("Lee");
        req.setEmail("bob@example.com");
        req.setPhone("0400123456");

        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(user);
        when(userService.updateUserProfile(any(UserUpdateRequest.class), any(User.class)))
                .thenReturn(true);

        mockMvc.perform(put("/user/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(true)))
                .andExpect(jsonPath("$.code", anyOf(is(0), nullValue())));
    }





    @Test
    void testUpdateUser_AsAdmin_Success() throws Exception {

        // 模拟 Service 更新成功
        when(userService.updateById(any(User.class))).thenReturn(true);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setId(1L);
        request.setFirstName("UpdatedName");
        request.setLastName("Lin");
        request.setPhone("1234567890");

        mockMvc.perform(post("/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }
    @Test
    void testGetUserById_AsAdmin_Success() throws Exception {


        // 模拟数据库中存在该用户
        User user = new User();
        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");

        when(userService.getById(1L)).thenReturn(user);

        mockMvc.perform(get("/user/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }
    @Test
    void testListUserVOByPage_AsAdmin_Success() throws Exception {
        // 准备模拟分页数据
        Page<User> userPage = new Page<>(1, 5, 1);
        User user1 = new User();
        user1.setId(1L);
        user1.setFirstName("Alice");
        user1.setLastName("Smith");
        userPage.setRecords(List.of(user1));

        // 模拟 UserVOList
        UserVO vo1 = new UserVO();
        vo1.setId(1L);
        vo1.setUserName("Alice Smith");
        vo1.setUserRole("user");
        List<UserVO> voList = List.of(vo1);

        // Mock service 层调用
        when(userService.page(any(Page.class), any())).thenReturn(userPage);
        when(userService.getUserVOList(any())).thenReturn(voList);

        // 构造请求参数
        UserQueryRequest queryRequest = new UserQueryRequest();
        queryRequest.setCurrent(1);
        queryRequest.setPageSize(5);

        mockMvc.perform(post("/user/list/page/vo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].userName").value("Alice Smith"))
                .andExpect(jsonPath("$.data.records[0].userRole").value("user"))
                .andExpect(jsonPath("$.code", anyOf(is(0), nullValue())));
    }
}
