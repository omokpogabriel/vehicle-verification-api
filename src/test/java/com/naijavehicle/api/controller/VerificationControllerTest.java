package com.naijavehicle.api.controller;

import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.service.AskNiidService;
import com.naijavehicle.api.service.AutoRegService;
import com.naijavehicle.api.service.DvisService;
import com.naijavehicle.api.service.PayvisService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.web.client.RestClient;

public class VerificationControllerTest {

    @Test
    public void testVerifyPlate_Success() {
        AskNiidService askNiidService = new AskNiidService(RestClient.builder()) {
            @Override
            public ScrapingResult verifyLicensePlate(String plateNumber) {
                return new ScrapingResult(plateNumber, "Make1", "Status1", "Info1");
            }
        };
        AutoRegService autoRegService = new AutoRegService(RestClient.builder()) {
            @Override
            public ScrapingResult verifyLicensePlate(String plateNumber) {
                return new ScrapingResult(plateNumber, "Make2", "Status2", "Info2");
            }
        };
        PayvisService payvisService = new PayvisService(RestClient.builder()) {
            @Override
            public ScrapingResult verifyLicensePlate(String plateNumber) {
                return new ScrapingResult(plateNumber, "Make3", "Status3", "Info3");
            }
        };
        DvisService dvisService = new DvisService(RestClient.builder()) {
            @Override
            public ScrapingResult verifyLicensePlate(String plateNumber) {
                return new ScrapingResult(plateNumber, "Make4", "Status4", "Info4");
            }
        };

        VerificationController controller = new VerificationController(
            askNiidService, autoRegService, payvisService, dvisService
        );

        String plate = "ABC123XY";

        ResponseEntity<List<ScrapingResult>> response = controller.verifyPlate(plate);
        
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().size());
        assertEquals("Info1", response.getBody().get(0).getAdditionalInfo());
        assertEquals("Info4", response.getBody().get(3).getAdditionalInfo());
    }

    @Test
    public void testVerifyPlate_PartialFailure() {
        AskNiidService askNiidService = new AskNiidService(RestClient.builder()) {
            @Override
            public ScrapingResult verifyLicensePlate(String plateNumber) {
                throw new RuntimeException("API Down");
            }
        };
        AutoRegService autoRegService = new AutoRegService(RestClient.builder()) {
            @Override
            public ScrapingResult verifyLicensePlate(String plateNumber) {
                return new ScrapingResult(plateNumber, "Make2", "Status2", "Info2");
            }
        };
        PayvisService payvisService = new PayvisService(RestClient.builder()) {
            @Override
            public ScrapingResult verifyLicensePlate(String plateNumber) {
                return new ScrapingResult(plateNumber, "Make3", "Status3", "Info3");
            }
        };
        DvisService dvisService = new DvisService(RestClient.builder()) {
            @Override
            public ScrapingResult verifyLicensePlate(String plateNumber) {
                return new ScrapingResult(plateNumber, "Make4", "Status4", "Info4");
            }
        };

        VerificationController controller = new VerificationController(
            askNiidService, autoRegService, payvisService, dvisService
        );

        String plate = "XYZ987AB";

        ResponseEntity<List<ScrapingResult>> response = controller.verifyPlate(plate);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().size());
        assertEquals("Error: Timeout/Unavailable", response.getBody().get(0).getStatus());
    }
}
