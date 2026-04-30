package com.naijavehicle.api.controller;

import com.naijavehicle.api.models.User;
import com.naijavehicle.api.models.VehicleReport;
import com.naijavehicle.api.repositoryService.UserRepository;
import com.naijavehicle.api.repositoryService.VehicleReportRepository;
import com.naijavehicle.api.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VerificationControllerTest {

    @Mock
    private VerificationService verificationService;

    @Mock
    private VehicleReportRepository vehicleReportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VerificationController verificationController;

    private User testUser;
    private VehicleReport testReport;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setUserId("test-user-id");

        testReport = new VehicleReport();
        testReport.setReportId("test-report-id");
        testReport.setPlateNumber("ABC-123");
        testReport.setUserId("test-user-id");
    }

    @Test
    void testGetHistory() {
        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        PageImpl<VehicleReport> page = new PageImpl<>(Collections.singletonList(testReport), PageRequest.of(0, 10), 1);
        when(vehicleReportRepository.findByUserId(anyString(), any())).thenReturn(page);

        ResponseEntity<?> response = verificationController.getHistory(0, 10, auth);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testVerifyPlate() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(verificationService.verifyPlate(anyString(), any())).thenReturn(Collections.emptyList());

        ResponseEntity<?> response = verificationController.verifyPlate("ABC-123", req);
        assertEquals(200, response.getStatusCode().value());
    }
}
