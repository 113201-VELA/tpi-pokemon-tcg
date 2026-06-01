package com.pokemon.tcg.domain.engine.attack;

//import com.pokemon.tcg.domain.model.game.Attack;
import com.pokemon.tcg.domain.model.card.Attack;
import com.pokemon.tcg.domain.model.game.BoardState;
//import com.pokemon.tcg.domain.model.game.DamageModifier;
import com.pokemon.tcg.domain.engine.DamageModifier;
import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.domain.model.game.GameEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private List<DamageModifier> modifiers;
    private List<GameEvent> events;

    public void cancel(String reason) {
        this.cancelled = true;
        this.cancellationReason = reason;
    }

    public void addEvent(GameEvent event) {
        if (events == null) events = new ArrayList<>();
        events.add(event);
    }
}
