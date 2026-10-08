package com.securehandoff.securehandoff.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securehandoff.securehandoff.exception.ApiException;
import com.securehandoff.securehandoff.model.Role;
import com.securehandoff.securehandoff.model.TrusteeLink;
import com.securehandoff.securehandoff.model.User;
import com.securehandoff.securehandoff.repository.TrusteeLinkRepository;
import com.securehandoff.securehandoff.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrusteeService {

    private static final Duration INVITE_TTL = Duration.ofDays(7);

    private final TrusteeLinkRepository trusteeLinkRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public TrusteeLink inviteTrustee(User owner, String trusteeEmail) {
        String email = trusteeEmail.toLowerCase().trim();

        if (email.equals(owner.getEmail())) {
            throw new ApiException("You can't designate yourself as your own trustee", HttpStatus.BAD_REQUEST);
        }

        boolean alreadyActive = trusteeLinkRepository.findByOwner(owner).stream().anyMatch(link ->
                link.getTrusteeEmail().equalsIgnoreCase(email)
                        && (link.getStatus() == TrusteeLink.TrusteeStatus.PENDING
                            || link.getStatus() == TrusteeLink.TrusteeStatus.ACCEPTED));
        if (alreadyActive) {
            throw new ApiException("This person is already invited or an accepted trustee", HttpStatus.CONFLICT);
        }

        TrusteeLink link = TrusteeLink.builder()
                .owner(owner)
                .trusteeEmail(email)
                .status(TrusteeLink.TrusteeStatus.PENDING)
                .inviteToken(UUID.randomUUID().toString())
                .inviteExpiresAt(Instant.now().plus(INVITE_TTL))
                .build();

        TrusteeLink saved = trusteeLinkRepository.save(link);
        notificationService.sendInviteEmail(email, owner.getFullName(), saved.getInviteToken());
        return saved;
    }

    @Transactional
    public TrusteeLink acceptInvite(String inviteToken, User acceptingUser) {
        TrusteeLink link = trusteeLinkRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new ApiException("Invalid or expired invite", HttpStatus.NOT_FOUND));

        if (link.getInviteExpiresAt() != null && Instant.now().isAfter(link.getInviteExpiresAt())) {
            throw new ApiException("Invalid or expired invite", HttpStatus.NOT_FOUND);
        }

        if (!link.getTrusteeEmail().equalsIgnoreCase(acceptingUser.getEmail())) {
            throw new ApiException("This invite was sent to a different email address", HttpStatus.FORBIDDEN);
        }

        if (link.getStatus() != TrusteeLink.TrusteeStatus.PENDING) {
            throw new ApiException("This invite has already been responded to", HttpStatus.CONFLICT);
        }

        link.setTrusteeUser(acceptingUser);
        link.setStatus(TrusteeLink.TrusteeStatus.ACCEPTED);
        link.setRespondedAt(Instant.now());
        trusteeLinkRepository.save(link);

        if (!acceptingUser.getRoles().contains(Role.TRUSTEE)) {
            acceptingUser.getRoles().add(Role.TRUSTEE);
            userRepository.save(acceptingUser);
        }

        return link;
    }

    @Transactional(readOnly = true)
    public List<TrusteeLink> listMyTrustees(User owner) {
        return trusteeLinkRepository.findByOwner(owner);
    }

    @Transactional(readOnly = true)
    public List<TrusteeLink> listWhereIAmTrustee(User trustee) {
        return trusteeLinkRepository.findByTrusteeUser(trustee);
    }

    @Transactional
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
