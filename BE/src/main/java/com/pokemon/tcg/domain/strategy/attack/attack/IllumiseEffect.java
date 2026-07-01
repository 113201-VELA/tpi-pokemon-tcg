package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IllumiseEffect implements AttackEffect {

    private static final String PHEROMATION  = "pheromation";
    private static final String QUICK_ATTACK = "quick attack";
    private static final String ATTACK_KEY   = "illumise|pheromation";
    private static final String GRASS_TYPE   = "GRASS";

    private final CardLookupPort  cardLookupPort;
    private final CoinFlipService coinFlipService;

    public IllumiseEffect(CardLookupPort cardLookupPort,
                          CoinFlipService coinFlipService) {
        this.cardLookupPort  = cardLookupPort;
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("illumise|pheromation", "illumise|quick attack");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case PHEROMATION  -> applyPheromation(ctx);
            case QUICK_ATTACK -> applyQuickAttack(ctx);
            default           -> { }
        }
    }

    private void applyPheromation(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState ps    = ctx.getBoardState().getStateFor(attackerId);

        List<String> deck = ps.getDeck() != null ? ps.getDeck() : List.of();

        List<String> grassPokemonIds = deck.stream()
                .filter(this::isGrassPokemon)
                .toList();

        if (grassPokemonIds.isEmpty()) return;

        ctx.setBoardState(ctx.getBoardState().toBuilder()
                .pendingAttackSelectionKey(ATTACK_KEY)
                .pendingAttackSelectionPlayerId(attackerId)
                .pendingDeckSelectionCardIds(new ArrayList<>(grassPokemonIds))
                .build());
    }

    private void applyQuickAttack(AttackContext ctx) {
        if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) == CoinResult.HEADS) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier("quick-attack-heads", 20, true));
            ctx.setModifiers(modifiers);
        }
    }

    private boolean isGrassPokemon(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getSupertype() == CardType.POKEMON
                        && card.getTypes() != null
                        && card.getTypes().contains(GRASS_TYPE))
                .orElse(false);
    }
}
