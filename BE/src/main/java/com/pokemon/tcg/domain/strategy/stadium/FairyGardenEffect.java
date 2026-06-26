package com.pokemon.tcg.domain.strategy.stadium;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.TrainerEffect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Strategy implementation for Fairy Garden (xy1-117).
 * Effect: each Fairy-type Pokémon with at least one Fairy Basic Energy attached
 * has no retreat cost.
 *
 * <p>Playing a Stadium replaces the previous one in play (if any).
 * The actual retreat cost suppression is handled by {@code RuleValidator}
 * and {@code TurnManager}, which read {@code activeStadiumCardId} from
 * the {@code BoardState}.
 *
 * <p>This is a Stadium card — the once-per-turn Stadium limit is enforced
 * by RuleValidator, not here.
 */
@Component
public class FairyGardenEffect implements TrainerEffect {

    private static final String CARD_ID = "xy1-117";

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        // Discard the previous Stadium if one was in play
        String previousStadiumId = state.getActiveStadiumCardId();
        if (previousStadiumId != null) {
            String playerId = action.getPlayerId();
            PlayerState ps  = state.getStateFor(playerId);
            discardCard(ps, previousStadiumId);
        }

        // Set this Stadium as the active one
        state.setActiveStadiumCardId(CARD_ID);

        // Mark Stadium as played this turn
        state.getTurnFlags().setStadiumPlayedThisTurn(true);

        GameEvent event = GameEvent.builder()
                .type(GameEventType.TRAINER_PLAYED)
                .gameId(state.getGameId())
                .playerId(action.getPlayerId())
                .turnNumber(state.getTurnNumber())
                .data(Map.of("cardId", CARD_ID))
                .occurredAt(Instant.now())
                .build();

        return EngineResult.of(state, List.of(event));
    }

    private void discardCard(PlayerState ps, String cardId) {
        List<String> discard = new ArrayList<>(
                ps.getDiscard() != null ? ps.getDiscard() : new ArrayList<>());
        discard.add(cardId);
        ps.setDiscard(discard);
    }
}
