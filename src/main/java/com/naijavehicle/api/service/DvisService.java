package com.naijavehicle.api.service;

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

            String jsonResponse = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            String readableStatus = jsonResponse;
            String code = "";
            try {
                if (jsonResponse.contains("\"message\"")) {
                    String msg = jsonResponse.split("\"message\":\"")[1].split("\"")[0];
                    readableStatus = msg;
                    if (jsonResponse.contains("\"RwcExp\"")) {
                        String exp = jsonResponse.split("\"RwcExp\":\"")[1].split("\"")[0];
                        if (!exp.isEmpty()) {
                            readableStatus += " (Expires: " + exp + ")";
                        }
                        code = ResponseEnum.FAILED.code;
                    }else{
                        code = ResponseEnum.SUCCESS.code;
                        readableStatus = ResponseEnum.SUCCESS.name();
                    }
                }
            } catch (Exception ex) {
                // Ignore parse errors, keep raw response
            }

            return new ScrapingResult<>(plateNumber, "DVIS", readableStatus, code,"",
                    ChannelEnum.DIVS.name());
        } catch (Exception e) {
            throw new RuntimeException("DVIS API failed: " + e.getMessage(), e);
        }
    }
}
