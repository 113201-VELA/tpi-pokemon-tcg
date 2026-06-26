package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.card.TypeModifier;
import com.pokemon.tcg.domain.model.game.ActivePokemon;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DamageCalculator {

    /**
     * Calculates the final damage dealt to the defending Pokémon.
     *
     * <p>Order of operations (per official rulebook):
     * <ol>
     *   <li>Base damage.</li>
     *   <li>Pre-weakness modifiers (e.g. Muscle Band).</li>
     *   <li>Weakness of the defender (×2 if attacker type matches).</li>
     *   <li>Resistance of the defender (−20, minimum 0).</li>
     *   <li>Post-weakness modifiers (e.g. Hard Charm).</li>
     * </ol>
     *
     * <p>If base damage is 0, returns 0 immediately without applying
     * weakness, resistance, or modifiers.
     */
    /**
     * Calculates damage with no active stadium card.
     */
    public int calculate(ActivePokemon attacker, ActivePokemon defender,
                         int baseDamage, List<DamageModifier> modifiers) {
        return calculate(attacker, defender, baseDamage, modifiers, null);
    }

    /**
     * Calculates the final damage dealt to the defending Pokémon,
     * accounting for the active stadium card.
     */
    public int calculate(ActivePokemon attacker, ActivePokemon defender,
                         int baseDamage, List<DamageModifier> modifiers,
                         String activeStadiumCardId) {
        if (baseDamage == 0) return 0;

        int damage = baseDamage;

        // Step 2 — pre-weakness modifiers
        damage = applyModifiers(damage, modifiers, true);

        // Step 3 — weakness (×2 if attacker type matches defender's weakness)
        // Skipped when the active stadium card suppresses it (e.g. Shadow Circle).
        if (!isWeaknessSuppressed(activeStadiumCardId, defender)) {
            damage = applyWeakness(damage, attacker, defender);
        }

        // Step 4 — resistance (−20, minimum 0 after resistance)
        damage = applyResistance(damage, attacker, defender);

        // Step 5 — post-weakness modifiers
        damage = applyModifiers(damage, modifiers, false);

        return Math.max(0, damage);
    }

    public int toCounters(int damage) {
        return damage / 10;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private int applyModifiers(int damage, List<DamageModifier> modifiers,
                               boolean beforeWeakness) {
        if (modifiers == null || modifiers.isEmpty()) return damage;
        for (DamageModifier mod : modifiers) {
            if (mod.beforeWeakness() == beforeWeakness) {
                damage += mod.amount();
            }
        }
        return damage;
    }

    /**
     * Applies weakness if the attacker's first type matches the defender's weakness type.
     * Weakness multiplier is always ×2 in XY rules.
     */
    private int applyWeakness(int damage, ActivePokemon attacker, ActivePokemon defender) {
        if (defender.getWeaknesses() == null || defender.getWeaknesses().isEmpty()) return damage;
        EnergyType attackerType = getAttackerType(attacker);
        if (attackerType == null) return damage;
        for (TypeModifier weakness : defender.getWeaknesses()) {
            if (attackerType == weakness.getType()) {
                return damage * 2;
            }
        }
        return damage;
    }

    /**
     * Applies resistance if the attacker's first type matches the defender's resistance type.
     * Resistance reduction is always −20 in XY rules. Result cannot go below 0.
     */
    private int applyResistance(int damage, ActivePokemon attacker, ActivePokemon defender) {
        if (defender.getResistances() == null || defender.getResistances().isEmpty()) return damage;
        EnergyType attackerType = getAttackerType(attacker);
        if (attackerType == null) return damage;
        for (TypeModifier resistance : defender.getResistances()) {
            if (attackerType == resistance.getType()) {
                return Math.max(0, damage - 20);
            }
        }
        return damage;
    }

    /**
     * Checks whether the active stadium card suppresses weakness for the defender.
     *
     * <p>Shadow Circle (xy1-126): Each player's Darkness Pokémon have no weakness.
     */
    private boolean isWeaknessSuppressed(String activeStadiumCardId, ActivePokemon defender) {
        if (activeStadiumCardId == null) return false;
        if (!"shadow circle".equals(activeStadiumCardId)) return false;
        return defenderHasType(defender, EnergyType.DARKNESS);
    }

    /**
     * Returns true if the Pokémon has the given energy type in its types list.
     */
    private boolean defenderHasType(ActivePokemon defender, EnergyType type) {
        if (defender.getTypes() == null || defender.getTypes().isEmpty()) return false;
        return defender.getTypes().contains(type);
    }

    /**
     * Resolves the primary energy type of the attacker from its types field,
     * which is populated by the engine before the pipeline runs.
     */
    private EnergyType getAttackerType(ActivePokemon attacker) {
        if (attacker.getTypes() == null || attacker.getTypes().isEmpty()) return null;
        return attacker.getTypes().get(0);
    }
}
