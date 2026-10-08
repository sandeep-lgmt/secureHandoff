package com.securehandoff.securehandoff.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.securehandoff.securehandoff.dto.CheckInStatusResponse;
import com.securehandoff.securehandoff.dto.SetupCheckInRequest;
import com.securehandoff.securehandoff.model.User;
import com.securehandoff.securehandoff.service.CheckInService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckInService checkInService;

    @PostMapping("/setup")
    public ResponseEntity<CheckInStatusResponse> setup(
            @AuthenticationPrincipal User owner,
            @Valid @RequestBody SetupCheckInRequest request) {
        var config = checkInService.setupCheckIn(owner, request.frequencyDays());
        return ResponseEntity.ok(CheckInStatusResponse.from(config));
    }

    @PostMapping
    public ResponseEntity<CheckInStatusResponse> checkIn(@AuthenticationPrincipal User owner) {
        var config = checkInService.checkIn(owner);
        return ResponseEntity.ok(CheckInStatusResponse.from(config));
    }

    @GetMapping("/status")
    public ResponseEntity<CheckInStatusResponse> status(@AuthenticationPrincipal User owner) {
        var config = checkInService.getStatus(owner);
        return ResponseEntity.ok(CheckInStatusResponse.from(config));
    }
}
