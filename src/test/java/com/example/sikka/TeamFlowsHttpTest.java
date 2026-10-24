package com.example.sikka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.datasource.url=jdbc:h2:mem:sikka-http;MODE=MySQL;DB_CLOSE_DELAY=-1")
class TeamFlowsHttpTest {
    @LocalServerPort private int port;

    @Test
    void serveMapAndResolveOneReuseHandler() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        URI mapUri = URI.create("http://localhost:" + port + "/api/v1/map/get/data");
        HttpResponse<String> map = client.send(HttpRequest.newBuilder(mapUri).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, map.statusCode());
        assertTrue(map.body().contains("SIKKA_IMAGE_ROUTE_V1"));
        URI reuseUri = URI.create("http://localhost:" + port + "/api/v1/visit/reuse/999999/999999");
        String body = "{\"visitDate\":\"" + LocalDate.now().plusDays(2) + "\"}";
        HttpResponse<String> reuse = client.send(HttpRequest.newBuilder(reuseUri).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, reuse.statusCode());
        assertTrue(reuse.body().contains("Account not found"));
    }
}
