package com.naijavehicle.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoadWorthinessDto {
    private String status;
    private String message;
    @JsonAlias("RwcNo")
    private String rwcNo;
    @JsonAlias("RwcExp")
    private String rwcExp;
    @JsonAlias("RwcStatus")
    private String rwcStatus;
    @JsonAlias("VehCate")
    private String vehCate;
}
