package com.pokemon.tcg.domain.strategy.trainer.item;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.trainer.TrainerEffect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class HardCharmEffect implements TrainerEffect {

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        String targetId = action.getPayloadString("targetInstanceId");
        PlayerState ps  = state.getStateFor(action.getPlayerId());

        if (targetId == null) {
            return ValidationResult.fail("No target Pokémon specified for Hard Charm.");
        }
        if (!isInPlay(ps, targetId)) {
            return ValidationResult.fail("Target Pokémon is not in play.");
        }
        if (hasToolAttached(ps, targetId)) {
            return ValidationResult.fail("That Pokémon already has a Pokémon Tool attached.");
        }
        return ValidationResult.ok();
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        String targetId = action.getPayloadString("targetInstanceId");
        String cardId   = action.getPayloadString("cardId");
        PlayerState ps  = state.getStateFor(action.getPlayerId());

        attachTool(ps, targetId, cardId);

        GameEvent event = GameEvent.builder()
                .type(GameEventType.TRAINER_PLAYED)
                .gameId(state.getGameId())
                .playerId(action.getPlayerId())
                .turnNumber(state.getTurnNumber())
                .data(Map.of(
                        "cardId",            cardId,
                        "targetInstanceId",  targetId))
                .occurredAt(Instant.now())
                .build();

        return EngineResult.of(state, List.of(event));
    }

    private boolean isInPlay(PlayerState ps, String instanceId) {
        if (ps.getActivePokemon() != null &&
                ps.getActivePokemon().getInstanceId().equals(instanceId)) return true;
        return ps.getBench() != null &&
                ps.getBench().stream().anyMatch(b -> b.getInstanceId().equals(instanceId));
    }

    private boolean hasToolAttached(PlayerState ps, String instanceId) {
        if (ps.getActivePokemon() != null &&
                ps.getActivePokemon().getInstanceId().equals(instanceId)) {
            return ps.getActivePokemon().getAttachedToolId() != null;
        }
        if (ps.getBench() != null) {
            return ps.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(instanceId))
                    .findFirst()
                    .map(b -> b.getAttachedToolId() != null)
                    .orElse(false);
        }
        return false;
    }

    private void attachTool(PlayerState ps, String instanceId, String cardId) {
        if (ps.getActivePokemon() != null &&
                ps.getActivePokemon().getInstanceId().equals(instanceId)) {
            ps.getActivePokemon().setAttachedToolId(cardId);
            return;
        }
        if (ps.getBench() != null) {
            ps.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(instanceId))
                    .findFirst()
                    .ifPresent(b -> b.setAttachedToolId(cardId));
        }
    }
}
