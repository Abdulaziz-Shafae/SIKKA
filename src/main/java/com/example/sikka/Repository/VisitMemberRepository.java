package com.example.sikka.Repository;

import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitMember;
import com.example.sikka.Model.VisitorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitMemberRepository extends JpaRepository<VisitMember, Integer> {
    VisitMember findVisitMemberById(Integer id);
    VisitMember findVisitMemberByVisitAndVisitorProfile(Visit visit, VisitorProfile visitorProfile);
    long countByVisit(Visit visit);
}