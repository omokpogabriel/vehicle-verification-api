package com.naijavehicle.api.service;

import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.enums.AppConstant;
import org.springframework.stereotype.Service;

@Service
public class AppError {

    public static ScrapingResult exceptionFormat(String plateNumber, AppConstant type){
        return new ScrapingResult(plateNumber, "",
                "Error: Timeout/Unavailable",
                "", type.name);
    }
}
