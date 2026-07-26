package com.stage.auth.authbackend.controller.geo;

import com.stage.auth.authbackend.service.geo.CountriesNowProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Proxy vers l'API Countries Now. Le frontend appelle ce controller
 * pour éviter CORS ; le backend appelle countriesnow.space.
 */
@RestController
@RequestMapping("/api/countriesnow")
@RequiredArgsConstructor
public class CountriesNowProxyController {

    private final CountriesNowProxyService countriesNowProxy;

    @GetMapping("/countries")
    public ResponseEntity<List<Map<String, String>>> getCountries() {
        return ResponseEntity.ok(countriesNowProxy.getCountries());
    }

    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates(@RequestParam String country) {
        return ResponseEntity.ok(countriesNowProxy.getStates(country));
    }
}
