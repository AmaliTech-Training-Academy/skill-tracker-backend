package com.amalitech.user.service.controller;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.model.User;
import com.amalitech.user.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class UserController {
    @Autowired
    private UserService service;

    @PostMapping("/register")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User newUser = service.createUser(user);
     return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }

    @PostMapping("/verify/{code}/{email}")
    public ResponseEntity<User> verifyCode(
            @PathVariable String code,
            @PathVariable String email) {
        User user = service.verifyCode(code, email);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }
}