package com.securehandoff.securehandoff.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.securehandoff.securehandoff.dto.ReleaseRequestResponse;
import com.securehandoff.securehandoff.model.User;
import com.securehandoff.securehandoff.service.ConsensusService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/release")
@RequiredArgsConstructor
public class ReleaseController {

    private final ConsensusService consensusService;

    /** Release requests waiting for the logged-in trustee. */
    @GetMapping("/pending")
    public ResponseEntity<List<ReleaseRequestResponse>> pending(@AuthenticationPrincipal User trustee) {
        return ResponseEntity.ok(consensusService.listPendingFor(trustee));
    }

    @PostMapping("/{releaseRequestId}/confirm")
    public ResponseEntity<ReleaseRequestResponse> confirm(
            @AuthenticationPrincipal User trustee,
            @PathVariable Long releaseRequestId) {
        return ResponseEntity.ok(consensusService.confirm(releaseRequestId, trustee));
    }

    /**
     * Returns decrypted packets. Succeeds ONLY if quorum was reached and the caller is an
     * accepted trustee of that owner; both rules are enforced inside ConsensusService.
     */
    @GetMapping("/{releaseRequestId}/packets")
    public ResponseEntity<Map<String, String>> getReleasedPackets(
            @AuthenticationPrincipal User trustee,
            @PathVariable Long releaseRequestId) {
        return ResponseEntity.ok(consensusService.getReleasedContent(releaseRequestId, trustee));
    }
}
