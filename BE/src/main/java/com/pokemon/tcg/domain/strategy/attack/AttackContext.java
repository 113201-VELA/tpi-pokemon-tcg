package com.pokemon.tcg.domain.strategy.attack;

import com.pokemon.tcg.domain.model.card.Attack;
import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.domain.model.game.GameEvent;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttackContext {
    private BoardState boardState;
    private GameAction action;
    private String attackName;
    private Attack attack;
    private boolean cancelled;
    private String cancellationReason;
    private int damageToApply;
    /** Max HP of the defending Active Pokémon, loaded from card cache before pipeline executes. */
    private int defenderMaxHp;
    private List<DamageModifier> modifiers;
    private List<GameEvent> events;

    @Builder.Default
    private boolean ignoreDefenderEffects = false;

    public void cancel(String reason) {
        this.cancelled = true;
        this.cancellationReason = reason;
    }

    public void addEvent(GameEvent event) {
        if (events == null) events = new ArrayList<>();
        events.add(event);
    }
}