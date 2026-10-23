package com.example.sikka.Controller;

import com.example.sikka.Service.MapService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.LinkRideMapDtoIn;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/v1/map")
@AllArgsConstructor
public class MapController {

    private final MapService mapService;

    @GetMapping("/get/rides")
    public ResponseEntity<?> getMapRides(){
        return ResponseEntity.status(200).body(mapService.getMapRides());
    }

    @PutMapping("/link/ride/{accountId}/{rideId}")
    public ResponseEntity<?> linkRideToMap(@PathVariable Integer accountId, @PathVariable Integer rideId, @RequestBody @Valid LinkRideMapDtoIn dto){

        mapService.linkRideToMap(accountId, rideId, dto);

        return ResponseEntity.status(200).body(new ApiResponse("Ride linked to map successfully"));
    }

    @PostMapping("/import/rides/{accountId}")
    public ResponseEntity<?> importMapRides(@PathVariable Integer accountId){

        int addedCount = mapService.importMapRides(accountId);

        return ResponseEntity.status(200).body(new ApiResponse(addedCount + " rides imported successfully"));
    }

    @PostMapping("/import/places/{accountId}")
    public ResponseEntity<?> importMapPlaces(@PathVariable Integer accountId){
        int addedCount = mapService.importMapPlaces(accountId);

        return ResponseEntity.status(200).body(
                new ApiResponse(addedCount + " places imported successfully"));
    }

    @GetMapping("/get/places")
    public ResponseEntity<?> getMapPlaces(){
        return ResponseEntity.status(200).body(mapService.getMapPlaces());
    }

    @GetMapping("/route/{fromType}/{fromId}/{toType}/{toId}")
    public ResponseEntity<?> getRoute(
            @PathVariable String fromType,
            @PathVariable Integer fromId,
            @PathVariable String toType,
            @PathVariable Integer toId){

        return ResponseEntity.status(200).body(
                mapService.getRoute(fromType, fromId, toType, toId)
        );
    }

    @GetMapping("/get/data")
    public ResponseEntity<?> getMapData(){
        return ResponseEntity.status(200).body(mapService.getMapData());
    }
}