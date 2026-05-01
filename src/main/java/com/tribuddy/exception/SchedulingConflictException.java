package com.tribuddy.exception;

public class SchedulingConflictException extends RuntimeException{
    public SchedulingConflictException(String message){
        super(message);
    }
}
