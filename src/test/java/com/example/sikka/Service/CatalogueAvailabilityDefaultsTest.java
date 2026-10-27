package com.example.sikka.Service;

import com.example.sikka.Model.Ride;
import com.example.sikka.Model.Place;
import com.example.sikka.Repository.RideRepository;
import com.example.sikka.Repository.PlaceRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogueAvailabilityDefaultsTest {
    @Test
    void enableSeedEntriesOnceAndNeverReopenKnownClosedOrManuallyUnavailableRides() {
        RideRepository rides = mock(RideRepository.class);
        PlaceRepository places = mock(PlaceRepository.class);
        Ride seed = new Ride();
        seed.setSourceRideId("adrena-line");
        Ride closed = new Ride();
        closed.setSourceRideId("windrider");
        closed.setStatus("CLOSED");
        Ride manual = new Ride();
        manual.setSourceRideId("saw-mill-falls");
        manual.setAvailabilitySource("ADMIN");
        Place restaurant = new Place();
        restaurant.setSourcePlaceId("chicken-sweets");
        restaurant.setKind("RESTAURANT");
        restaurant.setStatus("UNAVAILABLE");
        when(rides.findAll()).thenReturn(List.of(seed, closed, manual));
        when(places.findAll()).thenReturn(List.of(restaurant));
        CatalogueAvailabilityDefaultsService service = new CatalogueAvailabilityDefaultsService(rides, places);
        service.enableUnconfiguredCatalogueEntries();
        seed.setStatus("UNAVAILABLE");
        service.enableUnconfiguredCatalogueEntries();
        assertEquals("UNAVAILABLE", seed.getStatus());
        assertEquals("SIKKA_DEFAULT_AVAILABILITY", seed.getAvailabilitySource());
        assertEquals("CLOSED", closed.getStatus());
        assertEquals("UNAVAILABLE", manual.getStatus());
        assertEquals("OPEN", restaurant.getStatus());
        verify(rides, times(1)).save(seed);
        verify(rides, never()).save(closed);
        verify(rides, never()).save(manual);
        verify(places, times(1)).save(restaurant);
    }
}
