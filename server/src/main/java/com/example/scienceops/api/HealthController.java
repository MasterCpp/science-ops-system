package com.example.scienceops.api;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "service", "science-ops-server",
                        "status", "UP",
                        "time", OffsetDateTime.now().toString()
                ),
                "message", "OK"
        ));
    }
}
