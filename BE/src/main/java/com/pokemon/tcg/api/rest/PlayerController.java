package com.pokemon.tcg.api.rest;

import com.pokemon.tcg.api.dto.request.UpdateNicknameRequest;
import com.pokemon.tcg.api.dto.request.UpdatePasswordRequest;
import com.pokemon.tcg.api.dto.response.AuthResponse;
import com.pokemon.tcg.api.dto.response.CosmeticsOptionsResponse;
import com.pokemon.tcg.application.PlayerService;
import com.pokemon.tcg.domain.model.player.Player;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    /** Returns the available cosmetic options for card backs and coins. */
    @GetMapping("/cosmetics")
    public ResponseEntity<CosmeticsOptionsResponse> getCosmeticsOptions() {
        return ResponseEntity.ok(playerService.getCosmeticsOptions());
    }

    /** Updates the nickname of the authenticated player. */
    @PutMapping("/me/nickname")
    public ResponseEntity<AuthResponse> updateNickname(
            @AuthenticationPrincipal Player player,
            @Valid @RequestBody UpdateNicknameRequest request) {

        Player updated = playerService.updateNickname(player.getId(), request.nickname());
        return ResponseEntity.ok(new AuthResponse(
                updated.getId(),
                updated.getUsername(),
                updated.getNickname(),
                updated.getEmail(),
                null
        ));
    }

    /** Updates the password of the authenticated player. Requires the current password. */
    @PutMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal Player player,
            @Valid @RequestBody UpdatePasswordRequest request) {

        playerService.updatePassword(
                player.getId(),
                request.currentPassword(),
                request.newPassword()
        );
        return ResponseEntity.noContent().build();
    }
}
