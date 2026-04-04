package com.naijavehicle.api.service;

import com.naijavehicle.api.dto.ScrapingResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DvisServiceTest {

    @Disabled("External HTTP call test; disabled to keep unit tests deterministic.")
    @Test
    public void testVerifyLicensePlate() {
        DvisService service = new DvisService(
                org.mockito.Mockito.mock(org.springframework.web.client.RestClient.Builder.class)
        );
        try {
            ScrapingResult result = service.verifyLicensePlate("ABC123XY");
            assertNotNull(result);
            assertEquals("ABC123XY", result.getPlateNumber());
            assertEquals("DVIS", result.getCarMake());
            assertNotNull(result.getStatus());
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("DVIS"));
        }
    }
}
