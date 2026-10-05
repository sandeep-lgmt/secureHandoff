package com.securehandoff.securehandoff.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.securehandoff.securehandoff.service.TrusteeService;

import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/trustees")
@RequiredArgsConstructor
public class TrsusteeController {
     private final TrusteeService trusteeService;

    /** Owner invites someone by email to become their trustee. */
    @PostMapping("/invite")
    public ResponseEntity<TrusteeLinkResponse> invite(
            @AuthenticationPrincipal User owner,
            @Valid @RequestBody TrusteeInviteRequest request
    ) {
        var link = trusteeService.inviteTrustee(owner, request.trusteeEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(TrusteeLinkResponse.from(link));
    }

    /** The invited person (already logged in / just registered) accepts using their invite token. */
    @PostMapping("/invite/{token}/accept")
    public ResponseEntity<TrusteeLinkResponse> accept(
            @AuthenticationPrincipal User acceptingUser,
            @PathVariable String token
    ) {
        var link = trusteeService.acceptInvite(token, acceptingUser);
        return ResponseEntity.ok(TrusteeLinkResponse.from(link));
    }

    /** List the trustees I (as an owner) have designated. */
    @GetMapping("/mine")
    public ResponseEntity<List<TrusteeLinkResponse>> myTrustees(@AuthenticationPrincipal User owner) {
        var links = trusteeService.listMyTrustees(owner).stream().map(TrusteeLinkResponse::from).toList();
        return ResponseEntity.ok(links);
    }

    /** List the owners for whom I act as a trustee. */
    @GetMapping("/i-am-trustee-for")
    public ResponseEntity<List<TrusteeLinkResponse>> whereIAmTrustee(@AuthenticationPrincipal User trustee) {
        var links = trusteeService.listWhereIAmTrustee(trustee).stream().map(TrusteeLinkResponse::from).toList();
        return ResponseEntity.ok(links);
    }

    @DeleteMapping("/{linkId}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal User owner, @PathVariable Long linkId) {
        trusteeService.revokeTrustee(owner, linkId);
        return ResponseEntity.noContent().build();
    }

}
