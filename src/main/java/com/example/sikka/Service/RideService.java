package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.UpdateRideStatusDtoIn;
import com.example.sikka.DtoIn.UpdateRideWaitDtoIn;
import com.example.sikka.DtoOut.RideWaitTimeDtoOut;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.Ride;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.sikka.Model.VisitorProfile;
import com.example.sikka.Repository.VisitorProfileRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final VisitorProfileRepository visitorProfileRepository;
    private final AccountRepository accountRepository;

    @org.springframework.beans.factory.annotation.Value("${sikka.park.wait-freshness-minutes:15}")
    private long waitFreshnessMinutes = 15;

    public List<Ride> getRides() {
        return rideRepository.findAll();
    }

    public Ride getRideById(Integer id) {
        Ride ride = rideRepository.findRideById(id);

        if (ride == null) {
            throw new ApiException("Ride not found");
        }

        return ride;
    }

    public void addRide(Ride ride) {
        validateRide(ride);

        Ride newRide = new Ride();
        copyRideFields(newRide, ride);

        rideRepository.save(newRide);
    }

    @Transactional
    public void updateRide(Integer id, Ride ride) {
        Ride oldRide = rideRepository.findRideById(id);

        if (oldRide == null) {
            throw new ApiException("Ride not found");
        }

        validateRide(ride);
        copyRideFields(oldRide, ride);

        rideRepository.save(oldRide);
    }

    @Transactional
    public void deleteRide(Integer id) {
        Ride ride = rideRepository.findRideById(id);

        if (ride == null) {
            throw new ApiException("Ride not found");
        }

        if (!ride.getPlanItems().isEmpty() || !ride.getVisitRecords().isEmpty() || !ride.getRidePreferences().isEmpty()) {
            throw new ApiException( "Cannot delete a ride linked to plans, records or preferences" );
        }

        rideRepository.delete(ride);
    }

    private void validateRide(Ride ride){
        if (ride == null){
            throw new ApiException("Ride details are required");
        }

        if (ride.getThrillLevel() != null
                && !ride.getThrillLevel().matches("^(LOW|MILD|EXTREME)$")){
            throw new ApiException("Thrill level must be LOW, MILD or EXTREME");
        }

        String heightRuleStatus = ride.getHeightRuleStatus();

        if (heightRuleStatus == null){
            heightRuleStatus = "UNKNOWN";
            ride.setHeightRuleStatus(heightRuleStatus);
        }

        if (!heightRuleStatus.matches("^(UNKNOWN|NONE|SPECIFIED)$")){
            throw new ApiException("Invalid height rule status");
        }

        if ("NONE".equals(heightRuleStatus)
                && (ride.getMinHeightCm() != null || ride.getMaxHeightCm() != null)){
            throw new ApiException("Height limits must be empty when height rule status is NONE");
        }

        if ("SPECIFIED".equals(heightRuleStatus)
                && ride.getMinHeightCm() == null && ride.getMaxHeightCm() == null){
            throw new ApiException("At least one height limit is required when height rule status is SPECIFIED");
        }

        String weightRuleStatus = ride.getWeightRuleStatus();

        if (weightRuleStatus == null){
            weightRuleStatus = "UNKNOWN";
            ride.setWeightRuleStatus(weightRuleStatus);
        }

        if (!weightRuleStatus.matches("^(UNKNOWN|NONE|SPECIFIED)$")){
            throw new ApiException("Invalid weight rule status");
        }

        if ("NONE".equals(weightRuleStatus)
                && (ride.getMinWeightKg() != null || ride.getMaxWeightKg() != null)){
            throw new ApiException("Weight limits must be empty when weight rule status is NONE");
        }

        if ("SPECIFIED".equals(weightRuleStatus)
                && ride.getMinWeightKg() == null && ride.getMaxWeightKg() == null){
            throw new ApiException("At least one weight limit is required when weight rule status is SPECIFIED");
        }

        String ageRuleStatus = ride.getAgeRuleStatus();

        if (ageRuleStatus == null){
            ageRuleStatus = "UNKNOWN";
            ride.setAgeRuleStatus(ageRuleStatus);
        }

        if (!ageRuleStatus.matches("^(UNKNOWN|NONE|SPECIFIED)$")){
            throw new ApiException("Invalid age rule status");
        }

        if ("NONE".equals(ageRuleStatus)
                && (ride.getMinAgeYears() != null || ride.getMaxAgeYears() != null)){
            throw new ApiException("Age limits must be empty when age rule status is NONE");
        }

        if ("SPECIFIED".equals(ageRuleStatus)
                && ride.getMinAgeYears() == null && ride.getMaxAgeYears() == null){
            throw new ApiException("At least one age limit is required when age rule status is SPECIFIED");
        }

        if (!"UNKNOWN".equals(heightRuleStatus)
                || !"UNKNOWN".equals(weightRuleStatus)
                || !"UNKNOWN".equals(ageRuleStatus)){

            if (ride.getSourceUrl() == null || ride.getSourceUrl().isBlank()
                    || ride.getRequirementsVerifiedAt() == null){
                throw new ApiException("Source URL and verification time are required for documented rules");
            }
        }

        if (ride.getCompanionRequiredBelowHeightCm() != null
                && !Boolean.TRUE.equals(ride.getAdultCompanionRequired())){
            throw new ApiException("Adult companion must be required when a companion height threshold is provided");
        }

        if (ride.getCompanionRequiredBelowHeightCm() != null
                && ride.getMinHeightCm() != null
                && ride.getCompanionRequiredBelowHeightCm().compareTo(ride.getMinHeightCm()) < 0){
            throw new ApiException("Companion height threshold cannot be below minimum ride height");
        }

        if (ride.getMinHeightCm() != null
                && ride.getMaxHeightCm() != null
                && ride.getMinHeightCm().compareTo(ride.getMaxHeightCm()) > 0){
            throw new ApiException("Minimum height cannot exceed maximum height");
        }

        if (ride.getMinAgeYears() != null
                && ride.getMaxAgeYears() != null
                && ride.getMinAgeYears() > ride.getMaxAgeYears()){
            throw new ApiException("Minimum age cannot exceed maximum age");
        }

        if (ride.getMinWeightKg() != null
                && ride.getMaxWeightKg() != null
                && ride.getMinWeightKg().compareTo(ride.getMaxWeightKg()) > 0){
            throw new ApiException("Minimum weight cannot exceed maximum weight");
        }

        if (ride.getWaitMinutes() != null
                && (ride.getWaitSource() == null || ride.getWaitSource().isBlank())){
            throw new ApiException("Wait source is required when waiting time is provided");
        }

        if (ride.getWaitMinutes() != null && ride.getWaitObservedAt() == null){
            throw new ApiException("Wait observation time is required when waiting time is provided");
        }

        if (ride.getWaitMinutes() == null && ride.getWaitObservedAt() != null){
            throw new ApiException("Waiting time is required when wait observation time is provided");
        }

        if ((ride.getMapX() == null) != (ride.getMapY() == null)){
            throw new ApiException("Both map coordinates must be provided together");
        }
    }

    private void copyRideFields(Ride target, Ride source) {
        target.setName(source.getName());
        target.setLand(source.getLand());
        target.setRideFeatures(source.getRideFeatures());
        target.setAdultCompanionRequired(source.getAdultCompanionRequired());
        target.setCompanionRequiredBelowHeightCm(source.getCompanionRequiredBelowHeightCm());
        target.setRecommendedMinAgeYears(source.getRecommendedMinAgeYears());
        target.setRecommendedUnaccompaniedAgeYears(source.getRecommendedUnaccompaniedAgeYears());
        target.setRequirementsVerifiedAt(source.getRequirementsVerifiedAt());
        target.setCategory(source.getCategory());
        target.setThrillLevel(source.getThrillLevel());
        target.setStatus(source.getStatus());
        target.setMinHeightCm(source.getMinHeightCm());
        target.setMaxHeightCm(source.getMaxHeightCm());
        target.setMinAgeYears(source.getMinAgeYears());
        target.setMaxAgeYears(source.getMaxAgeYears());
        target.setMinWeightKg(source.getMinWeightKg());
        target.setMaxWeightKg(source.getMaxWeightKg());
        target.setWarnings(source.getWarnings());
        target.setWaitMinutes(source.getWaitMinutes());
        target.setWaitObservedAt(source.getWaitObservedAt());
        target.setSourceUrl(source.getSourceUrl());
        target.setWaitSource(source.getWaitSource());
        target.setMapX(source.getMapX());
        target.setMapY(source.getMapY());
        target.setHeightRuleStatus(source.getHeightRuleStatus());
        target.setWeightRuleStatus(source.getWeightRuleStatus());
        target.setAgeRuleStatus(source.getAgeRuleStatus());
    }

    public List<Ride> getOpenRides(){
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<Ride> rides = new ArrayList<>();
        for (Ride ride : rideRepository.findRidesByStatus("OPEN")) {
            if (ride.getWaitMinutes() != null && ride.getWaitObservedAt() != null && !ride.getWaitObservedAt().isAfter(now) && !ride.getWaitObservedAt().isBefore(now.minusMinutes(waitFreshnessMinutes)) && ride.getWaitSource() != null && !ride.getWaitSource().isBlank()) {
                rides.add(ride);
            }
        }
        return rides;
    }

    public boolean validateRideIds(List<Integer> rideIds){
        if (rideIds == null){
            return false;
        }

        for (Integer rideId : rideIds){
            if (rideId == null || rideId <= 0){
                return false;
            }
        }

        List<Integer> uniqueRideIds = new ArrayList<>();

        for (Integer rideId : rideIds){
            if (!uniqueRideIds.contains(rideId)){
                uniqueRideIds.add(rideId);
            }
        }

        return rideRepository.findAllById(uniqueRideIds).size() == uniqueRideIds.size();
    }

    // TODO: فحص الأهلية غير مكتمل؛ حاليًا يتحقق من قيود الطول والوزن والعمر المعروفة فقط.
    // نحتاج التمييز بين القيد غير المعروف وعدم وجود قيد، واستكمال شروط المرافق والسلامة.
    // نحتاج توحيد صيغة healthData وقواعد مقارنتها، والتحقق من الموافقة قبل استخدامها.
    // العمر يُحسب بتاريخ اليوم؛ نحتاج احتسابه بتاريخ الزيارة عند استخدام الدالة للتخطيط.
    // حاليًا: مخالفة قيد معروف ترجع NOT_ELIGIBLE، وما عدا ذلك UNKNOWN.
    // لا نرجع ELIGIBLE حتى تكتمل الفحوصات المطلوبة، ولا نعامل UNKNOWN كإثبات أهلية.
    // حالة تشغيل اللعبة تُفحص بشكل منفصل باستخدام getOpenRides().
    public String checkEligibility(Integer rideId, Integer profileId){
        Ride ride = rideRepository.findRideById(rideId);

        if (ride == null){
            throw new ApiException("Ride not found");
        }

        VisitorProfile profile = visitorProfileRepository.findVisitorProfileById(profileId);

        if (profile == null){
            throw new ApiException("Visitor profile not found");
        }

        if (profile.getHeightCm() != null && profile.getHeightCm().signum() > 0){
            if (ride.getMinHeightCm() != null
                    && profile.getHeightCm().compareTo(ride.getMinHeightCm()) < 0){
                return "NOT_ELIGIBLE";
            }

            if (ride.getMaxHeightCm() != null
                    && profile.getHeightCm().compareTo(ride.getMaxHeightCm()) > 0){
                return "NOT_ELIGIBLE";
            }
        }

        if (profile.getWeightKg() != null && profile.getWeightKg().signum() > 0){
            if (ride.getMinWeightKg() != null
                    && profile.getWeightKg().compareTo(ride.getMinWeightKg()) < 0){
                return "NOT_ELIGIBLE";
            }

            if (ride.getMaxWeightKg() != null
                    && profile.getWeightKg().compareTo(ride.getMaxWeightKg()) > 0){
                return "NOT_ELIGIBLE";
            }
        }

        LocalDate today = LocalDate.now();

        if (profile.getBirthDate() != null && !profile.getBirthDate().isAfter(today)){
            int age = Period.between(profile.getBirthDate(), today).getYears();

            if (ride.getMinAgeYears() != null && age < ride.getMinAgeYears()){
                return "NOT_ELIGIBLE";
            }

            if (ride.getMaxAgeYears() != null && age > ride.getMaxAgeYears()){
                return "NOT_ELIGIBLE";
            }
        }

        return "UNKNOWN";
    }

    public List<RideWaitTimeDtoOut> getRideWaitTimes(){
        List<Ride> rides = rideRepository.findAll();
        List<RideWaitTimeDtoOut> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Ride ride : rides){
            String freshness = "UNKNOWN";

            if (ride.getWaitMinutes() != null
                    && ride.getWaitMinutes() >= 0
                    && ride.getWaitObservedAt() != null
                    && !ride.getWaitObservedAt().isAfter(now)
                    && ride.getWaitSource() != null
                    && !ride.getWaitSource().isBlank()){

                if (ride.getWaitObservedAt().isBefore(now.minusMinutes(waitFreshnessMinutes))){
                    freshness = "STALE";
                } else {
                    freshness = "RECENT";
                }
            }

            result.add(new RideWaitTimeDtoOut(
                    ride.getId(),
                    ride.getName(),
                    ride.getStatus(),
                    ride.getWaitMinutes(),
                    ride.getWaitObservedAt(),
                    ride.getWaitSource(),
                    freshness
            ));
        }

        return result;
    }

    public List<Ride> filterRides(String category, String thrill, String status){
        if (category != null && !category.matches("^(THRILL|FAMILY|KIDS|WATER|ARCADE)$")){
            throw new ApiException("Category must be THRILL, FAMILY, KIDS, WATER or ARCADE");
        }

        if (thrill != null && !thrill.matches("^(LOW|MILD|EXTREME)$")){
            throw new ApiException("Thrill level must be LOW, MILD or EXTREME");
        }

        if (status != null && !status.matches("^(OPEN|CLOSED|UNAVAILABLE)$")){
            throw new ApiException("Status must be OPEN, CLOSED or UNAVAILABLE");
        }

        List<Ride> rides = rideRepository.filterRides(category, thrill, status);

        if (rides.isEmpty()){
            throw new ApiException("No rides found matching these filters");
        }

        return rides;
    }

    @Transactional
    public void updateRideWait(Integer accountId, Integer rideId, UpdateRideWaitDtoIn dto){
        Account account = accountRepository.findAccountById(accountId);

        if (account == null){
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())){
            throw new ApiException("Account is not active");
        }

        if (!"ADMIN".equals(account.getRole())){
            throw new ApiException("Only admins can update ride waiting times");
        }

        Ride ride = rideRepository.findRideById(rideId);

        if (ride == null){
            throw new ApiException("Ride not found");
        }

        if (ride.getWaitObservedAt() != null
                && dto.getWaitObservedAt().isBefore(ride.getWaitObservedAt())){
            throw new ApiException("Cannot replace a newer waiting time observation with an older one");
        }

        ride.setWaitMinutes(dto.getWaitMinutes());
        ride.setWaitSource(dto.getWaitSource());
        ride.setWaitObservedAt(dto.getWaitObservedAt());

        rideRepository.save(ride);

        // TODO: بعد تنفيذ onRideChanged وتحديد مقدار تغير الانتظار المؤثر،
        // نربط التحديث بفحص إعادة التخطيط عند عبدالعزيز.
    }

    @Transactional
    public void updateRideStatus(Integer accountId, Integer rideId, UpdateRideStatusDtoIn dto){
        Account account = accountRepository.findAccountById(accountId);

        if (account == null){
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())){
            throw new ApiException("Account is not active");
        }

        if (!"ADMIN".equals(account.getRole())){
            throw new ApiException("Only admins can update ride status");
        }

        Ride ride = rideRepository.findRideById(rideId);

        if (ride == null){
            throw new ApiException("Ride not found");
        }

        if (dto.getStatus().equals(ride.getStatus())){
            throw new ApiException("Ride already has this status");
        }

        ride.setStatus(dto.getStatus());

        rideRepository.save(ride);

        // TODO: ربط التغيير المؤثر بـ VisitPlanService.onRideChanged(rideId)
        // بعد تنفيذها، لفحص الخطط المتأثرة واقتراح بديل دون تغيير الخطة المعتمدة مباشرة.
    }
}
