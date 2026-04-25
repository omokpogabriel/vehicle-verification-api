package com.naijavehicle.api.enums;

public enum ChannelEnum {
    VEHICLE_INSURANCE("Vehicle Insurance"), //ASKnIID
    AUTO_REG("Vehicle License"), //verify.autoreg.ng
    PAY_VIS("Traffic Offence"),
    CUSTOM_REG("Vehicle Import Duty"),
    DIVS("Road worthiness");

    private ChannelEnum(String name){
        this.name = name;
    }

    public String name;
}
