package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.JoinDtoIn;
import com.example.sikka.DtoIn.ReviewDtoIn;
import com.example.sikka.DtoIn.VisitMemberDtoIn;
import com.example.sikka.DtoOut.VisitMemberDtoOut;
import com.example.sikka.DtoOut.VisitMembersSummaryDtoOut;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitGroup;
import com.example.sikka.Model.VisitMember;
import com.example.sikka.Model.VisitorProfile;
import com.example.sikka.Repository.VisitMemberRepository;
import com.example.sikka.Repository.VisitRepository;
import com.example.sikka.Repository.VisitorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitMemberService {

    private final VisitMemberRepository visitMemberRepository;
    private final VisitRepository visitRepository;
    private final VisitorProfileRepository visitorProfileRepository;
    private final VisitGroupService visitGroupService;

    // ---------- CRUD ----------

    public List<VisitMember> get() {
        return visitMemberRepository.findAll();
    }

    public void add(VisitMemberDtoIn dto) {
        Visit visit = visitRepository.findVisitById(dto.getVisitId());
        if (visit == null)
            throw new ApiException("visit not found");

        VisitorProfile profile = visitorProfileRepository.findVisitorProfileById(dto.getVisitorProfileId());
        if (profile == null)
            throw new ApiException("visitor profile not found");

        createMember(visit, profile);
    }

    public void delete(Integer id) {
        VisitMember member = visitMemberRepository.findVisitMemberById(id);
        if (member == null)
            throw new ApiException("member not found");

        checkOpenForChanges(member.getVisit());
        if (member.getVisitorProfile().getAccount().getId().equals(member.getVisit().getOrganizer().getId()))
            throw new ApiException("organizer cannot be removed");
        visitMemberRepository.delete(member);
    }

    // ---------- Flow 1 extras ----------

    // called by VisitService.create, the organizer is always a GOING member
    public void addOrganizer(Visit visit, VisitorProfile profile) {
        VisitMember member = new VisitMember();
        member.setVisit(visit);
        member.setVisitorProfile(profile);
        member.setRsvpStatus("GOING");
        member.setInvitedAt(LocalDateTime.now());
        visitMemberRepository.save(member);
    }

    // visitor joins by invite code, no organizer approval
    public void join(Integer accountId, JoinDtoIn dto) {
        VisitorProfile profile = visitorProfileRepository.findVisitorProfileByAccount_Id(accountId);
        if (profile == null)
            throw new ApiException("create your visitor profile first");

        VisitGroup group = visitGroupService.getGroupByInviteCode(dto.getCode());
        if (!Boolean.TRUE.equals(group.getInvitationsEnabled()))
            throw new ApiException("invitations are disabled for this group");

        Visit visit = group.getVisit();
        if (visit == null)
            throw new ApiException("group has no visit yet");

        createMember(visit, profile);
    }

    public void updateRsvp(Integer accountId, Integer visitId, String rsvp) {
        if (rsvp == null || !rsvp.matches("^(GOING|MAYBE|NOT_GOING)$"))
            throw new ApiException("rsvp must be GOING, MAYBE or NOT_GOING");

        VisitMember member = getMember(accountId, visitId);
        checkOpenForChanges(member.getVisit());

        member.setRsvpStatus(rsvp);
        visitMemberRepository.save(member);
    }

    public void updateSelectedRides(Integer accountId, Integer visitId, String selectedRides) {
        VisitMember member = getMember(accountId, visitId);
        checkOpenForChanges(member.getVisit());

        member.setSelectedRides(selectedRides);
        visitMemberRepository.save(member);
    }

    public void checkIn(Integer accountId, Integer visitId) {
        VisitMember member = getMember(accountId, visitId);

        if (!member.getVisit().getStatus().equals("IN_PROGRESS"))
            throw new ApiException("visit is not in progress");
        if (!"GOING".equals(member.getRsvpStatus()))
            throw new ApiException("only going members can check in");
        if (member.getCheckedInAt() != null)
            throw new ApiException("already checked in");

        member.setCheckedInAt(LocalDateTime.now());
        visitMemberRepository.save(member);
    }

    public void review(Integer accountId, Integer visitId, ReviewDtoIn dto) {
        VisitMember member = getMember(accountId, visitId);

        if (!member.getVisit().getStatus().equals("COMPLETED"))
            throw new ApiException("visit is not completed yet");
        if (!"GOING".equals(member.getRsvpStatus()))
            throw new ApiException("only going members can review");

        if (dto == null || dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new ApiException("Visit rating must be between 1 and 5");
        }
        member.setVisitRating(dto.getRating());
        member.setVisitReview(dto.getReview());
        visitMemberRepository.save(member);
    }

    public void leave(Integer accountId, Integer visitId) {
        VisitMember member = getMember(accountId, visitId);

        if (member.getVisit().getOrganizer().getId().equals(accountId))
            throw new ApiException("organizer cannot leave, cancel the visit instead");
        checkOpenForChanges(member.getVisit());

        visitMemberRepository.delete(member);
    }

    // organizer removes a member
    public void remove(Integer organizerId, Integer visitId, Integer memberId) {
        Visit visit = visitRepository.findVisitById(visitId);
        if (visit == null)
            throw new ApiException("visit not found");
        if (!visit.getOrganizer().getId().equals(organizerId))
            throw new ApiException("only the organizer can remove members");

        VisitMember member = visitMemberRepository.findVisitMemberById(memberId);
        if (member == null || !member.getVisit().getId().equals(visitId))
            throw new ApiException("member not found in this visit");
        if (member.getVisitorProfile().getAccount().getId().equals(organizerId))
            throw new ApiException("organizer cannot be removed");

        checkOpenForChanges(visit);
        visitMemberRepository.delete(member);
    }

    public VisitMembersSummaryDtoOut getSummary(Integer visitId) {
        if (visitRepository.findVisitById(visitId) == null)
            throw new ApiException("visit not found");

        List<VisitMemberDtoOut> list = new ArrayList<>();
        int going = 0;
        for (VisitMember m : visitMemberRepository.findAllByVisit_Id(visitId)) {
            list.add(new VisitMemberDtoOut(m.getId(), m.getVisitorProfile().getId(),
                    m.getVisitorProfile().getName(), m.getRsvpStatus(), m.getCheckedInAt()));
            if ("GOING".equals(m.getRsvpStatus()))
                going++;
        }
        return new VisitMembersSummaryDtoOut(list.size(), going, list);
    }

    // ---------- contract methods for the other flows ----------

    public List<VisitMember> getGoingMembers(Integer visitId) {
        return visitMemberRepository.findAllByVisit_IdAndRsvpStatus(visitId, "GOING");
    }

    public VisitMember getVisitMember(Integer memberId) {
        VisitMember member = visitMemberRepository.findVisitMemberById(memberId);
        if (member == null)
            throw new ApiException("member not found");
        return member;
    }

    public List<Integer> getMemberAccountIds(Integer visitId) {
        List<Integer> ids = new ArrayList<>();
        for (VisitMember m : visitMemberRepository.findAllByVisit_Id(visitId))
            ids.add(m.getVisitorProfile().getAccount().getId());
        return ids;
    }

    // ---------- helpers ----------

    private VisitMember getMember(Integer accountId, Integer visitId) {
        VisitMember member = visitMemberRepository
                .findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visitId, accountId);
        if (member == null)
            throw new ApiException("you are not a member of this visit");
        return member;
    }

    // join, rsvp, rides, leave and remove are allowed in DRAFT or PLANNING until the plan generation starts
    private void checkOpenForChanges(Visit visit) {
        String s = visit.getStatus();
        if ((!s.equals("DRAFT") && !s.equals("PLANNING")) || Boolean.TRUE.equals(visit.getMembersLocked()))
            throw new ApiException("members are locked for this visit");
    }

    private void createMember(Visit visit, VisitorProfile profile) {
        checkOpenForChanges(visit);

        if (visitMemberRepository.findVisitMemberByVisitAndVisitorProfile(visit, profile) != null)
            throw new ApiException("visitor is already a member of this visit");

        if (visitMemberRepository.countByVisit(visit) >= visit.getMaxParticipants())
            throw new ApiException("visit is full");

        VisitMember member = new VisitMember();
        member.setVisit(visit);
        member.setVisitorProfile(profile);
        member.setRsvpStatus("INVITED");
        member.setInvitedAt(LocalDateTime.now());
        visitMemberRepository.save(member);
    }
}