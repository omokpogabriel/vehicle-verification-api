package com.naijavehicle.api.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.naijavehicle.api.dto.InsuranceInfoDTO;
import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.enums.ChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AskNiidInsuranceService {

    private static final String TARGET_URL =
            "https://niid.org/NIA_API/Service.asmx/Vehicle_PolicyVerification";

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

    public ScrapingResult verifyLicensePlate(String plateNumber) {
        try {
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                throw new IllegalStateException("Missing NIID credentials in application.properties (askniid_insurance_user/askniid_insurance_pass).");
            }

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("SearchString", plateNumber); // {{plate_or_policy}}
            formData.add("SearchType", searchType); // {{search_type}}
            formData.add("Username", username); // {{askniid_insurance_user}}
            formData.add("Password", password); // {{askniid_insurance_pass}}

            String responseXml = restClient.post()
                    .uri(TARGET_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            String soapResult = extractSoapString(responseXml);
            var result= parseInsuranceXml(soapResult);//(plateNumber, soapResult);

            ScrapingResult scrapResult = new ScrapingResult();
            scrapResult.setPlateNumber(plateNumber);
            scrapResult.setCarMake(result.getMake());
            scrapResult.setAdditionalInfo(result);
            scrapResult.setStatus(result.getStatus());
            scrapResult.setType(ChannelEnum.VEHICLE_INSURANCE.name());
            return scrapResult;
        } catch (Exception e) {
            return new ScrapingResult<>(plateNumber, "Not found", "Error: " + e.getMessage(), "","Vehicle License");
        }
    }

    public InsuranceInfoDTO parseInsuranceXml(String xmlResult) throws Exception {
        XmlMapper xmlMapper = new XmlMapper();

        String cleanXml = xmlResult.replace("result -> ", "").trim();
        String unescapedXml = HtmlUtils.htmlUnescape(cleanXml);
        return xmlMapper.readValue(unescapedXml, InsuranceInfoDTO.class);
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

    private static ScrapingResult mapSoapInsuranceInfo(String plateNumber, String soapResult) {
        var scrapResult =  new ScrapingResult(plateNumber, "Unknown", "unable to get record",
                "", ChannelEnum.VEHICLE_INSURANCE.name);

        if (soapResult == null || soapResult.isBlank()) {
            return scrapResult;        }

        String maybeXml = unescapeXml(soapResult);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Extra safety: avoid XXE when parsing remote payloads.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(maybeXml)));
            doc.getDocumentElement().normalize();

            String statusRaw = getTagText(doc, "Status");
            int status = parseIntOrDefault(statusRaw, 0);

            String carMake = getTagText(doc, "CarMake");
            String policyNo = getTagText(doc, "PolicyNo");
            String model = getTagText(doc, "Model");
            String registrationNo = getTagText(doc, "RegistrationNo");
            String newRegistrationNo = getTagText(doc, "NewRegistrationNo");
            String issueDate = getTagText(doc, "IssueDate");
            String expirationDate = getTagText(doc, "ExpirationDate");
            String vehicleType = getTagText(doc, "VehicleType");

            StringBuilder info = new StringBuilder();
            if (!policyNo.isBlank()) info.append("PolicyNo: ").append(policyNo).append(" | ");
            if (!model.isBlank()) info.append("Model: ").append(model).append(" | ");
            if (!carMake.isBlank()) info.append("CarMake: ").append(carMake).append(" | ");
            if (!vehicleType.isBlank()) info.append("VehicleType: ").append(vehicleType).append(" | ");

            // Prefer the new reg number if present, otherwise fall back to registration no.
            String reg = !newRegistrationNo.isBlank() ? newRegistrationNo : registrationNo;
            if (!reg.isBlank()) info.append("RegistrationNo: ").append(reg).append(" | ");

            if (!issueDate.isBlank()) info.append("IssueDate: ").append(issueDate).append(" | ");
            if (!expirationDate.isBlank()) info.append("ExpirationDate: ").append(expirationDate);

            String insuranceStatus = status == 1 ? "Valid" : "Invalid";
            String additionalInfo = info.toString().trim();
            if (additionalInfo.endsWith("|")) additionalInfo = additionalInfo.substring(0, additionalInfo.length() - 1).trim();
            if (additionalInfo.isBlank()) additionalInfo = "Source: niid.org";

            scrapResult.setPlateNumber(plateNumber);
            scrapResult.setAdditionalInfo(carMake);
            scrapResult.setAdditionalInfo(additionalInfo);
            scrapResult.setStatus(insuranceStatus);
            return scrapResult;

        } catch (Exception parseEx) {
            scrapResult.setPlateNumber(plateNumber);
            scrapResult.setAdditionalInfo(soapResult);
           return scrapResult;
                   }
    }

    private static String unescapeXml(String value) {
        // The SOAP <string> sometimes contains escaped XML like &lt;InsuranceInfo&gt;...&lt;/InsuranceInfo&gt;
        return value
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    private static String getTagText(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes == null || nodes.getLength() == 0) return "";
        String text = nodes.item(0).getTextContent();
        return text == null ? "" : text.trim();
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        try {
            if (value == null) return defaultValue;
            String v = value.trim();
            if (v.isEmpty()) return defaultValue;
            return Integer.parseInt(v);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
