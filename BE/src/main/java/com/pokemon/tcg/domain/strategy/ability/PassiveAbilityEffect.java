package com.pokemon.tcg.domain.strategy.ability;

import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;

public interface PassiveAbilityEffect {

    void onDamageReceived(AttackContext ctx, ActivePokemon defender);

    String getIdentifier();
}
