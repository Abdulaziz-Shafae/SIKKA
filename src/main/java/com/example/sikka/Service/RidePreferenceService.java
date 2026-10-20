package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.Ride;
import com.example.sikka.Model.RidePreference;
import com.example.sikka.Model.VisitorProfile;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.RidePreferenceRepository;
import com.example.sikka.Repository.RideRepository;
import com.example.sikka.Repository.VisitorProfileRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class RidePreferenceService {

    private final RidePreferenceRepository ridePreferenceRepository;
    private final VisitorProfileRepository visitorProfileRepository;
    private final AccountRepository accountRepository;
    private final RideRepository rideRepository;

    public List<RidePreference> getMyRidePreferences(Integer accountId){
        Account account = accountRepository.findAccountById(accountId);

        if (account == null){
            throw new ApiException("Account not found");
        }

        VisitorProfile visitorProfile = visitorProfileRepository.findVisitorProfileByAccountId(accountId);

        if (visitorProfile == null){
            throw new ApiException("Visitor profile not found");
        }

        return ridePreferenceRepository.findRidePreferencesByVisitorProfileId(visitorProfile.getId());
    }

    public RidePreference getRidePreferenceById(Integer accountId, Integer preferenceId){
        RidePreference preference = ridePreferenceRepository.findRidePreferenceById(preferenceId);

        if (preference == null){
            throw new ApiException("Ride preference not found");
        }

        if (!preference.getVisitorProfile().getAccount().getId().equals(accountId)){
            throw new ApiException("You cannot access another user's preference");
        }

        return preference;
    }

    public void addRidePreference(Integer accountId, Integer rideId, RidePreference ridePreference){
        Account account = accountRepository.findAccountById(accountId);

        if (account == null){
            throw new ApiException("Account not found");
        }

        VisitorProfile visitorProfile = visitorProfileRepository.findVisitorProfileByAccountId(accountId);

        if (visitorProfile == null){
            throw new ApiException("Visitor profile not found");
        }

        Ride ride = rideRepository.findRideById(rideId);

        if (ride == null){
            throw new ApiException("Ride not found");
        }

        if (ridePreferenceRepository.existsByVisitorProfileIdAndRideId(visitorProfile.getId(), rideId)){
            throw new ApiException("A preference already exists for this ride");
        }

        RidePreference newPreference = new RidePreference();
        newPreference.setPreference(ridePreference.getPreference());
        newPreference.setVisitorProfile(visitorProfile);
        newPreference.setRide(ride);

        ridePreferenceRepository.save(newPreference);
    }

    public void updateRidePreference(Integer accountId, Integer preferenceId, RidePreference ridePreference){
        RidePreference oldPreference = ridePreferenceRepository.findRidePreferenceById(preferenceId);

        if (oldPreference == null){
            throw new ApiException("Ride preference not found");
        }

        if (!oldPreference.getVisitorProfile().getAccount().getId().equals(accountId)){
            throw new ApiException("You cannot update another user's preference");
        }

        oldPreference.setPreference(ridePreference.getPreference());

        ridePreferenceRepository.save(oldPreference);
    }

    public void deleteRidePreference(Integer accountId, Integer preferenceId){
        RidePreference preference = ridePreferenceRepository.findRidePreferenceById(preferenceId);

        if (preference == null){
            throw new ApiException("Ride preference not found");
        }

        if (!preference.getVisitorProfile().getAccount().getId().equals(accountId)){
            throw new ApiException("You cannot delete another user's preference");
        }

        ridePreferenceRepository.delete(preference);
    }
}