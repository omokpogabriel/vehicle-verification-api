package com.naijavehicle.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InsuranceInfoDTO {

    @JacksonXmlProperty(localName = "Status")
    private String status;

    @JacksonXmlProperty(localName = "ChasisNo")
    private String chassisNumber;

    @JacksonXmlProperty(localName = "Color")
    private String color;

    @JacksonXmlProperty(localName = "PolicyNo")
    private String policyNumber;

    @JacksonXmlProperty(localName = "Model")
    private String model;

    @JacksonXmlProperty(localName = "Name")
    private String ownerName;

    @JacksonXmlProperty(localName = "NewRegistrationNo")
    private String plateNumber;

    @JacksonXmlProperty(localName = "CarMake")
    private String make;

    @JacksonXmlProperty(localName = "VehicleType")
    private String vehicleType;

    @JacksonXmlProperty(localName = "IssueDate")
    private String issueDate; // e.g., "28 AUG 2025"

    @JacksonXmlProperty(localName = "ExpirationDate")
    private String expiryDate;

    @JacksonXmlProperty(localName = "ECOWASBrownCard")
    private String ecowasCard;
}