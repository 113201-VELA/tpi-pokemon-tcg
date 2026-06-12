package com.pokemon.tcg.domain.engine.attack.steps;

import com.pokemon.tcg.domain.engine.attack.AttackChain;
import com.pokemon.tcg.domain.engine.attack.AttackContext;
import com.pokemon.tcg.domain.engine.attack.AttackStep;
import com.pokemon.tcg.domain.model.card.Attack;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.ActivePokemon;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Step 1 — Verifies the attacking Pokémon has enough energy attached
 * to perform the declared attack. Cancels the attack if not.
 */
@Component
public class EnergyCheckStep implements AttackStep {

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        Attack attack = ctx.getAttack();
        ActivePokemon attacker = ctx.getBoardState()
                .getStateFor(ctx.getAction().getPlayerId())
                .getActivePokemon();

        if (attack == null || attacker == null) {
            ctx.cancel("No attack or attacker found");
            return;
        }

        if (attack.getCost() == null || attack.getCost().isEmpty()) {
            chain.next(ctx);
            return;
        }

        List<String> attached = attacker.getAttachedEnergyIds() != null
                ? new ArrayList<>(attacker.getAttachedEnergyIds())
                : new ArrayList<>();

        List<EnergyType> required = new ArrayList<>(attack.getCost());
        List<EnergyType> colorless = new ArrayList<>();

        // Separate colorless from typed requirements
        for (EnergyType e : required) {
            if (e == EnergyType.COLORLESS) colorless.add(e);
        }
        required.removeAll(colorless);

        // For now, we count attached energies vs required (simplified)
        int totalRequired = attack.getCost().size();
        if (attached.size() < totalRequired) {
            ctx.cancel("Not enough energy to use " + attack.getName());
            return;
        }

        chain.next(ctx);
    }
}