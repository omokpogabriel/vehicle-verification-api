package com.naijavehicle.api.service;

import com.naijavehicle.api.dto.InsuranceInfoDTO;
import com.naijavehicle.api.dto.ScrapingResult;
import com.naijavehicle.api.dto.VehicleAdditionalInfoDTO;
import com.naijavehicle.api.enums.ChannelEnum;
import com.naijavehicle.api.exceptions.SystemMalFunctionException;
import com.naijavehicle.api.models.User;
import com.naijavehicle.api.models.VehicleReport;
import com.naijavehicle.api.repositoryService.UserRepository;
import com.naijavehicle.api.repositoryService.VehicleReportRepository;
import com.naijavehicle.api.utils.GeneralUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final UserRepository userRepository;

    @Autowired
    @Qualifier("customThreadPool")
    private Executor customExecutor;

    private VehicleReport checkExists(String plateNumber){
        return vehicleReportRepository.findByPlateNumber(plateNumber);
    }

    private  Map<ChannelEnum,ScrapingResult<?>> callDirect(String plateNumber, HttpServletRequest request)
            throws BadRequestException {
        var askNiidFuture = CompletableFuture
                .supplyAsync(() -> askNiidInsuranceService.verifyLicensePlate(plateNumber),
                        customExecutor)
                .completeOnTimeout( null,10, TimeUnit.SECONDS);

        var autoRegFuture = CompletableFuture
                .supplyAsync(() -> autoRegService.verifyLicensePlate(plateNumber),
                        customExecutor)
                .orTimeout(15, TimeUnit.SECONDS)
                .exceptionally(ex -> AppError.<VehicleAdditionalInfoDTO>exceptionFormat(
                        plateNumber, ChannelEnum.AUTO_REG));

        var payvisFuture = CompletableFuture
                .supplyAsync(() -> payvisService.verifyLicensePlate(plateNumber),
                        customExecutor)
                .orTimeout(15, TimeUnit.SECONDS)
                .exceptionally(ex -> AppError.<com.naijavehicle.api.dto.PayVisResponseDto>exceptionFormat(
                        plateNumber, ChannelEnum.PAY_VIS));

        var dvisFuture = CompletableFuture.supplyAsync(
                        () -> dvisService.verifyLicensePlate(plateNumber), customExecutor)
                .orTimeout(15, TimeUnit.SECONDS)
                .exceptionally(ex -> AppError.<String>exceptionFormat(plateNumber,
                        ChannelEnum.DIVS));

        CompletableFuture.allOf(askNiidFuture,autoRegFuture, payvisFuture, dvisFuture).join();

        var askResult = askNiidInsuranceService.decodeAskNiidResult(askNiidFuture.join(), plateNumber);

        Map<ChannelEnum, ScrapingResult<?>> result = new HashMap<>();
        result.put(ChannelEnum.VEHICLE_INSURANCE, askResult);
        result.put(ChannelEnum.AUTO_REG, autoRegFuture.join());
        result.put(ChannelEnum.PAY_VIS, payvisFuture.join());
        result.put(ChannelEnum.DIVS, dvisFuture.join());
        log.info(" the g response -> {}", result);
        result.values().stream().filter(
                        value -> !value.getStatus().toLowerCase().contains("error"))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Failed to fetch result"));

        // 4. Audit & Persistence
        String appInstallationId = GeneralUtils.getAppInstallationId(request);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                userId = user.getUserId();
            }
        }

        VehicleReport vehicleReport = VehicleReport.builder()
                .appInstallationId(appInstallationId)
                .ipAddress(GeneralUtils.getIpAddress(request))
                .results(result)
                .plateNumber(plateNumber)
                .userId(userId)
                .build();

         vehicleReportRepository.saveReport(vehicleReport);
        return result;
    }

    @Override
    public List<ScrapingResult<?>> verifyPlate(String plateNumber, HttpServletRequest request) {
        try {

            VehicleReport existingReport = checkExists(plateNumber);

            log.info("again -> {}",existingReport);
            if (existingReport != null) {
                log.info("Cache hit for plate {}: {}", plateNumber, existingReport);


               var insurance =  existingReport.getResults().get(ChannelEnum.VEHICLE_INSURANCE);

               if(insurance.getCode().equalsIgnoreCase("ETO11")){
                   var responseXml = askNiidInsuranceService.verifyLicensePlate(plateNumber);
                   var parsed = askNiidInsuranceService.decodeAskNiidResult( responseXml,plateNumber);

                existingReport.getResults().put(ChannelEnum.VEHICLE_INSURANCE, parsed);
                   log.info("the parsed -> {}", existingReport);
                   vehicleReportRepository.updateReport(existingReport);
               }

                return existingReport.getResults().values().stream().toList();
            }


            return callDirect(plateNumber, request).values().stream().toList();
        } catch (Exception e) {
            log.error("Verification failed for plate {}: {}", plateNumber, ExceptionUtils.getStackTrace(e));
            throw new SystemMalFunctionException("External verification service failure");
        }
    }

    @Override
    public void verifyVin() {
        // ScrapingResult vregFuture = new ScrapingResult();
        // log.info("the response -> {}", autoRegFutureResult);
        // if(!Objects.isNull(autoRegFutureResult.getAdditionalInfo()) ){
        // vregFuture =
        // vregService.verifyLicensePlate(plateNumber,autoRegFutureResult.getAdditionalInfo().toString());
        // }
    }
}
