package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ArbokEffect implements AttackEffect {

    private static final String GASTRO_ACID = "gastro acid";
    private static final String POISON_JAB  = "poison jab";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("arbok|gastro acid", "arbok|poison jab");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case GASTRO_ACID -> applyGastroAcid(ctx);
            case POISON_JAB  -> applyPoisonJab(ctx);
            default          -> { }
        }
    }

    /**
     * Gastro Acid: the Defending Pokémon has no Abilities until the end of
     * your next turn. Implemented by adding NO_ABILITIES to its activeEffects.
     * clearActiveEffects() in TurnManager removes it between turns.
     */
    private void applyGastroAcid(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        List<PokemonEffect> effects = new ArrayList<>(
                defender.getActiveEffects() != null
                        ? defender.getActiveEffects() : new ArrayList<>());
        if (!effects.contains(PokemonEffect.NO_ABILITIES)) {
            effects.add(PokemonEffect.NO_ABILITIES);
        }
        defender.setActiveEffects(effects);
    }

    /**
     * Poison Jab: the opponent's Active Pokémon is now Poisoned.
     */
    private void applyPoisonJab(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        defender.getConditions().add(SpecialCondition.POISONED);
    }
}