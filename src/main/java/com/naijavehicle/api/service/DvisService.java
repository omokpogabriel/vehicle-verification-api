package com.naijavehicle.api.service;

import com.naijavehicle.api.dto.RoadWorthinessDto;
import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.enums.ChannelEnum;
import com.naijavehicle.api.enums.ResponseEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class DvisService {

    private final RestClient restClient;

    public DvisService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public ScrapingResult<String> verifyLicensePlate(String plateNumber) {
        String url = "https://dvis.lg.gov.ng/verify/api.php";
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("reg_no", plateNumber);

            RoadWorthinessDto jsonResponse = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(RoadWorthinessDto.class);

            String code = "";
            String readableStatus = "";
            String appInfo = "";
            log.info("the response from dvis is {}", jsonResponse);
            if (jsonResponse == null || jsonResponse.getMessage() == null) {
                code = ResponseEnum.FAILED.code;
                readableStatus = ResponseEnum.FAILED.name();

            } else {
                String message = jsonResponse.getMessage().toLowerCase();
                readableStatus = (message.contains("Invalid") || message.contains("not exist") )? message : "Valid";
                code = readableStatus.equalsIgnoreCase("Valid") ? ResponseEnum.SUCCESS.code
                        : ResponseEnum.FAILED.code;
                appInfo = jsonResponse.getMessage() + " -Expire: " + jsonResponse.getRwcExp();
            }

            return new ScrapingResult<>(plateNumber, "DVIS", readableStatus,
                    code,
                    appInfo,
                    ChannelEnum.DIVS.name(), false);
        } catch (Exception e) {
            throw new RuntimeException("DVIS API failed: " + e.getMessage(), e);
        }
    }
}
