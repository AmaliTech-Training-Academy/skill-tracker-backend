package com.amalitech.user.service.controller;

import com.amalitech.user.service.dto.ApiResponse;
import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@RequestBody UserRequestDTO userdto) {
        UserResponseDTO newUser = service.createUser(userdto);
     return ResponseEntity.ok(ApiResponse.success(newUser));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<UserResponseDTO>> verifyCode(
            @RequestParam("code") String code,
            @RequestParam("email") String email) {

        UserResponseDTO user = service.verifyCode(code, email).orElseThrow(() ->
                new RuntimeException("Invalid verification code"));

        return ResponseEntity.ok(ApiResponse.success(user));
    }
}