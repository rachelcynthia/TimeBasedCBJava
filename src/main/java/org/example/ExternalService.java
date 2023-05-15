package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ExternalService {
    String callExternalService() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8000/external-service"))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = null;
        HttpClient hc = HttpClient.newBuilder().build();
        try {
            response = hc.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return String.valueOf(response != null ? response.body() : null);
    }
}
