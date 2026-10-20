package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.VisitGroupDtoIn;
import com.example.sikka.DtoOut.VisitGroupDtoOut;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitGroup;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.VisitGroupRepository;
import com.example.sikka.Repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitGroupService {

    private final VisitGroupRepository visitGroupRepository;
    private final AccountRepository accountRepository;
    private final VisitRepository visitRepository;

    public List<VisitGroupDtoOut> get() {
        List<VisitGroupDtoOut> list = new ArrayList<>();
        for (VisitGroup g : visitGroupRepository.findAll())
            list.add(toDtoOut(g));
        return list;
    }

    // returns the raw invite code (shown once, only its hash is stored)
    public String add(VisitGroupDtoIn dto) {
        Account organizer = accountRepository.findAccountById(dto.getOrganizerId());
        if (organizer == null)
            throw new ApiException("organizer not found");

        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        VisitGroup group = new VisitGroup();
        group.setName(dto.getName());
        group.setInvitationsEnabled(dto.getInvitationsEnabled() == null ? true : dto.getInvitationsEnabled());
        group.setOrganizer(organizer);
        group.setInviteCodeHash(hash(code));
        visitGroupRepository.save(group);
        return code;
    }

    public void update(Integer id, VisitGroupDtoIn dto) {
        VisitGroup group = visitGroupRepository.findVisitGroupById(id);
        if (group == null)
            throw new ApiException("group not found");

        group.setName(dto.getName());
        if (dto.getInvitationsEnabled() != null)
            group.setInvitationsEnabled(dto.getInvitationsEnabled());
        visitGroupRepository.save(group);
    }

    public void delete(Integer id) {
        VisitGroup group = visitGroupRepository.findVisitGroupById(id);
        if (group == null)
            throw new ApiException("group not found");

        Visit visit = group.getVisit();
        if (visit != null) {
            visit.setGroup(null);
            group.setVisit(null);
            visitRepository.save(visit);
        }
        visitGroupRepository.delete(group);
    }

    // used by VisitMemberService to join by invite code
    public VisitGroup getGroupByInviteCode(String code) {
        VisitGroup group = visitGroupRepository.findVisitGroupByInviteCodeHash(hash(code));
        if (group == null)
            throw new ApiException("invalid invite code");
        return group;
    }

    private String hash(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException("hashing error");
        }
    }

    private VisitGroupDtoOut toDtoOut(VisitGroup g) {
        return new VisitGroupDtoOut(g.getId(), g.getName(), g.getOrganizer().getId(),
                g.getInvitationsEnabled(), g.getCreatedAt());
    }
}