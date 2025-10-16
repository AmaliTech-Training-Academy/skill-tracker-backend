package com.amalitech.user.service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BaseController {
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return new ResponseEntity<>("UserService Active ...", HttpStatus.OK);
    }
}
