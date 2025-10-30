package com.amalitech.user.service.exception;

public class UnverifiedUserException extends RuntimeException{
    public UnverifiedUserException(String message) {
        super(message);
    }
}
