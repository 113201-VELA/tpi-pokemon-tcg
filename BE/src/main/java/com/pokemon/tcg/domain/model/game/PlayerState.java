package com.pokemon.tcg.domain.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerState {
    private String playerId;
    private List<String> hand;
    private List<String> deck;
    private List<String> discard;
    private List<String> prizes;
    private ActivePokemon activePokemon;
    private List<BenchPokemon> bench;
    /** Total number of mulligans declared by this player during setup. */
    @Builder.Default
    private int totalMulligans = 0;
    /** Net bonus draws this player can accept, based on opponent's total mulligans. */
    @Builder.Default
    private int mulliganBonusDraws = 0;
    /** True once the player has confirmed their setup is complete. */
    @Builder.Default
    private boolean setupConfirmed = false;


    @JsonIgnore
    public int getHandSize() {
        return hand != null ? hand.size() : 0;
    }

    @JsonIgnore
    public int getDeckSize() {
        return deck != null ? deck.size() : 0;
    }

    @JsonIgnore
    public int getPrizeCount() {
        return prizes != null ? prizes.size() : 0;
    }

    @JsonIgnore
    public boolean hasBenchSpace() {
        return bench != null && bench.size() < 5;
    }

    @JsonIgnore
    public boolean hasAnyPokemonInPlay() {
        return activePokemon != null || (bench != null && !bench.isEmpty());
    }

    /**
     * Finds the cardId of a Pokémon in play by its instanceId.
     */
    public Optional<String> findCardIdByInstanceId(String instanceId) {
        if (activePokemon != null && activePokemon.getInstanceId().equals(instanceId)) {
            return Optional.of(activePokemon.getCardId());
        }
        if (bench != null) {
            return bench.stream()
                    .filter(p -> p.getInstanceId().equals(instanceId))
                    .map(BenchPokemon::getCardId)
                    .findFirst();
        }
        return Optional.empty();
    }
}
