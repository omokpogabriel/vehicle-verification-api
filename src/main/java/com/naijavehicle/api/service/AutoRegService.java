package com.naijavehicle.api.service;

import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.dto.VehicleAdditionalInfoDTO;
import com.naijavehicle.api.enums.ChannelEnum;
import com.naijavehicle.api.enums.ResponseEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AutoRegService {

    private static final String TARGET_URL = "https://verify.autoreg.ng/";

    private static final Pattern LEAD_PATTERN = Pattern.compile(
            "<p[^>]*class=\"[^\"]*\\blead\\b[^\"]*\"[^>]*>(.*?)</p>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern TD_PATTERN = Pattern.compile(
            "<td[^>]*>(.*?)</td>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern ALERT_PATTERN = Pattern.compile(
            "<[^>]*class=\"[^\"]*\\balert\\b[^\"]*\"[^>]*>(.*?)</[a-z]+>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern BODY_PATTERN = Pattern.compile(
            "<body[^>]*>(.*?)</body>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final RestClient restClient;

    public AutoRegService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public ScrapingResult<VehicleAdditionalInfoDTO> verifyLicensePlate(String plateNumber) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("regNumber", plateNumber);

            String html = restClient.post()
                    .uri(TARGET_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            // Scope to modal-body to avoid false matches elsewhere in the page
            String scope = modalBodySection(html);
            log.info("hello -> {}", "autoreg");         List<String> leads = new ArrayList<>();
            Matcher leadMatcher = LEAD_PATTERN.matcher(scope);
            while (leadMatcher.find()) {
                leads.add(stripTags(leadMatcher.group(1)));
            }

            if (!leads.isEmpty()) {
                String make = "AutoReg";
                StringBuilder details = new StringBuilder();

                for (String text : leads) {
                    text = text.trim();
                    if (text.startsWith("Vehicle Make:")) {
                        make = text.replace("Vehicle Make:", "").trim();
                    } else {
                        details.append(text).append(" | ");
                    }
                }

                List<String> tds = new ArrayList<>();
                Matcher tdMatcher = TD_PATTERN.matcher(scope);
                while (tdMatcher.find()) {
                    tds.add(stripTags(tdMatcher.group(1)));
                }
                if (tds.size() >= 4) {
                    details.append("Service: ").append(tds.get(0)).append(" | ");
                    details.append("Expiry: ").append(tds.get(3));
                }

                var detailInfo = VehicleAdditionalInfoDTO.fromRawString(details.toString());
                var status = checkExpiredDate(detailInfo.getExpiryDate()) ? "Valid" : "Expired";
                var code = checkExpiredDate(detailInfo.getExpiryDate()) ? ResponseEnum.SUCCESS.code
                        : ResponseEnum.FAILED.code ;
                return new ScrapingResult<>(plateNumber, make, status, code,
                        detailInfo, ChannelEnum.AUTO_REG.name());
            }

            String resultText = "";
            Matcher alertMatcher = ALERT_PATTERN.matcher(html);
            if (alertMatcher.find()) {
                resultText = stripTags(alertMatcher.group(1)).trim();
            }

            if (resultText.isEmpty()) {
                Matcher bodyMatcher = BODY_PATTERN.matcher(html);
                if (bodyMatcher.find()) {
                    resultText = stripTags(bodyMatcher.group(1)).replaceAll("\\s+", " ").trim();
                    if (resultText.length() > 100) resultText = resultText.substring(0, 100) + "...";
                }
            }

            return new ScrapingResult<>(plateNumber, "AutoReg",
                    resultText.isEmpty() ? "No Data Found" : resultText,
                    ResponseEnum.FAILED.code, null, ChannelEnum.AUTO_REG.name());

        } catch (Exception e) {
            throw new RuntimeException("Failed to reach server API", e);
        }
    }

    // Returns the substring starting from the modal-body section to scope regex matches
    private String modalBodySection(String html) {
        int idx = html.toLowerCase().indexOf("modal-body");
        return idx >= 0 ? html.substring(idx) : html;
    }

    private String stripTags(String html) {
        return HtmlUtils.htmlUnescape(TAG_PATTERN.matcher(html).replaceAll(""));
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
            return expiryDate.isAfter(LocalDate.now());

        } catch (DateTimeParseException e) {
            System.err.println("The date format provided is not supported: " + inputDate);
            return false;
        }
    }
}
