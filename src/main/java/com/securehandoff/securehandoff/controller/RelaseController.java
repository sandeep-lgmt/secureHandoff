package com.securehandoff.securehandoff.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/release")
@RequiredArgsConstructor
public class RelaseController {
     private final ConsensusService consensusService;

    /** A trustee confirms a release request. Requires quorum before anything unlocks. */
    @PostMapping("/{releaseRequestId}/confirm")
    public ResponseEntity<ReleaseRequestResponse> confirm(
            @AuthenticationPrincipal User trustee,
            @PathVariable Long releaseRequestId
    ) {
        var request = consensusService.confirm(releaseRequestId, trustee);
        return ResponseEntity.ok(ReleaseRequestResponse.from(request));
    }

    /**
     * Returns decrypted packet content, keyed by title. Only works once QUORUM_MET,
     * and only for an accepted trustee of that owner — enforced inside ConsensusService.
     */
    @GetMapping("/{releaseRequestId}/packets")
    public ResponseEntity<Map<String, String>> getReleasedPackets(
            @AuthenticationPrincipal User trustee,
            @PathVariable Long releaseRequestId
    ) {
        return ResponseEntity.ok(consensusService.getReleasedContent(releaseRequestId, trustee));
    }

}
