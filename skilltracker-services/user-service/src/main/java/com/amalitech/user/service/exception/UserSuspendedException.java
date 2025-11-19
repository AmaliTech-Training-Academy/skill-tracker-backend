package com.amalitech.user.service.exception;

public class UserSuspendedException extends RuntimeException{
    public UserSuspendedException(String message){
        super(message);
    }
}
