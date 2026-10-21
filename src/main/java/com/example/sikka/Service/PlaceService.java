package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.Place;
import com.example.sikka.Repository.PlaceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;

    public List<Place> getPlaces(){
        return placeRepository.findAll();
    }

    public Place getPlaceById(Integer placeId){
        Place place = placeRepository.findPlaceById(placeId);

        if (place == null){
            throw new ApiException("Place not found");
        }

        return place;
    }

    public void addPlace(Place place){
        validatePlace(place);

        Place newPlace = new Place();
        copyPlaceFields(newPlace, place);

        placeRepository.save(newPlace);
    }

    @Transactional
    public void updatePlace(Integer placeId, Place place){
        Place oldPlace = placeRepository.findPlaceById(placeId);

        if (oldPlace == null){
            throw new ApiException("Place not found");
        }

        validatePlace(place);
        copyPlaceFields(oldPlace, place);

        placeRepository.save(oldPlace);
    }

    @Transactional
    public void deletePlace(Integer placeId){
        Place place = placeRepository.findPlaceById(placeId);

        if (place == null){
            throw new ApiException("Place not found");
        }

        if (!place.getPlanItems().isEmpty()){
            throw new ApiException("Cannot delete a place linked to plans");
        }

        placeRepository.delete(place);
    }

    private void validatePlace(Place place){
        if ((place.getMapX() == null) != (place.getMapY() == null)){
            throw new ApiException("Both map coordinates must be provided together");
        }
    }

    private void copyPlaceFields(Place target, Place source){
        target.setName(source.getName());
        target.setKind(source.getKind());
        target.setMapX(source.getMapX());
        target.setMapY(source.getMapY());
        target.setDescription(source.getDescription());
    }

    public List<Place> getPlaces(String kind){
        if (kind == null || (!"RESTAURANT".equals(kind) && !"MEETING_POINT".equals(kind) && !"RESTROOM".equals(kind) && !"OTHER".equals(kind))){
            throw new ApiException("Invalid place kind");
        }

        return placeRepository.findPlacesByKind(kind);
    }
}