package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-78 Yveltal
 *
 * Oblivion Wing: attach a Darkness Energy card from your discard pile to
 *                1 of your Benched Pokémon.
 * Darkness Blade: flip a coin. If tails, Yveltal can't attack during your
 *                 next turn.
 */
@Component
public class YveltalEffect implements AttackEffect {

    private static final String DARKNESS_TYPE   = EnergyType.DARKNESS.name();
    private static final String OBLIVION_WING   = "oblivion wing";
    private static final String DARKNESS_BLADE  = "darkness blade";

    private final CoinFlipService coinFlipService;
    private final CardLookupPort  cardLookupPort;

    public YveltalEffect(CoinFlipService coinFlipService, CardLookupPort cardLookupPort) {
        this.coinFlipService = coinFlipService;
        this.cardLookupPort  = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("yveltal|oblivion wing", "yveltal|darkness blade");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case OBLIVION_WING  -> applyOblivionWing(ctx);
            case DARKNESS_BLADE -> applyDarknessBlade(ctx);
            default              -> { }
        }
    }

    /**
     * Oblivion Wing: attach a Darkness Energy card from your discard pile
     * to 1 of your Benched Pokémon.
     * <p>
     * Payload: {@code energyCardId} (must be in the attacker's discard and
     * be a Darkness Energy), {@code targetInstanceId} (must be on the
     * attacker's own bench). Both are visible/own information at the time
     * DECLARE_ATTACK is sent, so no pending selection is needed.
     */
    private void applyOblivionWing(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);

        String energyCardId     = ctx.getAction().getPayloadString("energyCardId");
        String targetInstanceId = ctx.getAction().getPayloadString("targetInstanceId");
        if (energyCardId == null || targetInstanceId == null) return;

        List<String> discard = attacker.getDiscard();
        if (discard == null || !discard.contains(energyCardId)) return;
        if (!isDarknessEnergy(energyCardId)) return;

        BenchPokemon target = attacker.getBench() != null
                ? attacker.getBench().stream()
                  .filter(b -> b.getInstanceId().equals(targetInstanceId))
                  .findFirst().orElse(null)
                : null;
        if (target == null) return;

        List<String> newDiscard = new ArrayList<>(discard);
        newDiscard.remove(energyCardId);
        attacker.setDiscard(newDiscard);

        List<String> energies = new ArrayList<>(
                target.getAttachedEnergyIds() != null
                        ? target.getAttachedEnergyIds() : new ArrayList<>());
        energies.add(energyCardId);
        target.setAttachedEnergyIds(energies);
    }

    /**
     * Darkness Blade: flip a coin. If tails, Yveltal can't attack during
     * your next turn.
     * <p>
     * blockedAttackUntilTurn = current turnNumber + 2 — same fixed
     * mechanism as Rhyperior's Rock Wrecker / Aegislash's King's Shield:
     * the block must survive the opponent's turn and cover Yveltal's own
     * next turn, expiring only once turnNumber reaches turnNumber + 2.
     * Checked by RuleValidator.validateAttack; no longer cleared by
     * TurnManager.clearActiveEffects.
     */
    private void applyDarknessBlade(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon yveltal = attacker.getActivePokemon();

        if (yveltal == null) return;

        if (coinFlipService.flipAndEmit(ctx, attackerId) != CoinResult.TAILS) return;

        yveltal.setBlockedAttackName(DARKNESS_BLADE);
        yveltal.setBlockedAttackUntilTurn(ctx.getBoardState().getTurnNumber() + 2);
    }

    private boolean isDarknessEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.isBasicEnergy()
                        && card.getTypes() != null
                        && card.getTypes().contains(DARKNESS_TYPE))
                .orElse(false);
    }
}