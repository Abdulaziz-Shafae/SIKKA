package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.VisitorProfileDtoIn;
import com.example.sikka.DtoOut.VisitorProfileDtoOut;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.VisitorProfile;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.VisitorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitorProfileService {

    private final VisitorProfileRepository visitorProfileRepository;
    private final AccountRepository accountRepository;

    public List<VisitorProfileDtoOut> get() {
        List<VisitorProfileDtoOut> list = new ArrayList<>();
        for (VisitorProfile p : visitorProfileRepository.findAll())
            list.add(toDtoOut(p));
        return list;
    }

    public void add(VisitorProfileDtoIn dto) {
        Account account = accountRepository.findAccountById(dto.getAccountId());
        if (account == null)
            throw new ApiException("account not found");
        if (account.getVisitorProfile() != null)
            throw new ApiException("account already has a profile");

        VisitorProfile profile = new VisitorProfile();
        fill(profile, dto);
        profile.setAccount(account);
        visitorProfileRepository.save(profile);
    }

    public void update(Integer id, VisitorProfileDtoIn dto) {
        VisitorProfile profile = visitorProfileRepository.findVisitorProfileById(id);
        if (profile == null)
            throw new ApiException("profile not found");

        fill(profile, dto);
        visitorProfileRepository.save(profile);
    }

    public void delete(Integer id) {
        VisitorProfile profile = visitorProfileRepository.findVisitorProfileById(id);
        if (profile == null)
            throw new ApiException("profile not found");

        profile.getAccount().setVisitorProfile(null);
        visitorProfileRepository.delete(profile);
    }

    private void fill(VisitorProfile p, VisitorProfileDtoIn dto) {
        p.setName(dto.getName());
        p.setBirthDate(dto.getBirthDate());
        p.setHeightCm(dto.getHeightCm());
        p.setWeightKg(dto.getWeightKg());
        p.setThrillPreference(dto.getThrillPreference());
        p.setPreferredRideTypes(dto.getPreferredRideTypes());
        p.setDislikedRideFeatures(dto.getDislikedRideFeatures());
        p.setFoodPreferences(dto.getFoodPreferences());
        p.setHealthData(dto.getHealthData());
    }

    private VisitorProfileDtoOut toDtoOut(VisitorProfile p) {
        return new VisitorProfileDtoOut(p.getId(), p.getAccount().getId(), p.getName(),
                p.getBirthDate(), p.getHeightCm(), p.getWeightKg(), p.getThrillPreference(),
                p.getPreferredRideTypes(), p.getDislikedRideFeatures(), p.getFoodPreferences(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}