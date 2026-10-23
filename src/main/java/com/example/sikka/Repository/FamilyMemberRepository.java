package com.example.sikka.Repository;

import com.example.sikka.Model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Integer> {

    FamilyMember findFamilyMemberById(Integer id);

    List<FamilyMember> findFamilyMembersByOwnerAccountId(Integer accountId);

    boolean existsByOwnerAccountIdAndVisitorProfileId(Integer accountId, Integer visitorProfileId);
}