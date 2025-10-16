package com.amalitech.user.service.controller;

import com.amalitech.user.service.dto.UserRequestDTO;
import com.amalitech.user.service.dto.UserResponseDTO;
import com.amalitech.user.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    @Autowired
    private UserService service;

    @PostMapping()
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody UserRequestDTO user) {
        UserResponseDTO newUser = service.createUser(user);
     return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }
}
