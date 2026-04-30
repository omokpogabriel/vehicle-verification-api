package com.naijavehicle.api.service;

import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.enums.ChannelEnum;


public class AppError {

    public static <T> ScrapingResult<T> exceptionFormat(String plateNumber, ChannelEnum type){
        return new ScrapingResult<T> (plateNumber, "",
                "Error: Timeout/Unavailable",
                null, type.name());
    }
}
