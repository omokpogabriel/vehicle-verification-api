package com.naijavehicle.api.service;

import com.naijavehicle.api.dto.ScrapingResult;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface VerificationService {
    List<ScrapingResult<?>> verifyPlate(String plateNumber, HttpServletRequest request);

    void verifyVin();
}
