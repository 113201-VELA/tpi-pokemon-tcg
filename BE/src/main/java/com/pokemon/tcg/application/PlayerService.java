package com.pokemon.tcg.application;

import com.pokemon.tcg.api.dto.response.CosmeticsOptionsResponse;
import com.pokemon.tcg.domain.model.deck.DeckCosmetic;
import com.pokemon.tcg.domain.model.player.Player;
import com.pokemon.tcg.infrastructure.repository.PlayerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    public PlayerService(PlayerRepository playerRepository,
                         PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.passwordEncoder  = passwordEncoder;
    }

    /** Updates the nickname of the given player. Fails if the nickname is already taken. */
    public Player updateNickname(UUID playerId, String newNickname) {
        if (playerRepository.existsByNickname(newNickname)) {
            throw new IllegalArgumentException("Nickname already taken");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        player.setNickname(newNickname);
        return playerRepository.save(player);
    }

    /** Updates the password of the given player. Requires the current password for verification. */
    public void updatePassword(UUID playerId, String currentPassword, String newPassword) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        if (!passwordEncoder.matches(currentPassword, player.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        player.setPassword(passwordEncoder.encode(newPassword));
        playerRepository.save(player);
    }

    /** Returns all available cosmetic options for card backs and coins. */
    @Transactional(readOnly = true)
    public CosmeticsOptionsResponse getCosmeticsOptions() {
        List<String> options = Arrays.stream(DeckCosmetic.values())
                .map(Enum::name)
                .toList();
        return new CosmeticsOptionsResponse(options, options);
    }
}
