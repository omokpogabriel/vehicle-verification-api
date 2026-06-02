package com.naijavehicle.api.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.naijavehicle.api.dto.InsuranceInfoDTO;
import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.enums.ChannelEnum;
import com.naijavehicle.api.enums.ResponseEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AskNiidInsuranceService {

    private static final String TARGET_URL =
            "https://niid.org/NIA_API/Service.asmx/Vehicle_PolicyVerification";

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private final RestClient restClient;

    @Value("${askniid.insurance.user}")
    private String username;

    @Value("${askniid.insurance.pass}")
    private String password;

    // This operation supports searching by plate or policy, depending on what NIID expects for SearchType.
    // Defaulting to plate lookup (matches the prior UI behavior: "Registration Number").
    @Value("${askniid_search_type:Registration Number}")
    private String searchType;

    public AskNiidInsuranceService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public String verifyLicensePlate(String plateNumber) {
        try {
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                throw new IllegalStateException("Missing NIID credentials.");
            }

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("SearchString", plateNumber); // {{plate_or_policy}}
            formData.add("SearchType", searchType); // {{search_type}}
            formData.add("Username", username); // {{askniid_insurance_user}}
            formData.add("Password", password); // {{askniid_insurance_pass}}

            return restClient.post()
                    .uri(TARGET_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

        } catch (Exception e) {
            log.info("the vehicle exception -> {}", e.getMessage());
            return null;
        }
    }

    public ScrapingResult<InsuranceInfoDTO> decodeAskNiidResult(String responseXml, String plateNumber) {
        try {
            log.info("it entered here ->");
            ScrapingResult<InsuranceInfoDTO> scrapResult = new ScrapingResult<>();
            if (responseXml == null) {
                return VerificationServiceImpl.getPendingRequest(plateNumber);
            }
            String soapResult = extractSoapString(responseXml);
            var result = parseInsuranceXml(soapResult);//(plateNumber, soapResult);
            scrapResult.setPlateNumber(plateNumber);
            scrapResult.setCarMake(result.getMake());
            scrapResult.setAdditionalInfo(result);
            scrapResult.setRetryState(false);
            scrapResult.setStatus(result.getStatus().equalsIgnoreCase("1") ?
                    ResponseEnum.SUCCESS.name() : ResponseEnum.FAILED.name());
            scrapResult.setCode(result.getStatus().equalsIgnoreCase("1") ?
                    ResponseEnum.SUCCESS.code : ResponseEnum.FAILED.code);
            scrapResult.setType(ChannelEnum.VEHICLE_INSURANCE.name());
            return scrapResult;
        } catch (Exception e) {
            return new ScrapingResult<>(plateNumber, "Not found",
                    "Error: " + e.getMessage(),
                    ResponseEnum.FAILED.code,
                    null,
                    "Vehicle License", false);
        }
    }

    public InsuranceInfoDTO parseInsuranceXml(String xmlResult) throws Exception {
        String cleanXml = xmlResult.replace("result -> ", "").trim();
        String unescapedXml = HtmlUtils.htmlUnescape(cleanXml);
        return XML_MAPPER.readValue(unescapedXml, InsuranceInfoDTO.class);
    }

    /**
     * NIID returns an XML wrapper like:
     * <string xmlns="http://hts.org/">some result</string>
     */
    private static String extractSoapString(String xml) {
        if (xml == null) return "";
        Pattern p = Pattern.compile("<string[^>]*>(.*?)</string>", Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        if (m.find()) {
            return m.group(1).trim();
        }
        return xml.trim();
    }


}
