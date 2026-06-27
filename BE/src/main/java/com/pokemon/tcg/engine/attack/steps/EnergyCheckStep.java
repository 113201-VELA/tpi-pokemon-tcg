package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackStep;
import com.pokemon.tcg.domain.model.card.Attack;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Step 1 — Verifies the attacking Pokémon has enough energy attached
 * to perform the declared attack. Cancels the attack if not.
 *
 * <p>Energy matching algorithm:
 * <ol>
 *   <li>Resolve each attached energy card to its provided types using the card cache.</li>
 *   <li>Satisfy typed requirements (non-COLORLESS) first, preferring exact type matches
 *       over Rainbow Energy wildcards.</li>
 *   <li>Satisfy COLORLESS requirements with any remaining energy.
 *       Double Colorless Energy satisfies 2 COLORLESS costs with one card.</li>
 *   <li>Cancel the attack if any requirement remains unsatisfied.</li>
 * </ol>
 *
 * <p>Special energy cards handled:
 * <ul>
 *   <li>Double Colorless Energy (xy1-130) — provides 2 COLORLESS.</li>
 *   <li>Rainbow Energy (xy1-131) — provides 1 of any type (wildcard).</li>
 * </ul>
 */
@Component
public class EnergyCheckStep implements AttackStep {

    private static final String DOUBLE_COLORLESS = "double colorless energy";
    private static final String RAINBOW_ENERGY   = "rainbow energy";

    private final CardLookupPort cardLookupPort;

    public EnergyCheckStep(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        Attack attack = ctx.getAttack();
        ActivePokemon attacker = ctx.getBoardState()
                .getStateFor(ctx.getAction().getPlayerId())
                .getActivePokemon();

        if (attack == null || attacker == null) {
            ctx.cancel("No attack or attacker found.");
            return;
        }

        if (attack.getCost() == null || attack.getCost().isEmpty()) {
            chain.next(ctx);
            return;
        }

        List<String> attachedIds = attacker.getAttachedEnergyIds() != null
                ? new ArrayList<>(attacker.getAttachedEnergyIds())
                : new ArrayList<>();

        if (!canSatisfyCost(attack.getCost(), attachedIds)) {
            ctx.cancel("Not enough energy to use " + attack.getName() + ".");
            return;
        }

        chain.next(ctx);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Returns true if the attached energies can satisfy the full attack cost.
     *
     * <p>Uses a two-pass approach: first satisfies typed requirements using
     * exact matches, then uses Rainbow Energy wildcards for any remaining
     * typed requirements, then satisfies COLORLESS costs with what is left.
     */
    private boolean canSatisfyCost(List<EnergyType> cost, List<String> attachedIds) {
        List<EnergySlot> slots = resolveSlots(attachedIds);

        List<EnergyType> typed    = new ArrayList<>();
        List<EnergyType> colorless = new ArrayList<>();

        for (EnergyType e : cost) {
            if (e == EnergyType.COLORLESS) colorless.add(e);
            else typed.add(e);
        }

        // Pass 1 — satisfy typed requirements with exact type matches
        for (EnergyType required : new ArrayList<>(typed)) {
            boolean satisfied = consumeExact(slots, required);
            if (satisfied) typed.remove(required);
        }

        // Pass 2 — satisfy remaining typed requirements with Rainbow Energy wildcards
        for (EnergyType required : new ArrayList<>(typed)) {
            boolean satisfied = consumeWildcard(slots);
            if (satisfied) typed.remove(required);
        }

        if (!typed.isEmpty()) return false;

        // Pass 3 — satisfy COLORLESS requirements with any remaining slot
        for (EnergyType ignored : colorless) {
            if (!consumeAny(slots)) return false;
        }

        return true;
    }

    /**
     * Resolves each attached energy card ID into one or more {@link EnergySlot}s.
     *
     * <p>Normal Basic Energy → 1 slot of its type.
     * Double Colorless Energy → 2 COLORLESS slots.
     * Rainbow Energy → 1 wildcard slot.
     * Unknown cards → 1 COLORLESS slot (safe fallback).
     */
    private List<EnergySlot> resolveSlots(List<String> attachedIds) {
        List<EnergySlot> slots = new ArrayList<>();
        for (String cardId : attachedIds) {
            Optional<Card> cardOpt = cardLookupPort.findCardById(cardId);
            if (cardOpt.isEmpty()) {
                slots.add(EnergySlot.colorless());
                continue;
            }
            Card card = cardOpt.get();
            String name = card.getName() != null ? card.getName().toLowerCase() : "";

            if (DOUBLE_COLORLESS.equals(name)) {
                slots.add(EnergySlot.colorless());
                slots.add(EnergySlot.colorless());
            } else if (RAINBOW_ENERGY.equals(name)) {
                slots.add(EnergySlot.wildcard());
            } else {
                EnergyType type = resolveEnergyType(card);
                slots.add(EnergySlot.typed(type));
            }
        }
        return slots;
    }

    /** Resolves the primary energy type from a card's types list. Falls back to COLORLESS. */
    private EnergyType resolveEnergyType(Card card) {
        if (card.getTypes() == null || card.getTypes().isEmpty()) return EnergyType.COLORLESS;
        try {
            return EnergyType.valueOf(card.getTypes().get(0));
        } catch (IllegalArgumentException e) {
            return EnergyType.COLORLESS;
        }
    }

    /** Consumes one unused slot that exactly matches the required type. */
    private boolean consumeExact(List<EnergySlot> slots, EnergyType required) {
        for (EnergySlot slot : slots) {
            if (!slot.used && slot.type == required) {
                slot.used = true;
                return true;
            }
        }
        return false;
    }

    /** Consumes one unused wildcard slot (Rainbow Energy). */
    private boolean consumeWildcard(List<EnergySlot> slots) {
        for (EnergySlot slot : slots) {
            if (!slot.used && slot.wildcard) {
                slot.used = true;
                return true;
            }
        }
        return false;
    }

    /** Consumes any unused slot regardless of type (for COLORLESS requirements). */
    private boolean consumeAny(List<EnergySlot> slots) {
        for (EnergySlot slot : slots) {
            if (!slot.used) {
                slot.used = true;
                return true;
            }
        }
        return false;
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    /**
     * Represents one unit of energy available from an attached card.
     * Mutable — marked as used once consumed during matching.
     */
    private static class EnergySlot {
        EnergyType type;
        boolean wildcard;
        boolean used;

        private EnergySlot(EnergyType type, boolean wildcard) {
            this.type     = type;
            this.wildcard = wildcard;
            this.used     = false;
        }

        static EnergySlot typed(EnergyType type) { return new EnergySlot(type, false); }
        static EnergySlot colorless()             { return new EnergySlot(EnergyType.COLORLESS, false); }
        static EnergySlot wildcard()              { return new EnergySlot(null, true); }
    }
}
