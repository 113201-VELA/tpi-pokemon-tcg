package com.pokemon.tcg.domain.strategy.ability.ability;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.strategy.ability.PassiveAbilityEffect;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

/**
 * Slurpuff — Sweet Veil (Passive Ability).
 *
 * <p>Card text: "Each of your Pokémon that has any Fairy Energy attached to
 * it can't be affected by any Special Conditions. (Remove any Special
 * Conditions affecting those Pokémon.)"
 *
 * <p><b>KNOWN LIMITATION (documented, agreed scope for this session):</b>
 * {@link PassiveAbilityEffect#onDamageReceived} only fires for the Pokémon
 * that owns the ability when <em>it</em> is the one taking damage — there is
 * no engine hook today for a continuous, team-wide condition immunity that
 * also covers Benched Pokémon or damage-less status attacks. A full
 * implementation would require refactoring every {@code AttackEffect} that
 * inflicts Special Conditions to go through a centralized point (e.g.
 * {@code StatusEffectManager.applyCondition}), which none of them currently
 * use — {@code StatusEffectManager.applyCondition} is presently only
 * exercised by tests, not by production attack effects.
 *
 * <p>What IS implemented: whenever Slurpuff itself takes damage from an
 * attack and has a Fairy Energy attached, any Special Conditions already on
 * it (including ones just inflicted by that same attack, since attack
 * effects like a coin-flip Paralyze run before this hook in the pipeline)
 * are cleared. This covers the common case of "this attack also inflicts a
 * Special Condition on Slurpuff" but NOT Benched Pokémon or condition-only
 * attacks with 0 damage.
 */
@Component
public class SlurpuffAbility implements PassiveAbilityEffect {

    private static final String FAIRY_TYPE = EnergyType.FAIRY.name();

    private final CardLookupPort cardLookupPort;

    public SlurpuffAbility(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public String getIdentifier() {
        return "slurpuff";
    }

    @Override
    public void onDamageReceived(AttackContext ctx, ActivePokemon defender) {
        if (ctx.getDamageToApply() <= 0) return;
        if (defender == null) return;
        if (!hasFairyEnergyAttached(defender)) return;

        defender.getConditions().clear();
    }

    private boolean hasFairyEnergyAttached(ActivePokemon pokemon) {
        if (pokemon.getAttachedEnergyIds() == null) return false;
        return pokemon.getAttachedEnergyIds().stream().anyMatch(this::isFairyEnergy);
    }

    private boolean isFairyEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getTypes() != null && card.getTypes().contains(FAIRY_TYPE))
                .orElse(false);
    }
}