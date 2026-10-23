package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.FamilyMemberDtoIn;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.FamilyMember;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.FamilyMemberRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class FamilyMemberService {

    private final FamilyMemberRepository familyMemberRepository;
    private final AccountRepository accountRepository;

    public List<FamilyMember> getMyFamilyMembers(Integer accountId){
        Account account = accountRepository.findAccountById(accountId);

        if (account == null){
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())){
            throw new ApiException("Account is not active");
        }

        return familyMemberRepository.findFamilyMembersByOwnerAccountId(accountId);
    }

    public FamilyMember getFamilyMemberById(Integer accountId, Integer familyMemberId){
        Account account = accountRepository.findAccountById(accountId);

        if (account == null){
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())){
            throw new ApiException("Account is not active");
        }

        FamilyMember familyMember = familyMemberRepository.findFamilyMemberById(familyMemberId);

        if (familyMember == null){
            throw new ApiException("Family member not found");
        }

        if (!familyMember.getOwnerAccount().getId().equals(accountId)){
            throw new ApiException("You cannot access another user's family member");
        }

        return familyMember;
    }

    public void addFamilyMember(Integer accountId, FamilyMemberDtoIn dto){
        Account account = accountRepository.findAccountById(accountId);

        if (account == null){
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())){
            throw new ApiException("Account is not active");
        }

        FamilyMember familyMember = new FamilyMember();
        familyMember.setName(dto.getName().trim());
        familyMember.setBirthDate(dto.getBirthDate());
        familyMember.setHeightCm(dto.getHeightCm());
        familyMember.setWeightKg(dto.getWeightKg());
        familyMember.setGender(dto.getGender());
        familyMember.setOwnerAccount(account);

        familyMemberRepository.save(familyMember);
    }

    public void updateFamilyMember(Integer accountId, Integer familyMemberId, FamilyMemberDtoIn dto){
        Account account = accountRepository.findAccountById(accountId);

        if (account == null){
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())){
            throw new ApiException("Account is not active");
        }

        FamilyMember familyMember = familyMemberRepository.findFamilyMemberById(familyMemberId);

        if (familyMember == null){
            throw new ApiException("Family member not found");
        }

        if (!familyMember.getOwnerAccount().getId().equals(accountId)){
            throw new ApiException("You cannot update another user's family member");
        }

        familyMember.setName(dto.getName().trim());
        familyMember.setBirthDate(dto.getBirthDate());
        familyMember.setHeightCm(dto.getHeightCm());
        familyMember.setWeightKg(dto.getWeightKg());
        familyMember.setGender(dto.getGender());

        familyMemberRepository.save(familyMember);
    }
}