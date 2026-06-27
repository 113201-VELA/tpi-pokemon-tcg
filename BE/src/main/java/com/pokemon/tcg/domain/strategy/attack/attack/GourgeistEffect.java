package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * xy1-57 Gourgeist
 *
 * Eerie Voice: Put 2 damage counters on each of your opponent's Pokémon.
 * Spirit Scream: Put damage counters on both Active Pokémon until each has 10 HP remaining.
 */
@Component
public class GourgeistEffect implements AttackEffect {

    private static final String EERIE_VOICE   = "eerie voice";
    private static final String SPIRIT_SCREAM = "spirit scream";

    private final CardLookupPort cardLookupPort;

    public GourgeistEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("gourgeist|eerie voice", "gourgeist|spirit scream");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case EERIE_VOICE   -> applyEerieVoice(ctx);
            case SPIRIT_SCREAM -> applySpiritScream(ctx);
            default            -> { }
        }
    }

    /**
     * Eerie Voice: put 2 damage counters on each of the opponent's Pokémon
     * (Active and all Bench Pokémon). These counters are placed directly and
     * are not affected by Weakness, Resistance, or other modifiers.
     */
    private void applyEerieVoice(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        ActivePokemon active = opponent.getActivePokemon();
        if (active != null) {
            active.setDamageCounters(active.getDamageCounters() + 2);
        }

        List<BenchPokemon> bench = opponent.getBench();
        if (bench != null) {
            for (BenchPokemon bp : bench) {
                bp.setDamageCounters(bp.getDamageCounters() + 2);
            }
        }
    }

    /**
     * Spirit Scream: put damage counters on both Active Pokémon until each
     * has exactly 10 HP remaining. Counters are placed directly — not
     * affected by Weakness or Resistance.
     */
    private void applySpiritScream(AttackContext ctx) {
        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState attacker  = ctx.getBoardState().getStateFor(attackerId);
        PlayerState opponent  = ctx.getBoardState().getOpponentState(attackerId);

        ActivePokemon attackerPokemon = attacker.getActivePokemon();
        ActivePokemon defenderPokemon = opponent.getActivePokemon();

        // Defender
        if (defenderPokemon != null) {
            int defenderMaxHp     = ctx.getDefenderMaxHp();
            int defenderTargetHp  = 10;
            int defenderCurrentHp = defenderMaxHp - defenderPokemon.getDamageCounters() * 10;
            int defenderDamageNeeded = defenderCurrentHp - defenderTargetHp;
            if (defenderDamageNeeded > 0) {
                int countersToAdd = defenderDamageNeeded / 10;
                defenderPokemon.setDamageCounters(
                        defenderPokemon.getDamageCounters() + countersToAdd);
            }
        }

        // Attacker
        if (attackerPokemon != null) {
            int attackerMaxHp     = cardLookupPort.getMaxHp(attackerPokemon.getCardId());
            int attackerTargetHp  = 10;
            int attackerCurrentHp = attackerMaxHp - attackerPokemon.getDamageCounters() * 10;
            int attackerDamageNeeded = attackerCurrentHp - attackerTargetHp;
            if (attackerDamageNeeded > 0) {
                int countersToAdd = attackerDamageNeeded / 10;
                attackerPokemon.setDamageCounters(
                        attackerPokemon.getDamageCounters() + countersToAdd);
            }
        }
    }
}