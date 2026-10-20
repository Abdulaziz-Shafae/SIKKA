package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.CreateVisitRecordDtoIn;
import com.example.sikka.DtoIn.UpdateVisitRecordDtoIn;
import com.example.sikka.DtoIn.RateVisitRecordDtoIn;
import com.example.sikka.DtoOut.VisitRecordDtoOut;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.Ride;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitMember;
import com.example.sikka.Model.VisitRecord;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.RideRepository;
import com.example.sikka.Repository.VisitMemberRepository;
import com.example.sikka.Repository.VisitRecordRepository;
import com.example.sikka.Repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitRecordService {

    private final VisitRecordRepository visitRecordRepository;
    private final AccountRepository accountRepository;
    private final VisitRepository visitRepository;
    private final VisitMemberRepository visitMemberRepository;
    private final RideRepository rideRepository;

    @Transactional(readOnly = true)
    public List<VisitRecordDtoOut> getMyVisitRecords(Integer accountId, Integer visitId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Visit visit = visitRepository.findVisitById(visitId);

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        VisitMember visitMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visitId, accountId);

        if (visitMember == null) {
            throw new ApiException("You are not a member of this visit");
        }

        List<VisitRecord> visitRecords = visitRecordRepository.findAllByVisitMember_IdOrderByCreatedAtDescIdDesc(visitMember.getId());

        if (visitRecords.isEmpty()) {
            throw new ApiException("No ride records found for this visit");
        }

        List<VisitRecordDtoOut> visitRecordDtoOuts = new ArrayList<>();

        for (VisitRecord visitRecord : visitRecords) {
            visitRecordDtoOuts.add(new VisitRecordDtoOut(visitRecord.getId(), visitRecord.getVisitMember().getId(), visitRecord.getRide().getId(), visitRecord.getRide().getName(), visitRecord.getRecordedByAccount().getId(), visitRecord.getStatus(), visitRecord.getCompletedAt(), visitRecord.getSkippedAt(), visitRecord.getRating(), visitRecord.getReviewNote(), visitRecord.getCorrectedAt(), visitRecord.getCreatedAt()));
        }

        return visitRecordDtoOuts;
    }

    @Transactional
    public void createVisitRecords(Integer accountId, Integer visitId, CreateVisitRecordDtoIn createVisitRecordDtoIn) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Visit visit = visitRepository.findVisitById(visitId);

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        VisitMember recordingMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visitId, accountId);

        if (recordingMember == null) {
            throw new ApiException("You are not a member of this visit");
        }

        if (!"GOING".equals(recordingMember.getRsvpStatus())) {
            throw new ApiException("Only GOING members can record ride activities");
        }

        if (!"IN_PROGRESS".equals(visit.getStatus())) {
            throw new ApiException("Ride activities can only be recorded during the visit");
        }

        if (createVisitRecordDtoIn == null) {
            throw new ApiException("Ride record details are required");
        }

        if (createVisitRecordDtoIn.getRideId() == null) {
            throw new ApiException("Ride ID is required");
        }

        Ride ride = rideRepository.findRideById(createVisitRecordDtoIn.getRideId());

        if (ride == null) {
            throw new ApiException("Ride not found");
        }

        String status = createVisitRecordDtoIn.getStatus();

        if (!"COMPLETED".equals(status) && !"SKIPPED".equals(status)) {
            throw new ApiException("Status must be COMPLETED or SKIPPED");
        }

        List<Integer> visitMemberIds = createVisitRecordDtoIn.getVisitMemberIds();

        if (visitMemberIds == null || visitMemberIds.isEmpty()) {
            throw new ApiException("Select at least one visit member");
        }

        if (new HashSet<>(visitMemberIds).size() != visitMemberIds.size()) {
            throw new ApiException("Visit member IDs must not be repeated");
        }

        List<VisitRecord> visitRecords = new ArrayList<>();
        LocalDateTime recordedAt = LocalDateTime.now();

        for (Integer visitMemberId : visitMemberIds) {
            if (visitMemberId == null || visitMemberId <= 0) {
                throw new ApiException("Visit member ID must be positive");
            }

            VisitMember visitMember = visitMemberRepository.findVisitMemberById(visitMemberId);

            if (visitMember == null) {
                throw new ApiException("Visit member not found");
            }

            if (!visitMember.getVisit().getId().equals(visitId)) {
                throw new ApiException("Every selected member must belong to this visit");
            }

            if (!"GOING".equals(visitMember.getRsvpStatus())) {
                throw new ApiException("Every selected member must be GOING");
            }

            VisitRecord visitRecord = new VisitRecord();
            visitRecord.setVisitMember(visitMember);
            visitRecord.setRide(ride);
            visitRecord.setRecordedByAccount(account);
            visitRecord.setStatus(status);

            if ("COMPLETED".equals(status)) {
                visitRecord.setCompletedAt(recordedAt);
            }

            if ("SKIPPED".equals(status)) {
                visitRecord.setSkippedAt(recordedAt);
            }

            visitRecords.add(visitRecord);
        }

        visitRecordRepository.saveAll(visitRecords);
    }

    @Transactional
    public void updateVisitRecordStatus(Integer accountId, UpdateVisitRecordDtoIn updateVisitRecordDtoIn) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        if (updateVisitRecordDtoIn == null || updateVisitRecordDtoIn.getVisitRecordId() == null) {
            throw new ApiException("Visit record ID is required");
        }

        VisitRecord visitRecord = visitRecordRepository.findVisitRecordByIdForUpdate(updateVisitRecordDtoIn.getVisitRecordId());

        if (visitRecord == null) {
            throw new ApiException("Visit record not found");
        }

        Visit visit = visitRecord.getVisitMember().getVisit();
        VisitMember actingMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visit.getId(), accountId);

        if (actingMember == null) {
            throw new ApiException("You are not a member of this visit");
        }

        if (!"GOING".equals(actingMember.getRsvpStatus())) {
            throw new ApiException("Only GOING members can correct ride activities");
        }

        if (!"IN_PROGRESS".equals(visit.getStatus()) && !"COMPLETED".equals(visit.getStatus())) {
            throw new ApiException("Records can only be corrected during or after a completed visit");
        }

        String status = updateVisitRecordDtoIn.getStatus();


        if (!"COMPLETED".equals(status) && !"SKIPPED".equals(status)) {
            throw new ApiException("Status must be COMPLETED or SKIPPED");
        }

        if (status.equals(visitRecord.getStatus())) {
            throw new ApiException("The record already has this status");
        }

        LocalDateTime correctedAt = LocalDateTime.now();
        visitRecord.setStatus(status);
        visitRecord.setCorrectedAt(correctedAt);
        visitRecord.setCompletedAt(null);
        visitRecord.setSkippedAt(null);

        if ("COMPLETED".equals(status)) {
            visitRecord.setCompletedAt(correctedAt);
        }

        if ("SKIPPED".equals(status)) {
            visitRecord.setSkippedAt(correctedAt);
        }

        if (!"COMPLETED".equals(status)) {
            visitRecord.setRating(null);
            visitRecord.setReviewNote(null);
        }

        visitRecordRepository.save(visitRecord);
    }

    @Transactional
    public void rateVisitRecord(Integer accountId, RateVisitRecordDtoIn rateVisitRecordDtoIn) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        if (rateVisitRecordDtoIn == null || rateVisitRecordDtoIn.getVisitRecordId() == null) {
            throw new ApiException("Visit record ID is required");
        }

        VisitRecord visitRecord = visitRecordRepository.findVisitRecordByIdForUpdate(rateVisitRecordDtoIn.getVisitRecordId());

        if (visitRecord == null) {
            throw new ApiException("Visit record not found");
        }

        if (!visitRecord.getVisitMember().getVisitorProfile().getAccount().getId().equals(accountId)) {
            throw new ApiException("You can only rate your own ride records");
        }

        if (!"COMPLETED".equals(visitRecord.getStatus())) {
            throw new ApiException("Only completed rides can be rated");
        }

        Integer rating = rateVisitRecordDtoIn.getRating();

        if (rating == null || rating < 1 || rating > 5) {
            throw new ApiException("Rating must be between 1 and 5");
        }

        visitRecord.setRating(rating);
        visitRecord.setReviewNote(rateVisitRecordDtoIn.getReviewNote());
        visitRecordRepository.save(visitRecord);
    }
}
