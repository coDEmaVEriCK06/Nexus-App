package com.nexus.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class PingController {

    public record PingResponse(String status, String app, String version, String time) {}

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ok", "nexus", "1.0.0", Instant.now().toString());
    }
}
