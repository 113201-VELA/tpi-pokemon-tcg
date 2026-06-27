package com.pokemon.tcg.engine.ability;

import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.engine.attack.AttackContext;

public interface PassiveAbilityEffect {

    void onDamageReceived(AttackContext ctx, ActivePokemon defender);
}
