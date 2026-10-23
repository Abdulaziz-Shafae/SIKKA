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
import com.example.sikka.DtoIn.ReuseVisitDtoIn;
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

    @Transactional
    public void add(VisitDtoIn dto) {
        if (dto == null) {
            throw new ApiException("Visit details are required");
        }
        Account organizer = accountRepository.findAccountById(dto.getOrganizerId());
        if (organizer == null)
            throw new ApiException("organizer not found");

        checkTimes(dto.getPlannedStartTime(), dto.getPlannedEndTime());

        VisitorProfile profile = visitorProfileRepository.findVisitorProfileByAccount_Id(organizer.getId());
        if (profile == null) {
            throw new ApiException("create your visitor profile first");
        }
        validateVisitDetails(dto.getVisitDate(), dto.getMaxParticipants());
        if (!"ACTIVE".equals(organizer.getStatus())) {
            throw new ApiException("Account is not active");
        }

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
        visitMemberService.addOrganizer(visit, profile);
    }

    @Transactional
    public void update(Integer id, VisitDtoIn dto) {
        if (dto == null) {
            throw new ApiException("Visit details are required");
        }
        Visit visit = visitRepository.findVisitByIdForUpdate(id);
        if (visit == null)
            throw new ApiException("visit not found");

        checkTimes(dto.getPlannedStartTime(), dto.getPlannedEndTime());

        if (!"DRAFT".equals(visit.getStatus()) || Boolean.TRUE.equals(visit.getMembersLocked())) {
            throw new ApiException("Visit details can only be changed while the visit is in DRAFT");
        }
        validateVisitDetails(dto.getVisitDate(), dto.getMaxParticipants());

        if (dto.getMaxParticipants() < visitMemberRepository.countByVisit(visit))
            throw new ApiException("max participants is less than current members");

        visit.setName(dto.getName());
        visit.setVisitDate(dto.getVisitDate());
        visit.setPlannedStartTime(dto.getPlannedStartTime());
        visit.setPlannedEndTime(dto.getPlannedEndTime());
        visit.setMaxParticipants(dto.getMaxParticipants());
        visitRepository.save(visit);
    }

    @org.springframework.transaction.annotation.Transactional
    public void delete(Integer id) {
        Visit visit = visitRepository.findVisitById(id);
        if (visit == null)
            throw new ApiException("visit not found");

        if ((visit.getVisitPlans() != null && !visit.getVisitPlans().isEmpty()) || (visit.getNotifications() != null && !visit.getNotifications().isEmpty()) || (visit.getPaymentOrders() != null && !visit.getPaymentOrders().isEmpty())) {
            throw new ApiException("Cannot delete a visit linked to plans, notifications or payments");
        }
        if (visit.getMembers() != null) {
            for (VisitMember member : visit.getMembers()) {
                if ((member.getVisitPlans() != null && !member.getVisitPlans().isEmpty()) || (member.getVisitRecords() != null && !member.getVisitRecords().isEmpty())) {
                    throw new ApiException("Cannot delete a visit whose members have plans or ride records");
                }
            }
        }
        if (visit.getGroup() != null)
            visit.getGroup().setVisit(null);
        if (visit.getMembers() != null) {
            for (VisitMember member : visit.getMembers()) {
                if (member.getVisitorProfile() != null && member.getVisitorProfile().getVisitMembers() != null) {
                    member.getVisitorProfile().getVisitMembers().remove(member);
                }
            }
        }
        if (visit.getOrganizer().getOrganizedVisits() != null) {
            visit.getOrganizer().getOrganizedVisits().remove(visit);
        }
        visitRepository.delete(visit);
    }

    // ---------- Flow 1 extras ----------

    // creates the visit and adds the organizer as GOING
    // returns the raw invite code for a FRIENDS trip, or null for SOLO and FAMILY
    @Transactional
    public String create(Integer accountId, CreateVisitDtoIn dto) {
        if (dto == null) {
            throw new ApiException("Visit details are required");
        }
        int max = resolveMaxParticipants(dto);
        validateVisitDetails(dto.getVisitDate(), max);
        Account organizer = accountRepository.findAccountById(accountId);
        if (organizer == null)
            throw new ApiException("account not found");

        if (!"ACTIVE".equals(organizer.getStatus())) {
            throw new ApiException("Account is not active");
        }
        VisitorProfile profile = visitorProfileRepository.findVisitorProfileByAccount_Id(accountId);
        if (profile == null)
            throw new ApiException("create your visitor profile first");

        checkTimes(dto.getPlannedStartTime(), dto.getPlannedEndTime());

        Visit visit = new Visit();
        visit.setOrganizer(organizer);
        fillVisit(visit, dto, max);
        visit.setStatus("DRAFT");
        visit.setMembersLocked(false);
        visitRepository.save(visit);

        visitMemberService.addOrganizer(visit, profile);

        // only a FRIENDS trip has a group and an invite code
        if ("FRIENDS".equals(dto.getTripType())) {
            String groupName = dto.getGroupName();
            if (groupName == null || groupName.isBlank())
                groupName = (dto.getName() == null || dto.getName().isBlank()) ? "Friends group" : dto.getName();
            return visitGroupService.attachGroup(visit, groupName);
        }
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
    // visit details, only for members of the visit
    public Visit getVisitDetails(Integer accountId, Integer visitId) {
        Visit visit = visitRepository.findVisitById(visitId);
        if (visit == null)
            throw new ApiException("visit not found");
        if (visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visitId, accountId) == null)
            throw new ApiException("you are not a member of this visit");
        return visit;
    }

    // organizer edits the visit details (same body as create, groupName is ignored)
    @Transactional
    public void updateByOrganizer(Integer accountId, Integer visitId, CreateVisitDtoIn dto) {
        if (dto == null)
            throw new ApiException("Visit details are required");

        Visit visit = visitRepository.findVisitByIdForUpdate(visitId);
        if (visit == null)
            throw new ApiException("visit not found");
        if (!visit.getOrganizer().getId().equals(accountId))
            throw new ApiException("only the organizer can do this");

        checkTimes(dto.getPlannedStartTime(), dto.getPlannedEndTime());

        if (!"DRAFT".equals(visit.getStatus()) || Boolean.TRUE.equals(visit.getMembersLocked()))
            throw new ApiException("Visit details can only be changed while the visit is in DRAFT");

        if (visit.getTripType() != null && !visit.getTripType().equals(dto.getTripType()))
            throw new ApiException("trip type cannot be changed, create a new visit");

        int max = resolveMaxParticipants(dto);
        validateVisitDetails(dto.getVisitDate(), max);

        if (max < visitMemberRepository.countByVisit(visit))
            throw new ApiException("max participants is less than current members");

        fillVisit(visit, dto, max);
        visitRepository.save(visit);
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

    // creates a new DRAFT visit from an old one with a new date
    // copies the details and the organizer's selected rides, not the friends and not the plan
    // returns the raw invite code for a FRIENDS trip, or null
    @Transactional
    public String reuse(Integer accountId, Integer visitId, ReuseVisitDtoIn dto) {
        if (dto == null)
            throw new ApiException("Visit date is required");

        Visit old = visitRepository.findVisitById(visitId);
        if (old == null)
            throw new ApiException("visit not found");
        if (!old.getOrganizer().getId().equals(accountId))
            throw new ApiException("only the organizer can do this");
        if (old.getTripType() == null)
            throw new ApiException("this visit has no trip type, create a new visit instead");

        validateVisitDetails(dto.getVisitDate(), old.getMaxParticipants());

        Account organizer = old.getOrganizer();
        if (!"ACTIVE".equals(organizer.getStatus()))
            throw new ApiException("Account is not active");
        VisitorProfile profile = visitorProfileRepository.findVisitorProfileByAccount_Id(accountId);
        if (profile == null)
            throw new ApiException("create your visitor profile first");

        Visit visit = new Visit();
        visit.setOrganizer(organizer);
        visit.setName(old.getName());
        visit.setVisitDate(dto.getVisitDate());
        visit.setPlannedStartTime(old.getPlannedStartTime());
        visit.setPlannedEndTime(old.getPlannedEndTime());
        visit.setMaxParticipants(old.getMaxParticipants());
        visit.setTripType(old.getTripType());
        visit.setGroupMode(old.getGroupMode());
        visit.setGoals(old.getGoals());
        visit.setTopGoal(old.getTopGoal());
        visit.setPrayerEnabled(old.getPrayerEnabled());
        visit.setStatus("DRAFT");
        visit.setMembersLocked(false);
        visitRepository.save(visit);

        VisitMember newMember = visitMemberService.addOrganizer(visit, profile);
        VisitMember oldMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visitId, accountId);
        if (oldMember != null && oldMember.getSelectedRides() != null) {
            newMember.setSelectedRides(oldMember.getSelectedRides());
            visitMemberRepository.save(newMember);
        }

        // friends do not carry over: a FRIENDS trip gets a new group and a new invite code
        if ("FRIENDS".equals(old.getTripType())) {
            String groupName = old.getGroup() != null ? old.getGroup().getName()
                    : ((old.getName() == null || old.getName().isBlank()) ? "Friends group" : old.getName());
            return visitGroupService.attachGroup(visit, groupName);
        }
        return null;
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

        if (status == null || visit.getStatus() == null) {
            throw new ApiException("Visit status is required");
        }
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

    private void validateVisitDetails(java.time.LocalDate visitDate, Integer maxParticipants) {
        if (visitDate == null || visitDate.isBefore(java.time.LocalDate.now())) {
            throw new ApiException("Visit date must be today or later");
        }
        if (maxParticipants == null || maxParticipants <= 0) {
            throw new ApiException("Maximum participants must be positive");
        }
    }

    private void checkTimes(LocalTime start, LocalTime end) {
        if (start != null && end != null && !end.isAfter(start))
            throw new ApiException("end time must be after start time");
    }

    // checks the rules of each trip type and returns the real maxParticipants
    private int resolveMaxParticipants(CreateVisitDtoIn dto) {
        String type = dto.getTripType();
        if ("SOLO".equals(type)) {
            if (dto.getGroupMode() != null || dto.getFamilyCount() != null)
                throw new ApiException("SOLO trip has no group mode or family count");
            return 1;
        }
        if ("FRIENDS".equals(type)) {
            if (dto.getGroupMode() == null)
                throw new ApiException("group mode is required for FRIENDS trip");
            if (dto.getFamilyCount() != null)
                throw new ApiException("family count is only for FAMILY trip");
            if (dto.getMaxParticipants() == null)
                throw new ApiException("max participants is required for FRIENDS trip");
            return dto.getMaxParticipants();
        }
        if ("FAMILY".equals(type)) {
            if (dto.getGroupMode() != null)
                throw new ApiException("group mode is only for FRIENDS trip");
            if (dto.getFamilyCount() == null)
                throw new ApiException("family count is required for FAMILY trip");
            return dto.getFamilyCount() + 1;
        }
        throw new ApiException("invalid trip type");
    }

    // copies the visit details from the dto (used by create and update)
    private void fillVisit(Visit visit, CreateVisitDtoIn dto, int max) {
        visit.setName(dto.getName());
        visit.setVisitDate(dto.getVisitDate());
        visit.setPlannedStartTime(dto.getPlannedStartTime());
        visit.setPlannedEndTime(dto.getPlannedEndTime());
        visit.setMaxParticipants(max);
        visit.setTripType(dto.getTripType());
        visit.setGroupMode(dto.getGroupMode());
        visit.setGoals(dto.getGoals());
        visit.setTopGoal(dto.getTopGoal());
        visit.setPrayerEnabled(Boolean.TRUE.equals(dto.getPrayerEnabled()));
    }

}
