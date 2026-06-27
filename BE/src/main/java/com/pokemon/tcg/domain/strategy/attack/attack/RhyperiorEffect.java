package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * xy1-62 Rhyperior
 *
 * Rock Blast: flip a coin for each Fighting Energy attached; 50 damage × heads.
 * Rock Wrecker: 130 damage not affected by Weakness or Resistance.
 *               Rhyperior can't attack during your next turn.
 */
@Component
public class RhyperiorEffect implements AttackEffect {

    private static final String FIGHTING_TYPE  = EnergyType.FIGHTING.name();
    private static final String ROCK_BLAST     = "rock blast";
    private static final String ROCK_WRECKER   = "rock wrecker";

    private final CoinFlipService coinFlipService;
    private final CardLookupPort  cardLookupPort;

    public RhyperiorEffect(CoinFlipService coinFlipService, CardLookupPort cardLookupPort) {
        this.coinFlipService = coinFlipService;
        this.cardLookupPort  = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("rhyperior|rock blast", "rhyperior|rock wrecker");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case ROCK_BLAST   -> applyRockBlast(ctx);
            case ROCK_WRECKER -> applyRockWrecker(ctx);
            default           -> { }
        }
    }

    /**
     * Rock Blast: flip one coin per Fighting Energy attached to Rhyperior.
     * Add 50 damage per heads result via a damage modifier.
     */
    private void applyRockBlast(AttackContext ctx) {
        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState attacker  = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon rhyperior = attacker.getActivePokemon();

        if (rhyperior == null) return;

        List<String> energies = rhyperior.getAttachedEnergyIds();
        if (energies == null || energies.isEmpty()) return;

        long fightingCount = energies.stream()
                .filter(this::isFightingEnergy)
                .count();

        if (fightingCount == 0) return;

        int heads = 0;
        for (int i = 0; i < fightingCount; i++) {
            if (coinFlipService.flip() == CoinResult.HEADS) heads++;
        }

        if (heads > 0) {
            List<DamageModifier> modifiers = new java.util.ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new java.util.ArrayList<>());
            modifiers.add(new DamageModifier("rock-blast-heads", heads * 50, true));
            ctx.setModifiers(modifiers);
        }
    }

    /**
     * Rock Wrecker: ignore Weakness and Resistance; block Rhyperior from
     * using Rock Wrecker again next turn by setting its blockedAttackName.
     */
    private void applyRockWrecker(AttackContext ctx) {
        ctx.setIgnoreDefenderEffects(true);

        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState attacker  = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon rhyperior = attacker.getActivePokemon();

        if (rhyperior == null) return;

        rhyperior.setBlockedAttackName(ROCK_WRECKER);
    }

    private boolean isFightingEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.isBasicEnergy()
                        && card.getTypes() != null
                        && card.getTypes().contains(FIGHTING_TYPE))
                .orElse(false);
    }
}