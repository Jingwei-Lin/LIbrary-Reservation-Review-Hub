package com.comp5619.libraryreservationreviewhub.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comp5619.libraryreservationreviewhub.annotation.AuthCheck;
import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.DeleteRequest;
import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.model.dto.user.*;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.model.vo.LoginUserVO;
import com.comp5619.libraryreservationreviewhub.model.vo.UserVO;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Date; // Existing import
import java.util.List; // Added import

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户管理控制器
 * User Management Controller
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {

    @Resource
    private UserService userService;

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount(); // email
        System.out.println(userAccount);
        String userPassword = userRegisterRequest.getUserPassword();
        System.out.println(userPassword);
        String checkPassword = userRegisterRequest.getCheckPassword();
        System.out.println(checkPassword);
        String firstName = userRegisterRequest.getFirstName();
        System.out.println(firstName);
        String lastName = userRegisterRequest.getLastName();
        System.out.println(lastName);
        String phone = userRegisterRequest.getPhone();
        System.out.println(phone);
        long result = userService.userRegister(userAccount, userPassword, checkPassword, firstName, lastName, phone);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
            HttpServletRequest request) {
        log.info("Login request: {}", userLoginRequest);
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount(); // email
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @GetMapping("/getProfile")
    public BaseResponse<UserVO> getUserProfile(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getUserVO(user));
    }

    @PutMapping("/updateProfile")
    public BaseResponse<Boolean> updateUserProfile(@RequestBody UserUpdateRequest req, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.updateUserProfile(req, user));
    }

    // /**
    // * 创建用户
    // */
    // @PostMapping("/add")
    // @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    // public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest)
    // {
    // ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
    // User user = new User();
    // BeanUtil.copyProperties(userAddRequest, user);
    // // 默认密码
    // final String DEFAULT_PASSWORD = "12345678";
    // String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
    // user.setUserPassword(encryptPassword);
    // user.setAdmin(false); // Default false
    // user.setCreateTime(new Date()); // Use imported Date class
    // // 插入数据库
    // boolean result = userService.save(user);
    // ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    // return ResultUtils.success(user.getId());
    // }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    // /**
    // * 根据 id 获取包装类
    // */
    // @GetMapping("/get/vo")
    // public BaseResponse<UserVO> getUserVOById(long id) {
    // BaseResponse<User> response = getUserById(id);
    // User user = response.getData();
    // return ResultUtils.success(userService.getUserVO(user));
    // }

    // /**
    // * 删除用户
    // */
    // @PostMapping("/delete")
    // @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    // public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest
    // deleteRequest) {
    // if (deleteRequest == null || deleteRequest.getId() <= 0) {
    // throw new BusinessException(ErrorCode.PARAMS_ERROR);
    // }
    // boolean b = userService.removeById(deleteRequest.getId());
    // return ResultUtils.success(b);
    // }
    //
    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        user.setCreateTime(user.getCreateTime()); // Preserve if updating
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    //
    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        // 防止空参数
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }


}