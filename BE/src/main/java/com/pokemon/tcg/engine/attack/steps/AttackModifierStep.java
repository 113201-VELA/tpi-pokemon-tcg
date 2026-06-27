package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Step 5 — Applies damage modifiers from Pokémon Tools attached to the attacker
 * or defender before passing control to the next step.
 *
 * <p>Currently supported tools:
 * <ul>
 *   <li>Muscle Band (xy1-121) — attacker deals +20 damage (before weakness).</li>
 *   <li>Hard Charm (xy1-119) — defender receives −20 damage (after weakness).</li>
 * </ul>
 *
 * <p>To add a new Tool modifier, register its card name and modifier below.
 */
@Component
public class AttackModifierStep implements AttackStep {

    private static final String MUSCLE_BAND = "muscle band";
    private static final String HARD_CHARM  = "hard charm";

    private final CardLookupPort cardLookupPort;

    public AttackModifierStep(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());

        String attackerId = ctx.getAction().getPlayerId();
        String defenderId = ctx.getBoardState().getOpponentState(attackerId).getPlayerId();

        ActivePokemon attacker = ctx.getBoardState().getStateFor(attackerId).getActivePokemon();
        ActivePokemon defender = ctx.getBoardState().getStateFor(defenderId).getActivePokemon();

        // Check attacker's Tool
        if (attacker != null && attacker.getAttachedToolId() != null) {
            resolveToolModifier(attacker.getAttachedToolId(), true, modifiers);
        }

        // Check defender's Tool
        if (defender != null && defender.getAttachedToolId() != null) {
            resolveToolModifier(defender.getAttachedToolId(), false, modifiers);
        }

        ctx.setModifiers(modifiers);
        chain.next(ctx);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Looks up the card name for the given tool ID and adds the corresponding
     * DamageModifier to the list if the tool is recognized.
     */
    private void resolveToolModifier(String toolCardId, boolean isAttacker,
                                     List<DamageModifier> modifiers) {
        cardLookupPort.findCardById(toolCardId).ifPresent(card -> {
            String cardName = card.getName().toLowerCase();
            if (isAttacker && MUSCLE_BAND.equals(cardName)) {
                modifiers.add(new DamageModifier("muscle-band", +20, true));
            } else if (!isAttacker && HARD_CHARM.equals(cardName)) {
                modifiers.add(new DamageModifier("hard-charm", -20, false));
            }
        });
    }
}
