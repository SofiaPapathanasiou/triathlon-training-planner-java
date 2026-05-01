package com.tribuddy.exception;

public class InvalidWorkoutException extends RuntimeException{
    public InvalidWorkoutException(String message){
        super(message);
    }
}
