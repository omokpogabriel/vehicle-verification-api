package com.naijavehicle.api.service;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.naijavehicle.api.dto.CustomResponseDto;
import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.enums.ChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class VregService {

    private final RestClient restClient;

    public VregService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public ScrapingResult verifyLicensePlate(String plateNumber, String askiInfo) {

        var scrapingResult = new ScrapingResult(
                plateNumber, "", "Unabled to fetch data", "",
                ChannelEnum.VEHICLE_INSURANCE.name
        );

        var getChasis = Arrays.stream(askiInfo.split("(\s+\\|\s+)"))
                .filter( value-> value.contains("Chassis:"))
                .findFirst();

        if(getChasis.isEmpty()){
            return scrapingResult;
        }

        String vin = getChasis.get().trim().split(":")[1];
        String url = "https://vreg.gov.ng/api/v1/validate-duty";

        Map<String, String> body = new HashMap<>();
        body.put("email", "worker" +RandomUtil.getPositiveInt()+"@gmail.com");
        body.put("vin" ,vin);

        log.info("the info -> {}", body);
        try {
            var jsonResponse = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                   .retrieve()
                   .toEntity(CustomResponseDto.class);

            if(jsonResponse.getStatusCode().is2xxSuccessful() ){
                boolean status = jsonResponse.getBody().dutyRecordFound();
                if(status){

                    scrapingResult.setPlateNumber(vin);
                    return new ScrapingResult(vin, "status", String.valueOf(status)
                            , ""+jsonResponse.getBody(), ChannelEnum.CUSTOM_REG.name);
                }
            }

            return scrapingResult;
        } catch (Exception e) {
            throw new RuntimeException("Custom papers API failed: " + e.getMessage(), e);
        }
    }
}


