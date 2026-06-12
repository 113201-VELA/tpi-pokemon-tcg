package com.pokemon.tcg.application;

import com.pokemon.tcg.api.dto.request.LoginRequest;
import com.pokemon.tcg.api.dto.request.RegisterRequest;
import com.pokemon.tcg.api.dto.response.AuthResponse;
import com.pokemon.tcg.domain.model.player.Player;
import com.pokemon.tcg.infrastructure.repository.PlayerRepository;
import com.pokemon.tcg.infrastructure.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(PlayerRepository playerRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.playerRepository = playerRepository;
        this.passwordEncoder  = passwordEncoder;
        this.jwtService       = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (playerRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (playerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Player player = Player.builder()
                .username(request.username())
                .nickname(request.nickname() != null && !request.nickname().isBlank() ? request.nickname() : request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        player = playerRepository.save(player);

        String token = jwtService.generateToken(player.getId(), player.getUsername());
        return new AuthResponse(player.getId(), player.getUsername(), player.getNickname(), player.getEmail(), token);
    }

    public AuthResponse login(LoginRequest request) {
        Player player = playerRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), player.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(player.getId(), player.getUsername());
        return new AuthResponse(player.getId(), player.getUsername(), player.getNickname(), player.getEmail(), token);
    }
}