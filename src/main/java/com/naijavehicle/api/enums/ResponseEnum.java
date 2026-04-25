package com.naijavehicle.api.enums;

public enum ResponseEnum {

    SUCCESS("00"),
    FAILED("01");

    private ResponseEnum(String code) {
        this.code = code;
    }

    public String code;
}