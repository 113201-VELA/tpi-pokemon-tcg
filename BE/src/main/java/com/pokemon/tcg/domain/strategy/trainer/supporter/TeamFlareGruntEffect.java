package com.pokemon.tcg.domain.strategy.trainer.supporter;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.trainer.TrainerEffect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Strategy implementation for Team Flare Grunt (xy1-129).
 * Effect: Discard an Energy attached to your opponent's Active Pokémon.
 *
 * <p>If the opponent's Active Pokémon has no attached Energy, the card
 * is played with no additional effect — this is not an error condition.
 *
 * <p>When multiple Energies are attached, the first one in the list is
 * discarded. In a full implementation this would require the player to
 * choose; for now the first attached Energy is used as a deterministic
 * default.
 *
 * <p>This is a Supporter card — the once-per-turn Supporter limit
 * is enforced by RuleValidator, not here.
 */
@Component
public class TeamFlareGruntEffect implements TrainerEffect {

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        String opponentId   = state.getOpponentState(action.getPlayerId()).getPlayerId();
        PlayerState opponent = state.getStateFor(opponentId);

        if (opponent.getActivePokemon() == null) {
            return ValidationResult.fail("Opponent has no Active Pokémon.");
        }
        return ValidationResult.ok();
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        String opponentId    = state.getOpponentState(action.getPlayerId()).getPlayerId();
        PlayerState opponent  = state.getStateFor(opponentId);
        ActivePokemon active  = opponent.getActivePokemon();

        String discardedEnergyId = null;

        // Discard the first attached Energy if any exist
        if (active.getAttachedEnergyIds() != null && !active.getAttachedEnergyIds().isEmpty()) {
            List<String> energies = new ArrayList<>(active.getAttachedEnergyIds());
            discardedEnergyId = energies.remove(0);
            active.setAttachedEnergyIds(energies);

            List<String> discard = new ArrayList<>(
                    opponent.getDiscard() != null ? opponent.getDiscard() : new ArrayList<>());
            discard.add(discardedEnergyId);
            opponent.setDiscard(discard);
        }

        // Mark Supporter as played this turn
        state.getTurnFlags().setSupporterPlayedThisTurn(true);

        Map<String, Object> eventData = discardedEnergyId != null
                ? Map.of("cardId", action.getPayloadString("cardId"),
                         "discardedEnergyId", discardedEnergyId)
                : Map.of("cardId", action.getPayloadString("cardId"));

        GameEvent event = GameEvent.builder()
                .type(GameEventType.TRAINER_PLAYED)
                .gameId(state.getGameId())
                .playerId(action.getPlayerId())
                .turnNumber(state.getTurnNumber())
                .data(eventData)
                .occurredAt(Instant.now())
                .build();

        return EngineResult.of(state, List.of(event));
    }
}
