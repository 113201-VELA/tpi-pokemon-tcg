package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SkiddoEffect implements AttackEffect {

    private static final String ATTACK_KEY = "skiddo|lead";
    private static final String SUPPORTER  = "Supporter";

    private final CoinFlipService coinFlipService;
    private final CardLookupPort  cardLookupPort;

    public SkiddoEffect(CoinFlipService coinFlipService,
                        CardLookupPort cardLookupPort) {
        this.coinFlipService = coinFlipService;
        this.cardLookupPort  = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("skiddo|lead");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (coinFlipService.flip() != CoinResult.HEADS) return;

        String attackerId = ctx.getAction().getPlayerId();
        PlayerState ps    = ctx.getBoardState().getStateFor(attackerId);

        List<String> deck = ps.getDeck() != null ? ps.getDeck() : List.of();

        List<String> supporterIds = deck.stream()
                .filter(this::isSupporter)
                .toList();

        if (supporterIds.isEmpty()) return;

        ctx.setBoardState(ctx.getBoardState().toBuilder()
                .pendingAttackSelectionKey(ATTACK_KEY)
                .pendingAttackSelectionPlayerId(attackerId)
                .pendingDeckSelectionCardIds(new ArrayList<>(supporterIds))
                .build());
    }

    private boolean isSupporter(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getSubtypes() != null
                        && card.getSubtypes().contains(SUPPORTER))
                .orElse(false);
    }
}
