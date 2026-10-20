package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.Ride;
import com.example.sikka.Repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;

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

        if (!ride.getPlanItems().isEmpty()
                || !ride.getVisitRecords().isEmpty()
                || !ride.getRidePreferences().isEmpty()) {
            throw new ApiException(
                    "Cannot delete a ride linked to plans, records or preferences"
            );
        }

        rideRepository.delete(ride);
    }

    private void validateRide(Ride ride) {
        if (ride.getMinHeightCm() != null
                && ride.getMaxHeightCm() != null
                && ride.getMinHeightCm().compareTo(ride.getMaxHeightCm()) > 0) {
            throw new ApiException("Minimum height cannot exceed maximum height");
        }

        if (ride.getMinAgeYears() != null
                && ride.getMaxAgeYears() != null
                && ride.getMinAgeYears() > ride.getMaxAgeYears()) {
            throw new ApiException("Minimum age cannot exceed maximum age");
        }

        if (ride.getMinWeightKg() != null
                && ride.getMaxWeightKg() != null
                && ride.getMinWeightKg().compareTo(ride.getMaxWeightKg()) > 0) {
            throw new ApiException("Minimum weight cannot exceed maximum weight");
        }

        if (ride.getWaitMinutes() != null && ride.getWaitObservedAt() == null) {
            throw new ApiException(
                    "Wait observation time is required when waiting time is provided"
            );
        }

        if (ride.getWaitMinutes() == null && ride.getWaitObservedAt() != null) {
            throw new ApiException(
                    "Waiting time is required when wait observation time is provided"
            );
        }

        if ((ride.getMapX() == null) != (ride.getMapY() == null)) {
            throw new ApiException("Both map coordinates must be provided together");
        }
    }

    private void copyRideFields(Ride target, Ride source) {
        target.setName(source.getName());
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
    }
}