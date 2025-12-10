package com.back.web7_9_codecrete_be.domain.users.controller;

import com.back.web7_9_codecrete_be.domain.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/users/")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
}
