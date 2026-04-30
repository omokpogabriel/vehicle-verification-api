package com.naijavehicle.api.service;

import com.naijavehicle.api.dto.PayVisResponseDto;
import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.enums.ChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class PayvisService {
    private static final String TARGET_URL = "https://payvis.ng/api/search?key=plate_number&value=";
    private final RestClient restClient;

    public PayvisService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public ScrapingResult<PayVisResponseDto> verifyLicensePlate(String plateNumber) {
        try {
            var jsonResponse = restClient.get()
                    .uri(TARGET_URL+plateNumber)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Referer", "https://payvis.ng/summary?sv=MUS549JR&st=pn")
                    .retrieve()
                 // .body(String.class);
             .toEntity(PayVisResponseDto.class);
            log.info("the response jj->{} ->{}",jsonResponse.getBody(), TARGET_URL+plateNumber);
            if(jsonResponse.getStatusCode().is2xxSuccessful()){
                var body = jsonResponse.getBody();

                String status = body.compoundBills().size() == 0
                        &&  body.externalBills().size() == 0
                        &&  body.localBills().size() == 0
                        ? "No Data Found" : "Data Found";
                return new ScrapingResult<>(plateNumber, "Payvis", status, body, ChannelEnum.PAY_VIS.name());
            }

            return new ScrapingResult<>(
                    plateNumber, "", "Unabled to fetch data", null,
                    ChannelEnum.PAY_VIS.name()
            );
        } catch (Exception e) {
            throw new RuntimeException("Payvis API failed: " + e.getMessage(), e);
        }
    }
}

