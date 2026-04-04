package com.naijavehicle.api.service;

import com.naijavehicle.api.dto.PayVisResponseDto;
import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.enums.AppConstant;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PayvisService {

    private static final String TARGET_URL = "https://payvis.ng/api/search?key=plate_number&value=";
    private final RestClient restClient;

    public PayvisService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public ScrapingResult verifyLicensePlate(String plateNumber) {
        try {
            var jsonResponse = restClient.get()
                    .uri(TARGET_URL+plateNumber)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Referer", "https://payvis.ng/summary?sv=MUS549JR&st=pn")
                    .retrieve()
                    .toEntity(PayVisResponseDto.class);

            if(jsonResponse.getStatusCode().is2xxSuccessful()){
                var body = jsonResponse.getBody();

                String status = body.compoundBills().length == 0
                        &&  body.externalBills().length == 0
                        &&  body.localBills().length == 0
                        ? "No Data Found" : "Data Found";
                return new ScrapingResult(plateNumber, "Payvis", status, body, AppConstant.PAYVIS.name);
            }

            return new ScrapingResult(
                    plateNumber, "", "Unabled to fetch data", "",
                    AppConstant.PAYVIS.name
            );
        } catch (Exception e) {
            throw new RuntimeException("Payvis API failed: " + e.getMessage(), e);
        }
    }
}

