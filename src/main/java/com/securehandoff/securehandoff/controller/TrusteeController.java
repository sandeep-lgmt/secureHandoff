package com.securehandoff.securehandoff.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.securehandoff.securehandoff.dto.TrusteeInviteRequest;
import com.securehandoff.securehandoff.dto.TrusteeLinkResponse;
import com.securehandoff.securehandoff.model.User;
import com.securehandoff.securehandoff.service.TrusteeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trustees")
@RequiredArgsConstructor
public class TrusteeController {

    private final TrusteeService trusteeService;

    @PostMapping("/invite")
    public ResponseEntity<TrusteeLinkResponse> invite(
            @AuthenticationPrincipal User owner,
            @Valid @RequestBody TrusteeInviteRequest request) {
        var link = trusteeService.inviteTrustee(owner, request.trusteeEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(TrusteeLinkResponse.from(link));
    }

    @PostMapping("/invite/{token}/accept")
    public ResponseEntity<TrusteeLinkResponse> accept(
            @AuthenticationPrincipal User acceptingUser,
            @PathVariable String token) {
        var link = trusteeService.acceptInvite(token, acceptingUser);
        return ResponseEntity.ok(TrusteeLinkResponse.from(link));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<TrusteeLinkResponse>> myTrustees(@AuthenticationPrincipal User owner) {
        var links = trusteeService.listMyTrustees(owner).stream().map(TrusteeLinkResponse::from).toList();
        return ResponseEntity.ok(links);
    }

    @GetMapping("/i-am-trustee-for")
    public ResponseEntity<List<TrusteeLinkResponse>> whereIAmTrustee(@AuthenticationPrincipal User trustee) {
        var links = trusteeService.listWhereIAmTrustee(trustee).stream().map(TrusteeLinkResponse::from).toList();
        return ResponseEntity.ok(links);
    }

    @DeleteMapping("/{linkId}")
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal User owner,
            @PathVariable Long linkId) {
        trusteeService.revokeTrustee(owner, linkId);
        return ResponseEntity.noContent().build();
    }
}
