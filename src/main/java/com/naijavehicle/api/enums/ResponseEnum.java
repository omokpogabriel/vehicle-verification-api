package com.naijavehicle.api.enums;

import com.fasterxml.jackson.annotation.JsonSubTypes;

public enum ResponseEnum {

    SUCCESS("00"),
    FAILED("01");


    private ResponseEnum(String code) {
        this.code = code;
    }

    public String code;
}