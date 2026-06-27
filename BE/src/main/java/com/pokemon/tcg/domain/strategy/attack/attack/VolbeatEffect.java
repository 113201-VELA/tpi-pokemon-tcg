package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VolbeatEffect implements AttackEffect {

    private static final String LURING_GLOW  = "luring glow";
    private static final String SIGNAL_BEAM  = "signal beam";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("volbeat|luring glow", "volbeat|signal beam");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case LURING_GLOW -> applyLuringGlow(ctx);
            case SIGNAL_BEAM -> applySignalBeam(ctx);
            default          -> { }
        }
    }

    private void applyLuringGlow(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        if (opponent.getBench() == null || opponent.getBench().isEmpty()) return;

        ctx.setBoardState(ctx.getBoardState().toBuilder()
                .pendingForcedSwitchPlayerId(opponent.getPlayerId())
                .build());
    }

    private void applySignalBeam(AttackContext ctx) {
        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState opponent  = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        defender.getConditions().remove(SpecialCondition.ASLEEP);
        defender.getConditions().remove(SpecialCondition.PARALYZED);
        defender.getConditions().add(SpecialCondition.CONFUSED);
    }
}
