package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.CreateVisitDtoIn;
import com.example.sikka.DtoIn.VisitDtoIn;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitGroup;
import com.example.sikka.Model.VisitMember;
import com.example.sikka.Model.VisitorProfile;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.VisitGroupRepository;
import com.example.sikka.Repository.VisitMemberRepository;
import com.example.sikka.Repository.VisitRepository;
import com.example.sikka.Repository.VisitorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository visitRepository;
    private final AccountRepository accountRepository;
    private final VisitGroupRepository visitGroupRepository;
    private final VisitMemberRepository visitMemberRepository;
    private final VisitorProfileRepository visitorProfileRepository;
    private final VisitMemberService visitMemberService;
    private final VisitGroupService visitGroupService;

    // ---------- CRUD ----------

    public List<Visit> get() {
        return visitRepository.findAll();
    }

    public void add(VisitDtoIn dto) {
        Account organizer = accountRepository.findAccountById(dto.getOrganizerId());
        if (organizer == null)
            throw new ApiException("organizer not found");

        checkTimes(dto.getPlannedStartTime(), dto.getPlannedEndTime());

        Visit visit = new Visit();
        if (dto.getGroupId() != null) {
            VisitGroup group = visitGroupRepository.findVisitGroupById(dto.getGroupId());
            if (group == null)
                throw new ApiException("group not found");
            if (!group.getOrganizer().getId().equals(organizer.getId()))
                throw new ApiException("group belongs to another organizer");
            if (group.getVisit() != null)
                throw new ApiException("group is already linked to a visit");
            visit.setGroup(group);
        }

        visit.setOrganizer(organizer);
        visit.setName(dto.getName());
        visit.setVisitDate(dto.getVisitDate());
        visit.setPlannedStartTime(dto.getPlannedStartTime());
        visit.setPlannedEndTime(dto.getPlannedEndTime());
        visit.setMaxParticipants(dto.getMaxParticipants());
        visit.setStatus("DRAFT");
        visit.setMembersLocked(false);
        visitRepository.save(visit);
    }

    public void update(Integer id, VisitDtoIn dto) {
        Visit visit = visitRepository.findVisitById(id);
        if (visit == null)
            throw new ApiException("visit not found");

        checkTimes(dto.getPlannedStartTime(), dto.getPlannedEndTime());

        if (dto.getMaxParticipants() < visitMemberRepository.countByVisit(visit))
            throw new ApiException("max participants is less than current members");

        visit.setName(dto.getName());
        visit.setVisitDate(dto.getVisitDate());
        visit.setPlannedStartTime(dto.getPlannedStartTime());
        visit.setPlannedEndTime(dto.getPlannedEndTime());
        visit.setMaxParticipants(dto.getMaxParticipants());
        visitRepository.save(visit);
    }

    public void delete(Integer id) {
        Visit visit = visitRepository.findVisitById(id);
        if (visit == null)
            throw new ApiException("visit not found");

        if (visit.getGroup() != null)
            visit.getGroup().setVisit(null);
        visitRepository.delete(visit);
    }

    // ---------- Flow 1 extras ----------

    // creates the visit, adds the organizer as GOING, and a group only if groupName is sent
    // returns the raw invite code, or null for a solo visit
    @Transactional
    public String create(Integer accountId, CreateVisitDtoIn dto) {
        Account organizer = accountRepository.findAccountById(accountId);
        if (organizer == null)
            throw new ApiException("account not found");

        VisitorProfile profile = visitorProfileRepository.findVisitorProfileByAccount_Id(accountId);
        if (profile == null)
            throw new ApiException("create your visitor profile first");

        checkTimes(dto.getPlannedStartTime(), dto.getPlannedEndTime());

        Visit visit = new Visit();
        visit.setOrganizer(organizer);
        visit.setName(dto.getName());
        visit.setVisitDate(dto.getVisitDate());
        visit.setPlannedStartTime(dto.getPlannedStartTime());
        visit.setPlannedEndTime(dto.getPlannedEndTime());
        visit.setMaxParticipants(dto.getMaxParticipants());
        visit.setStatus("DRAFT");
        visit.setMembersLocked(false);
        visitRepository.save(visit);

        visitMemberService.addOrganizer(visit, profile);

        if (dto.getGroupName() != null && !dto.getGroupName().isBlank())
            return visitGroupService.attachGroup(visit, dto.getGroupName());
        return null;
    }

    // every visit the account is a member of (the organizer is a member too)
    public List<Visit> myVisits(Integer accountId) {
        if (accountRepository.findAccountById(accountId) == null)
            throw new ApiException("account not found");

        List<Visit> visits = new ArrayList<>();
        for (VisitMember m : visitMemberRepository.findAllByVisitorProfile_Account_Id(accountId))
            visits.add(m.getVisit());
        return visits;
    }

    @Transactional
    public void cancel(Integer accountId, Integer visitId) {
        checkOrganizer(accountId, visitId);
        changeStatus(visitId, "CANCELLED");
    }

    @Transactional
    public void startVisit(Integer accountId, Integer visitId) {
        checkOrganizer(accountId, visitId);
        changeStatus(visitId, "IN_PROGRESS");
    }

    @Transactional
    public void completeVisit(Integer accountId, Integer visitId) {
        checkOrganizer(accountId, visitId);
        changeStatus(visitId, "COMPLETED");
    }

    // ---------- contract methods for the other flows ----------

    public Visit getVisit(Integer visitId) {
        Visit visit = visitRepository.findVisitById(visitId);
        if (visit == null)
            throw new ApiException("visit not found");
        return visit;
    }

    public boolean isOrganizer(Integer visitId, Integer accountId) {
        Visit visit = visitRepository.findVisitById(visitId);
        return visit != null && visit.getOrganizer().getId().equals(accountId);
    }

    // the only place where Visit.status changes
    @Transactional
    public void changeStatus(Integer visitId, String status) {
        Visit visit = visitRepository.findVisitByIdForUpdate(visitId);
        if (visit == null)
            throw new ApiException("visit not found");

        if (!isAllowed(visit.getStatus(), status))
            throw new ApiException("cannot change status from " + visit.getStatus() + " to " + status);

        visit.setStatus(status);
        visitRepository.save(visit);
    }

    // called when plan generation starts, members list becomes fixed
    @Transactional
    public void lockMembers(Integer visitId) {
        Visit visit = visitRepository.findVisitByIdForUpdate(visitId);
        if (visit == null)
            throw new ApiException("visit not found");
        if (!visit.getStatus().equals("PLANNING"))
            throw new ApiException("visit is not in planning");

        visit.setMembersLocked(true);
        visitRepository.save(visit);
    }

    // ---------- helpers ----------

    private boolean isAllowed(String from, String to) {
        return switch (from) {
            case "DRAFT" -> to.equals("PLANNING") || to.equals("CANCELLED");
            case "PLANNING" -> to.equals("READY") || to.equals("CANCELLED");
            case "READY" -> to.equals("IN_PROGRESS") || to.equals("CANCELLED");
            case "IN_PROGRESS" -> to.equals("COMPLETED");
            default -> false;
        };
    }

    private void checkOrganizer(Integer accountId, Integer visitId) {
        Visit visit = visitRepository.findVisitById(visitId);
        if (visit == null)
            throw new ApiException("visit not found");
        if (!visit.getOrganizer().getId().equals(accountId))
            throw new ApiException("only the organizer can do this");
    }

    private void checkTimes(LocalTime start, LocalTime end) {
        if (start != null && end != null && !end.isAfter(start))
            throw new ApiException("end time must be after start time");
    }
}