package com.naijavehicle.api.service;

import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.dto.VehicleAdditionalInfoDTO;
import com.naijavehicle.api.enums.ChannelEnum;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

@Service
public class AutoRegService {

    private static final String TARGET_URL = "https://verify.autoreg.ng/";

    private final RestClient restClient;

    public AutoRegService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public ScrapingResult verifyLicensePlate(String plateNumber) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("regNumber", plateNumber);

            String html = restClient.post()
                    .uri(TARGET_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            Document doc = Jsoup.parse(html);

            org.jsoup.select.Elements leads = doc.select(".modal-body p.lead");
            if (!leads.isEmpty()) {
                String make = "AutoReg";
                StringBuilder details = new StringBuilder();

                for (org.jsoup.nodes.Element lead : leads) {
                    String text = lead.text().trim();
                    if (text.startsWith("Vehicle Make:")) {
                        make = text.replace("Vehicle Make:", "").trim();
                    } else {
                        details.append(text).append(" | ");
                    }
                }

                org.jsoup.select.Elements tds = doc.select(".modal-body table tbody tr td");
                if (tds.size() >= 4) {
                    details.append("Service: ").append(tds.get(0).text()).append(" | ");
                    details.append("Expiry: ").append(tds.get(3).text());
                }
                 var detailInfo  = VehicleAdditionalInfoDTO.fromRawString(details.toString());
                var status = checkExpiredDate(detailInfo.getExpiryDate()) ? "Valid" : "Expired";
                return new ScrapingResult(plateNumber, make, status,
                        detailInfo
                        , ChannelEnum.AUTO_REG.name);
            }

            String resultText = doc.select(".alert").text();
            if (resultText.isEmpty()) {
                resultText = doc.body().text().length() > 100 ? doc.body().text().substring(0, 100) + "..." : doc.body().text();
            }

            return new ScrapingResult(plateNumber, "AutoReg", resultText.isEmpty() ? "No Data Found" : resultText,
                    "", ChannelEnum.AUTO_REG.name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reach server API", e);
        }
    }

    public boolean checkExpiredDate(String inputDate) {

        // 1. Build a formatter that accepts multiple patterns
        DateTimeFormatter flexibleFormatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                .appendOptional(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                .appendOptional(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                .toFormatter();

        try {
            // 2. Parse the date using the flexible formatter
            LocalDate expiryDate = LocalDate.parse(inputDate, flexibleFormatter);

            // 3. Compare with "Now" (Current date: April 26, 2026)
            return expiryDate.isBefore(LocalDate.now());

        } catch (DateTimeParseException e) {
            System.err.println("The date format provided is not supported: " + inputDate);
            return false;
        }
    }
}
