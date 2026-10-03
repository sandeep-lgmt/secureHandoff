package com.securehandoff.securehandoff.service;

import org.apache.kafka.common.errors.ApiException;
import org.springframework.http.HttpStatus;

import com.securehandoff.securehandoff.model.TrusteeLink;
import com.securehandoff.securehandoff.repository.TrusteeLinkRepository;
import com.securehandoff.securehandoff.repository.UserRepository;
import com.securehandoff.exception.ApiException;
import com.securehandoff.model.Role;
import com.securehandoff.model.TrusteeLink;
import com.securehandoff.model.User;
import com.securehandoff.repository.TrusteeLinkRepository;
import com.securehandoff.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrusteeService {

     private final TrusteeLinkRepository trusteeLinkRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Owner invites someone (by email) to be a trustee. The invite is a signed, unguessable
     * token — in Phase 4 this gets emailed out via the notification service.
     */
    public TrusteeLink inviteTrustee(User owner, String trusteeEmail) {
        String normalizedEmail = trusteeEmail.toLowerCase().trim();

        if (normalizedEmail.equals(owner.getEmail())) {
            throw new ApiException("You can't designate yourself as your own trustee", HttpStatus.BAD_REQUEST);
        }

        boolean duplicate = trusteeLinkRepository.findByOwner(owner).stream().anyMatch(l ->
                l.getTrusteeEmail().equalsIgnoreCase(normalizedEmail)
                        && (l.getStatus() == TrusteeLink.TrusteeStatus.PENDING || l.getStatus() == TrusteeLink.TrusteeStatus.ACCEPTED));
        if (duplicate) {
            throw new ApiException("This person is already invited or an accepted trustee", HttpStatus.CONFLICT);
        }

        TrusteeLink link = TrusteeLink.builder()
                .owner(owner)
                .trusteeEmail(normalizedEmail)
                .status(TrusteeLink.TrusteeStatus.PENDING)
                .inviteToken(UUID.randomUUID().toString())
                .build();

        TrusteeLink saved = trusteeLinkRepository.save(link);
        notificationService.sendInviteEmail(normalizedEmail, owner.getFullName(), saved.getInviteToken());
        return saved;
    }

    /**
     * The invited person accepts using the token from their invite email/link. They must
     * already have (or now create) a SecureHandoff account matching the invited email.
     */
    public TrusteeLink acceptInvite(String inviteToken, User acceptingUser) {
        TrusteeLink link = trusteeLinkRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new ApiException("Invalid or expired invite", HttpStatus.NOT_FOUND));

        if (!link.getTrusteeEmail().equalsIgnoreCase(acceptingUser.getEmail())) {
            throw new ApiException("This invite was sent to a different email address", HttpStatus.FORBIDDEN);
        }

        if (link.getStatus() != TrusteeLink.TrusteeStatus.PENDING) {
            throw new ApiException("This invite has already been responded to", HttpStatus.CONFLICT);
        }

        link.setTrusteeUser(acceptingUser);
        link.setStatus(TrusteeLink.TrusteeStatus.ACCEPTED);
        link.setRespondedAt(java.time.Instant.now());
        trusteeLinkRepository.save(link);

        // Grant the TRUSTEE role so their JWT reflects the new capability on next login.
        if (!acceptingUser.getRoles().contains(Role.TRUSTEE)) {
            acceptingUser.getRoles().add(Role.TRUSTEE);
            userRepository.save(acceptingUser);
        }

        return link;
    }

    public List<TrusteeLink> listMyTrustees(User owner) {
        return trusteeLinkRepository.findByOwner(owner);
    }

    public List<TrusteeLink> listWhereIAmTrustee(User trustee) {
        return trusteeLinkRepository.findByTrusteeUser(trustee);
    }

    public void revokeTrustee(User owner, Long linkId) {
        TrusteeLink link = trusteeLinkRepository.findById(linkId)
                .orElseThrow(() -> new ApiException("Trustee link not found", HttpStatus.NOT_FOUND));

        if (!link.getOwner().getId().equals(owner.getId())) {
            throw new ApiException("You don't have permission to modify this trustee link", HttpStatus.FORBIDDEN);
        }

        link.setStatus(TrusteeLink.TrusteeStatus.REVOKED);
        trusteeLinkRepository.save(link);
    }
}
