package com.naijavehicle.api.enums;

public enum AppConstant {
    VEHICLE_LICENSE("Vehicle License ASKNIID"),
    AUTOREG(" verify.autoreg.ng"),
    PAYVIS("Traffic Offence"),
    CUSTOM_REG("Vehicle Import Duty"),
    DIVS("Road worthiness");

    private AppConstant(String name){
        this.name = name;
    }

    public String name;
}
