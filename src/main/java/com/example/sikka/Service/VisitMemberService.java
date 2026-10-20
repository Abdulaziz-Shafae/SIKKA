package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.VisitMemberDtoIn;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitMemberService {

    private final VisitMemberRepository visitMemberRepository;
    private final VisitRepository visitRepository;
    private final VisitorProfileRepository visitorProfileRepository;
    private final VisitGroupService visitGroupService;

    public List<VisitMember> get() {
        return visitMemberRepository.findAll();
    }

    // organizer adds a profile directly
    public void add(VisitMemberDtoIn dto) {
        Visit visit = visitRepository.findVisitById(dto.getVisitId());
        if (visit == null)
            throw new ApiException("visit not found");

        VisitorProfile profile = visitorProfileRepository.findVisitorProfileById(dto.getVisitorProfileId());
        if (profile == null)
            throw new ApiException("visitor profile not found");

        createMember(visit, profile);
    }

    // visitor joins by invite code, no organizer approval
    public void join(String code, Integer visitorProfileId) {
        VisitGroup group = visitGroupService.getGroupByInviteCode(code);
        if (!group.getInvitationsEnabled())
            throw new ApiException("invitations are disabled for this group");

        Visit visit = group.getVisit();
        if (visit == null)
            throw new ApiException("group has no visit yet");

        VisitorProfile profile = visitorProfileRepository.findVisitorProfileById(visitorProfileId);
        if (profile == null)
            throw new ApiException("visitor profile not found");

        createMember(visit, profile);
    }

    public void updateRsvp(Integer id, String rsvp) {
        VisitMember member = visitMemberRepository.findVisitMemberById(id);
        if (member == null)
            throw new ApiException("member not found");

        if (!rsvp.matches("^(GOING|MAYBE|NOT_GOING)$"))
            throw new ApiException("rsvp must be GOING, MAYBE or NOT_GOING");

        member.setRsvpStatus(rsvp);
        visitMemberRepository.save(member);
    }

    public void updateSelectedRides(Integer id, String selectedRides) {
        VisitMember member = visitMemberRepository.findVisitMemberById(id);
        if (member == null)
            throw new ApiException("member not found");

        member.setSelectedRides(selectedRides);
        visitMemberRepository.save(member);
    }

    public void delete(Integer id) {
        VisitMember member = visitMemberRepository.findVisitMemberById(id);
        if (member == null)
            throw new ApiException("member not found");

        visitMemberRepository.delete(member);
    }

    private void createMember(Visit visit, VisitorProfile profile) {
        if (visit.getStatus().equals("CANCELLED") || visit.getStatus().equals("COMPLETED"))
            throw new ApiException("visit is closed");

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