package com.example.sikka.Repository;

import com.example.sikka.Model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Integer> {

    Ride findRideById(Integer id);

    List<Ride> findRidesByStatus(String status);
}