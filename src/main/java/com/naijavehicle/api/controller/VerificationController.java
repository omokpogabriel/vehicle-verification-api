package com.naijavehicle.api.controller;

import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.enums.AppConstant;
import com.naijavehicle.api.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import java.util.concurrent.TimeUnit;
import org.springframework.cache.annotation.Cacheable;

@RestController
@RequestMapping("/api/v1/verify")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final AskNiidService askNiidService;
    private final AutoRegService autoRegService;
    private final PayvisService payvisService;
    private final DvisService dvisService;
    private final VregService vregService;

    @GetMapping("/plate/{plateNumber}")
    @Cacheable(value = "verifications", key = "#plateNumber")
    public ResponseEntity<List<ScrapingResult>> verifyPlate(@PathVariable String plateNumber) {
        try {
            CompletableFuture<ScrapingResult> askNiidFuture = CompletableFuture.supplyAsync(() -> askNiidService.verifyLicensePlate(plateNumber))
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(ex -> AppError.exceptionFormat(plateNumber, AppConstant.VEHICLE_LICENSE));

            CompletableFuture<ScrapingResult> autoRegFuture = CompletableFuture.supplyAsync(() -> autoRegService.verifyLicensePlate(plateNumber))
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(ex -> AppError.exceptionFormat(plateNumber, AppConstant.AUTOREG));

            CompletableFuture<ScrapingResult> payvisFuture = CompletableFuture.supplyAsync(() -> payvisService.verifyLicensePlate(plateNumber))
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(ex ->  AppError.exceptionFormat(plateNumber, AppConstant.PAYVIS));

            CompletableFuture<ScrapingResult> dvisFuture = CompletableFuture.supplyAsync(() -> dvisService.verifyLicensePlate(plateNumber))
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(ex -> AppError.exceptionFormat(plateNumber, AppConstant.DIVS));



            CompletableFuture.allOf(askNiidFuture, autoRegFuture, payvisFuture, dvisFuture).join();
            ScrapingResult autoRegFutureResult = autoRegFuture.get();

            ScrapingResult vregFuture = new ScrapingResult();
            log.info("the response -> {}", autoRegFutureResult);
            if(!Objects.isNull(autoRegFutureResult.getAdditionalInfo()) ){
                vregFuture =  vregService.verifyLicensePlate(plateNumber,autoRegFutureResult.getAdditionalInfo().toString());
            }
            List<ScrapingResult> results = Arrays.asList(
                    askNiidFuture.get(),
                    autoRegFutureResult,
                    payvisFuture.get(),
                    dvisFuture.get(),
                    vregFuture
            );



            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }


}
