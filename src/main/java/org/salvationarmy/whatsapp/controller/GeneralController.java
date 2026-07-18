package org.salvationarmy.whatsapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeneralController {

    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok("HTF Data collection API is running.");
    }

    @GetMapping("/favicon.ico")
    public void favicon() {
        // Just to prevent 500 when browser asks for it
    }
}
