package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.VisitDtoIn;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitGroup;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.VisitGroupRepository;
import com.example.sikka.Repository.VisitMemberRepository;
import com.example.sikka.Repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository visitRepository;
    private final AccountRepository accountRepository;
    private final VisitGroupRepository visitGroupRepository;
    private final VisitMemberRepository visitMemberRepository;

    public List<Visit> get() {
        return visitRepository.findAll();
    }

    public void add(VisitDtoIn dto) {
        Account organizer = accountRepository.findAccountById(dto.getOrganizerId());
        if (organizer == null)
            throw new ApiException("organizer not found");

        checkTimes(dto);

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
        visitRepository.save(visit);
    }

    public void update(Integer id, VisitDtoIn dto) {
        Visit visit = visitRepository.findVisitById(id);
        if (visit == null)
            throw new ApiException("visit not found");

        checkTimes(dto);

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

    private void checkTimes(VisitDtoIn dto) {
        if (dto.getPlannedStartTime() != null && dto.getPlannedEndTime() != null
                && !dto.getPlannedEndTime().isAfter(dto.getPlannedStartTime()))
            throw new ApiException("end time must be after start time");
    }
}