package com.guo.guopicturebackend.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guo.guopicturebackend.model.dto.user.UserQueryRequest;
import com.guo.guopicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guo.guopicturebackend.model.vo.LoginUserVO;
import com.guo.guopicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 44884
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-01-08 17:04:52
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);


    public String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    public LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);



    public UserVO getUserVO(User user) ;


    public List<UserVO> getUserVOList(List<User> userList);


    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

}
