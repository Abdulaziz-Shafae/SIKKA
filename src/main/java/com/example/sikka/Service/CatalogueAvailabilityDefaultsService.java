package com.example.sikka.Service;

import com.example.sikka.Model.Ride;
import com.example.sikka.Model.Place;
import com.example.sikka.Repository.RideRepository;
import com.example.sikka.Repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogueAvailabilityDefaultsService {
    private final RideRepository rideRepository;
    private final PlaceRepository placeRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void enableUnconfiguredCatalogueEntries() {
        for (Ride ride : rideRepository.findAll()) {
            if ("UNAVAILABLE".equals(ride.getStatus()) && ride.getAvailabilitySource() == null && ride.getSourceRideId() != null && ride.getWaitObservedAt() == null && ride.getWaitSource() == null) {
                ride.setStatus("OPEN");
                ride.setAvailabilitySource("SIKKA_DEFAULT_AVAILABILITY");
                rideRepository.save(ride);
            }
        }
        for (Place place : placeRepository.findAll()) {
            if ("RESTAURANT".equals(place.getKind()) && "UNAVAILABLE".equals(place.getStatus()) && place.getAvailabilitySource() == null && place.getSourcePlaceId() != null) {
                place.setStatus("OPEN");
                place.setAvailabilitySource("SIKKA_DEFAULT_AVAILABILITY");
                placeRepository.save(place);
            }
        }
    }
}
