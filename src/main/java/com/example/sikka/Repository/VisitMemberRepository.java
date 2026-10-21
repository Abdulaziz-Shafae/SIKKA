package com.example.sikka.Repository;

import com.example.sikka.Model.VisitMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitMemberRepository extends JpaRepository<VisitMember, Integer> {

    VisitMember findVisitMemberById(Integer id);

    VisitMember findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(Integer visitId, Integer accountId);

    List<VisitMember> findAllByVisit_IdAndRsvpStatus(Integer visitId, String rsvpStatus);
}