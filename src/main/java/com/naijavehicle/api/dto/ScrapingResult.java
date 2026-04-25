package com.naijavehicle.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScrapingResult<T> {
    private String plateNumber;
    private String carMake;
    private String status;
    private T additionalInfo;
    private String type;
}
