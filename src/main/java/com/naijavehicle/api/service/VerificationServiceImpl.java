package com.naijavehicle.api.service;

import com.naijavehicle.api.dto.InsuranceInfoDTO;
import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.dto.VehicleAdditionalInfoDTO;
import com.naijavehicle.api.enums.ChannelEnum;
import com.naijavehicle.api.exceptions.SystemMalFunctionException;
import com.naijavehicle.api.models.VehicleReport;
import com.naijavehicle.api.repositoryService.VehicleReportRepository;
import com.naijavehicle.api.utils.GeneralUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    private final AskNiidInsuranceService askNiidInsuranceService;
    private final AutoRegService autoRegService;
    private final PayvisService payvisService;
    private final DvisService dvisService;
    private final VregService vregService;
    private final VehicleReportRepository vehicleReportRepository;

    @Autowired
    @Qualifier("customThreadPool")
    private final Executor customExecutor;

    @Override
    public List<ScrapingResult<?>> verifyPlate(String plateNumber, HttpServletRequest request) {
        try {

            var askNiidFuture = CompletableFuture.supplyAsync(() -> askNiidInsuranceService.verifyLicensePlate(plateNumber)
                            , customExecutor)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(ex -> AppError.<InsuranceInfoDTO>exceptionFormat(plateNumber, ChannelEnum.VEHICLE_INSURANCE));

            var autoRegFuture = CompletableFuture.supplyAsync(() -> autoRegService.verifyLicensePlate(plateNumber)
                            , customExecutor)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(ex -> AppError.<VehicleAdditionalInfoDTO>exceptionFormat(plateNumber, ChannelEnum.AUTO_REG));


            var payvisFuture = CompletableFuture.supplyAsync(() -> payvisService.verifyLicensePlate(plateNumber), customExecutor)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(ex -> AppError.<Object>exceptionFormat(plateNumber, ChannelEnum.PAY_VIS));

            var dvisFuture = CompletableFuture.supplyAsync(
                            () -> dvisService.verifyLicensePlate(plateNumber), customExecutor)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(ex -> AppError.<Object>exceptionFormat(plateNumber, ChannelEnum.DIVS));


            CompletableFuture.allOf(askNiidFuture, autoRegFuture, payvisFuture, dvisFuture).join();

            List<ScrapingResult<?>> result = Arrays.asList(
                    askNiidFuture.join(), // Use .join() instead of .get() to avoid checked exceptions
                    autoRegFuture.join(),
                    payvisFuture.join(),
                    dvisFuture.join()
            );
           log.info(" the response -> {}", result);
            result.stream().filter(
                            value -> !value.getStatus().toLowerCase().contains("error"))
                    .findFirst().orElseThrow(() -> new BadRequestException("Failed to fetch result"));

            // 4. Audit & Persistence
            String appInstallationId = GeneralUtils.getAppInstallationId(request);

            VehicleReport vehicleReport = VehicleReport.builder()
                    .appInstallationId(appInstallationId)
                    .ipAddress(GeneralUtils.getIpAddress(request))
                    .results(result)
                    .plateNumber(plateNumber)
                    .build();

           // vehicleReportRepository.saveReport(vehicleReport);

            return result;
        } catch (Exception e) {
            log.error("Verification failed for plate {}: {}", plateNumber, ExceptionUtils.getStackTrace(e));
            throw new SystemMalFunctionException("External verification service failure");
        }
    }

    @Override
    public void verifyVin() {
        //            ScrapingResult vregFuture = new ScrapingResult();
        //log.info("the response -> {}", autoRegFutureResult);
//            if(!Objects.isNull(autoRegFutureResult.getAdditionalInfo()) ){
//                vregFuture =  vregService.verifyLicensePlate(plateNumber,autoRegFutureResult.getAdditionalInfo().toString());
//            }
    }
}
