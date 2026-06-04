package com.naijavehicle.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckItemDTO {
    private String type;
    private String code;
    private String status;
}
