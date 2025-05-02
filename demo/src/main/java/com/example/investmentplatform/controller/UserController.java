package com.example.investmentplatform.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.investmentplatform.entity.User;
import com.example.investmentplatform.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        logger.info("User before save: {}", user);
        return userService.register(user);
    }

    @GetMapping("/{email}")
    public Optional<User> getUserByEmail(@PathVariable String email) {
        return userService.findByEmail(email);
    }
}
