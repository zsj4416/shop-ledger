package com.shopledger.service;

import com.shopledger.dao.UserDao;
import com.shopledger.model.User;
import com.shopledger.util.PasswordUtil;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("请输入用户名和密码");
        }
        User user = userDao.findByUsername(username.trim());
        if (user == null || !PasswordUtil.verify(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        user.setPassword(null);
        return user;
    }

    public User findById(long id) {
        return userDao.findById(id);
    }
}
