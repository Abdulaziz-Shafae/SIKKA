package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.AccountDtoIn;
import com.example.sikka.DtoOut.AccountDtoOut;
import com.example.sikka.Model.Account;
import com.example.sikka.Repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public List<AccountDtoOut> get() {
        List<AccountDtoOut> list = new ArrayList<>();
        for (Account a : accountRepository.findAll())
            list.add(toDtoOut(a));
        return list;
    }

    public void add(AccountDtoIn dto) {
        if (accountRepository.findAccountByEmail(dto.getEmail()) != null)
            throw new ApiException("email already exists");

        Account account = new Account();
        account.setName(dto.getName());
        account.setEmail(dto.getEmail());
        account.setPasswordHash(dto.getPassword());
        account.setRole("USER");
        account.setStatus("ACTIVE");
        account.setEmailVerified(false);
        accountRepository.save(account);
    }

    public void update(Integer id, AccountDtoIn dto) {
        Account account = accountRepository.findAccountById(id);
        if (account == null)
            throw new ApiException("account not found");

        Account sameEmail = accountRepository.findAccountByEmail(dto.getEmail());
        if (sameEmail != null && !sameEmail.getId().equals(id))
            throw new ApiException("email already exists");

        account.setName(dto.getName());
        account.setEmail(dto.getEmail());
        account.setPasswordHash(dto.getPassword());
        accountRepository.save(account);
    }

    public void delete(Integer id) {
        Account account = accountRepository.findAccountById(id);
        if (account == null)
            throw new ApiException("account not found");

        accountRepository.delete(account);
    }

    private AccountDtoOut toDtoOut(Account a) {
        return new AccountDtoOut(a.getId(), a.getName(), a.getEmail(), a.getRole(),
                a.getStatus(), a.getEmailVerified(), a.getCreatedAt());
    }
}