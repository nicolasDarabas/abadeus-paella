package br.com.abadeus.paellaapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/** Usado pela splash do app para saber se o servidor esta no ar. */
@RestController
public class StatusController {

    @GetMapping("/api/status")
    public Map<String, Object> status() {
        return Map.of("status", "online", "servico", "paella-api", "momento", LocalDateTime.now().toString());
    }
}
