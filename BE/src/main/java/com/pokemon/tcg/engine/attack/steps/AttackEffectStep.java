package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffectRegistry;
import com.pokemon.tcg.engine.attack.AttackStep;
import org.springframework.stereotype.Component;

@Component
public class AttackEffectStep implements AttackStep {

    private final AttackEffectRegistry attackEffectRegistry;
    private final CardLookupPort       cardLookupPort;

    public AttackEffectStep(AttackEffectRegistry attackEffectRegistry,
                            CardLookupPort cardLookupPort) {
        this.attackEffectRegistry = attackEffectRegistry;
        this.cardLookupPort       = cardLookupPort;
    }

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        if (ctx.getAttack() == null) {
            chain.next(ctx);
            return;
        }

        String attackerId = ctx.getAction().getPlayerId();
        String cardId = ctx.getBoardState()
                .getStateFor(attackerId)
                .getActivePokemon()
                .getCardId();

        cardLookupPort.findCardById(cardId).ifPresent(card -> {
            attackEffectRegistry
                    .findEffect(card.getName(), ctx.getAttackName())
                    .ifPresent(effect -> effect.apply(ctx));
        });

        chain.next(ctx);
    }
}
