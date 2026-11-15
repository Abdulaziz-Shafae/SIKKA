package com.example.sikka.Config;

import com.example.sikka.Service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sikka.park.initialize-empty-catalogue", havingValue = "true")
public class ParkCatalogueInitializer implements ApplicationRunner {
    private final MapService mapService;

    @Override
    public void run(ApplicationArguments arguments) {
        mapService.initializeEmptyParkCatalogue();
    }
}
