package com.yuan.exception;

public class AppException extends RuntimeException{
    private int code;
    public AppException(int code, String message){
        super(message);
        this.code = code;
    }

    public int getCode(){
        return code;
    }
}
