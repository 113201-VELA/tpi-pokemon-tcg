# Backend — Scaffolding

> Clases base con firmas, anotaciones y relaciones. Sin lógica de negocio implementada.
> Referencia: `backend.md`, `contexto-general.md`

---

## Enums de dominio

```java
// domain/model/card/CardType.java
public enum CardType {
    POKEMON, ENERGY, TRAINER
}

// domain/model/card/PokemonPhase.java
public enum PokemonPhase {
    BASIC, STAGE_1, STAGE_2, MEGA, RESTORED
}

// domain/model/card/TrainerSubtype.java
public enum TrainerSubtype {
    ITEM, SUPPORTER, STADIUM, TOOL
}

// domain/model/card/EnergyType.java
public enum EnergyType {
    GRASS, FIRE, WATER, LIGHTNING, PSYCHIC,
    FIGHTING, DARKNESS, METAL, FAIRY, DRAGON, COLORLESS
}

// domain/model/game/GameState.java
public enum GameState {
    WAITING, SETUP, ACTIVE, FINISHED
}

// domain/model/game/TurnPhase.java
public enum TurnPhase {
    DRAW, MAIN, ATTACK, BETWEEN_TURNS
}

// domain/model/game/SpecialCondition.java
public enum SpecialCondition {
    ASLEEP, BURNED, CONFUSED, PARALYZED, POISONED
}

// domain/model/game/FinishReason.java
public enum FinishReason {
    PRIZES, KNOCKOUT, DECK_OUT, SUDDEN_DEATH
}

// domain/model/game/CoinResult.java
public enum CoinResult {
    HEADS, TAILS
}

// domain/model/game/GameActionType.java
public enum GameActionType {
    JOIN_GAME,
    DRAW_CARD,
    PLACE_BASIC_POKEMON,
    ATTACH_ENERGY,
    PLAY_TRAINER,
    EVOLVE_POKEMON,
    RETREAT,
    USE_ABILITY,
    DECLARE_ATTACK,
    END_TURN,
    CHOOSE_BENCH_POKEMON,
    MULLIGAN_CONFIRM,
    SETUP_PLACE_ACTIVE,
    SETUP_PLACE_BENCH
}

// domain/model/game/GameEventType.java
public enum GameEventType {
    GAME_STARTED,
    TURN_STARTED,
    CARD_DRAWN,
    POKEMON_PLACED,
    ENERGY_ATTACHED,
    TRAINER_PLAYED,
    POKEMON_EVOLVED,
    POKEMON_RETREATED,
    ABILITY_USED,
    ATTACK_DECLARED,
    DAMAGE_APPLIED,
    SPECIAL_CONDITION_APPLIED,
    SPECIAL_CONDITION_CLEARED,
    POKEMON_KNOCKED_OUT,
    PRIZE_TAKEN,
    TURN_ENDED,
    GAME_OVER,
    SUDDEN_DEATH_STARTED,
    MULLIGAN_DECLARED,
    RECONNECTED
}
```

---

## Modelos de dominio — cartas

```java
// domain/model/card/Attack.java
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Attack {
    private String name;
    private List<EnergyType> cost;
    private String damage;        // puede ser "80", "80+", "80×", etc.
    private String text;

    /** Retorna el daño base como entero. Retorna 0 si no aplica. */
    public int getBaseDamage() {
        // TODO: parsear this.damage extrayendo solo el número base
        return 0;
    }

    /** Indica si el daño tiene modificador variable (×, +, -). */
    public boolean hasVariableDamage() {
        // TODO
        return false;
    }
}

// domain/model/card/TypeModifier.java
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TypeModifier {
    private EnergyType type;
    private String value;    // ej: "×2" o "-20"
}

// domain/model/card/Ability.java
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Ability {
    private String name;
    private String text;
    private String type;    // "Ability", "Poké-Power", "Poké-Body"
}

// domain/model/card/Card.java
@Entity
@Table(name = "card_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    private String id;

    @Column(name = "set_id", nullable = false)
    private String setId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType supertype;

    @ElementCollection
    @CollectionTable(name = "card_subtypes", joinColumns = @JoinColumn(name = "card_id"))
    @Column(name = "subtype")
    private List<String> subtypes = new ArrayList<>();

    // Pokémon específico
    private Integer hp;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<EnergyType> types = new ArrayList<>();

    private String evolvesFrom;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<Attack> attacks = new ArrayList<>();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<TypeModifier> weaknesses = new ArrayList<>();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<TypeModifier> resistances = new ArrayList<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<EnergyType> retreatCost = new ArrayList<>();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<Ability> abilities = new ArrayList<>();

    // Energía específico
    @Column(nullable = false)
    private boolean isBasicEnergy = false;

    // Metadatos
    private String imageSmall;
    private String imageLarge;
    private String rarity;
    private String number;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private String rawData;

    @Column(nullable = false)
    private Instant cachedAt = Instant.now();

    // --- helpers ---

    public boolean isPokemon()  { return supertype == CardType.POKEMON; }
    public boolean isEnergy()   { return supertype == CardType.ENERGY; }
    public boolean isTrainer()  { return supertype == CardType.TRAINER; }

    public boolean isBasicPokemon() {
        // TODO: verificar supertype == POKEMON y subtypes incluye "Basic"
        return false;
    }

    public boolean isEX() {
        // TODO: verificar subtypes incluye "EX"
        return false;
    }

    public boolean isMega() {
        // TODO: verificar subtypes incluye "MEGA"
        return false;
    }

    public PokemonPhase getPhase() {
        // TODO: derivar la fase desde subtypes
        return null;
    }

    public TrainerSubtype getTrainerSubtype() {
        // TODO: derivar el subtipo de Entrenador desde subtypes
        return null;
    }
}
```

---

## Modelos de dominio — estado del juego

```java
// domain/model/game/TurnFlags.java
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TurnFlags {
    private boolean energyAttachedThisTurn;
    private boolean retreatedThisTurn;
    private boolean supporterPlayedThisTurn;
    private boolean stadiumPlayedThisTurn;
    private boolean attackedThisTurn;
    private boolean isFirstTurnOfGame;      // el jugador que empieza no roba ni ataca

    public static TurnFlags fresh() {
        return TurnFlags.builder().build();
    }
}

// domain/model/game/ActivePokemon.java
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivePokemon {
    private String instanceId;          // UUID único de esta instancia en juego
    private String cardId;
    private List<String> attachedEnergyIds;
    private String attachedToolId;
    private List<String> evolutionStack; // [básico, fase1, fase2] — el último es el activo
    private int damageCounters;
    private Set<SpecialCondition> conditions;
    private boolean enteredThisTurn;    // restricción de evolución

    public int getCurrentHp(int maxHp) {
        return Math.max(0, maxHp - damageCounters * 10);
    }

    public boolean isKnockedOut(int maxHp) {
        return damageCounters * 10 >= maxHp;
    }

    public boolean hasCondition(SpecialCondition condition) {
        return conditions != null && conditions.contains(condition);
    }

    public boolean canAttack() {
        return !hasCondition(SpecialCondition.ASLEEP)
            && !hasCondition(SpecialCondition.PARALYZED);
    }

    public boolean canRetreat() {
        return !hasCondition(SpecialCondition.ASLEEP)
            && !hasCondition(SpecialCondition.PARALYZED);
    }
}

// domain/model/game/BenchPokemon.java
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BenchPokemon {
    private String instanceId;
    private String cardId;
    private List<String> attachedEnergyIds;
    private String attachedToolId;
    private List<String> evolutionStack;
    private int damageCounters;
    private boolean enteredThisTurn;

    // Nota: los Pokémon en Banca NO tienen SpecialConditions activas.
    // Al pasar a Banca, todas las condiciones se eliminan.
}

// domain/model/game/PlayerState.java
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerState {
    private String playerId;
    private List<String> hand;          // IDs de cartas, orden importa
    private List<String> deck;          // IDs de cartas, orden importa
    private List<String> discard;
    private List<String> prizes;        // IDs ocultos hasta que se toman
    private ActivePokemon activePokemon;
    private List<BenchPokemon> bench;   // máximo 5

    public int getHandSize()  { return hand  != null ? hand.size()  : 0; }
    public int getDeckSize()  { return deck  != null ? deck.size()  : 0; }
    public int getPrizeCount(){ return prizes != null ? prizes.size() : 0; }
    public boolean hasBenchSpace() { return bench != null && bench.size() < 5; }
    public boolean hasAnyPokemonInPlay() {
        return activePokemon != null || (bench != null && !bench.isEmpty());
    }
}

// domain/model/game/BoardState.java
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardState {
    private String gameId;
    private GameState gameState;
    private TurnPhase turnPhase;
    private String currentPlayerId;
    private int turnNumber;
    private PlayerState player1State;
    private PlayerState player2State;
    private String activeStadiumCardId;
    private TurnFlags turnFlags;
    private List<GameEvent> pendingEvents;

    public PlayerState getStateFor(String playerId) {
        if (playerId.equals(player1State.getPlayerId())) return player1State;
        if (playerId.equals(player2State.getPlayerId())) return player2State;
        throw new IllegalArgumentException("Player not found: " + playerId);
    }

    public PlayerState getOpponentState(String playerId) {
        if (playerId.equals(player1State.getPlayerId())) return player2State;
        if (playerId.equals(player2State.getPlayerId())) return player1State;
        throw new IllegalArgumentException("Player not found: " + playerId);
    }
}

// domain/model/game/GameAction.java
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameAction {
    private GameActionType type;
    private String playerId;
    private Map<String, Object> payload;

    public String getPayloadString(String key) {
        return payload != null ? (String) payload.get(key) : null;
    }

    public Integer getPayloadInt(String key) {
        return payload != null ? (Integer) payload.get(key) : null;
    }
}

// domain/model/game/GameEvent.java
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameEvent {
    private GameEventType type;
    private String gameId;
    private String playerId;
    private int turnNumber;
    private Map<String, Object> data;
    private Instant occurredAt;
}

// domain/model/game/ValidationResult.java
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResult {
    private boolean valid;
    private String errorMessage;

    public static ValidationResult ok() {
        return ValidationResult.builder().valid(true).build();
    }

    public static ValidationResult fail(String message) {
        return ValidationResult.builder().valid(false).errorMessage(message).build();
    }
}

// domain/model/game/EngineResult.java
public record EngineResult(BoardState newState, List<GameEvent> events) {
    public static EngineResult of(BoardState state, List<GameEvent> events) {
        return new EngineResult(state, events);
    }
}
```

---

## Entidades JPA — jugadores, mazos y partidas

```java
// domain/model/player/Player.java
@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}

// domain/model/deck/Deck.java
@Entity
@Table(name = "decks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean isValid = false;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeckCard> cards = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public int getTotalCardCount() {
        return cards.stream().mapToInt(DeckCard::getQuantity).sum();
    }
}

// domain/model/deck/DeckCard.java
@Entity
@Table(name = "deck_cards",
       uniqueConstraints = @UniqueConstraint(columnNames = {"deck_id", "card_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeckCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(nullable = false)
    private int quantity;
}

// domain/model/game/Game.java
@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameState state = GameState.WAITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Player winner;

    @Enumerated(EnumType.STRING)
    private FinishReason finishReason;

    @Column(nullable = false)
    private boolean isSuddenDeath = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_game_id")
    private Game parentGame;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<GamePlayer> players = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant finishedAt;
}

// domain/model/game/GamePlayer.java
@Entity
@Table(name = "game_players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @Column(nullable = false)
    private int playerNumber;   // 1 o 2
}

// domain/model/game/GameStateSnapshot.java
@Entity
@Table(name = "game_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameStateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private int turnNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TurnPhase turnPhase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_player_id", nullable = false)
    private Player currentPlayer;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private BoardState boardState;  // serializado completo

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

// domain/model/game/GameLogEntry.java
@Entity
@Table(name = "game_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private int turnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(nullable = false, length = 50)
    private String actionType;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> actionData;

    @Column(nullable = false, length = 20)
    private String result;      // SUCCESS, FAILED, CANCELLED

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> resultData;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
```

---

## Repositorios

```java
// infrastructure/repository/CardRepository.java
public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findBySetId(String setId);

    Page<Card> findBySetIdAndNameContainingIgnoreCase(
        String setId, String name, Pageable pageable);

    Page<Card> findBySetIdAndSupertype(
        String setId, CardType supertype, Pageable pageable);

    boolean existsBySetId(String setId);

    long countBySetId(String setId);
}

// infrastructure/repository/DeckRepository.java
public interface DeckRepository extends JpaRepository<Deck, UUID> {

    List<Deck> findByPlayerIdOrderByCreatedAtDesc(UUID playerId);

    boolean existsByIdAndPlayerId(UUID deckId, UUID playerId);
}

// infrastructure/repository/GameRepository.java
public interface GameRepository extends JpaRepository<Game, UUID> {

    List<Game> findByStateOrderByCreatedAtDesc(GameState state);

    Optional<Game> findByIdAndState(UUID gameId, GameState state);
}

// infrastructure/repository/GameStateRepository.java
public interface GameStateRepository extends JpaRepository<GameStateSnapshot, UUID> {

    Optional<GameStateSnapshot> findTopByGameIdOrderByCreatedAtDesc(UUID gameId);

    List<GameStateSnapshot> findByGameIdOrderByCreatedAtAsc(UUID gameId);
}

// infrastructure/repository/GameLogRepository.java
public interface GameLogRepository extends JpaRepository<GameLogEntry, UUID> {

    List<GameLogEntry> findByGameIdOrderByCreatedAtAsc(UUID gameId);

    List<GameLogEntry> findByGameIdAndTurnNumberOrderByCreatedAtAsc(
        UUID gameId, int turnNumber);
}

// infrastructure/repository/PlayerRepository.java
public interface PlayerRepository extends JpaRepository<Player, UUID> {

    Optional<Player> findByUsername(String username);

    Optional<Player> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
```

---

## Cliente API externa — pokemontcg.io

```java
// infrastructure/cache/PokemonTcgApiClient.java
@Component
public class PokemonTcgApiClient {

    private static final String API_URL = "https://api.pokemontcg.io/v2/cards";

    private final RestTemplate restTemplate;

    public PokemonTcgApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Obtiene las cartas de un set desde la API de pokemontcg.io.
     */
    public List<Map<String, Object>> fetchCardsBySet(String setId) {
        // TODO: GET /v2/cards?q=set.id:{setId}&pageSize=250
        // TODO: mapear respuesta a entidades Card
        return List.of();
    }
}
```

---

## Game Engine — interfaces y clases base

```java
// domain/engine/GameEngineFacade.java
public interface GameEngineFacade {

    /**
     * Procesa una acción del jugador y retorna el nuevo estado + eventos ocurridos.
     * No tiene efectos secundarios fuera del BoardState retornado.
     */
    EngineResult processAction(BoardState currentState, GameAction action);

    /**
     * Inicializa el estado de la partida (fase SETUP).
     * Baraja mazos, gestiona mulligans, coloca cartas de Premio.
     */
    EngineResult initializeGame(String gameId, PlayerState player1, PlayerState player2);
}

// domain/engine/TurnManager.java
@Component
public class TurnManager {

    private final RuleValidator ruleValidator;

    public TurnManager(RuleValidator ruleValidator) {
        this.ruleValidator = ruleValidator;
    }

    /**
     * Avanza la fase del turno según la acción recibida.
     * Retorna el nuevo BoardState con la fase actualizada.
     */
    public BoardState advancePhase(BoardState state, GameAction action) {
        // TODO
        return state;
    }

    /**
     * Determina si la acción es válida en la fase actual.
     */
    public ValidationResult validateActionForPhase(BoardState state, GameAction action) {
        // TODO
        return ValidationResult.ok();
    }

    /**
     * Retorna las acciones disponibles para el jugador activo en la fase actual.
     */
    public Set<GameActionType> getAvailableActions(BoardState state) {
        // TODO
        return Set.of();
    }
}

// domain/engine/RuleValidator.java
@Component
public class RuleValidator {

    /**
     * Valida cualquier acción antes de ejecutarla.
     * Punto de entrada principal para validación.
     */
    public ValidationResult validate(BoardState state, GameAction action) {
        return switch (action.getType()) {
            case PLACE_BASIC_POKEMON -> validatePlaceBasicPokemon(state, action);
            case EVOLVE_POKEMON      -> validateEvolution(state, action);
            case ATTACH_ENERGY       -> validateAttachEnergy(state, action);
            case PLAY_TRAINER        -> validatePlayTrainer(state, action);
            case RETREAT             -> validateRetreat(state, action);
            case DECLARE_ATTACK      -> validateAttack(state, action);
            default                  -> ValidationResult.ok();
        };
    }

    private ValidationResult validatePlaceBasicPokemon(BoardState state, GameAction action) {
        // TODO: verificar que hay espacio en Banca (máx 5)
        return ValidationResult.ok();
    }

    private ValidationResult validateEvolution(BoardState state, GameAction action) {
        // TODO:
        // - No en el primer turno del jugador
        // - No en el primer turno en que el Pokémon entró en juego
        // - El Pokémon destino evoluciona del correcto (nombre exacto)
        // - El Pokémon no puede volver a evolucionar en el mismo turno
        return ValidationResult.ok();
    }

    private ValidationResult validateAttachEnergy(BoardState state, GameAction action) {
        // TODO: verificar que no se unió Energía este turno
        return ValidationResult.ok();
    }

    private ValidationResult validatePlayTrainer(BoardState state, GameAction action) {
        // TODO:
        // - Partidario: verificar que no se jugó uno este turno
        // - Estadio: verificar que no se jugó uno este turno
        return ValidationResult.ok();
    }

    private ValidationResult validateRetreat(BoardState state, GameAction action) {
        // TODO:
        // - No retirado este turno
        // - Pokémon no Dormido ni Paralizado
        // - Tiene suficiente Energía para el Coste de Retirada
        // - Hay al menos 1 Pokémon en Banca
        return ValidationResult.ok();
    }

    private ValidationResult validateAttack(BoardState state, GameAction action) {
        // TODO:
        // - No es el primer turno del jugador que empieza
        // - Tiene la Energía requerida para el ataque elegido
        // - El Pokémon puede atacar (no Dormido, no Paralizado)
        return ValidationResult.ok();
    }
}

// domain/engine/DamageCalculator.java
@Component
public class DamageCalculator {

    /**
     * Calcula el daño final aplicando modificadores en el orden del rulebook:
     * 1. Daño base
     * 2. Modificadores del atacante (pre-Debilidad)
     * 3. Debilidad del defensor (×2)
     * 4. Resistencia del defensor (-20, mínimo 0)
     * 5. Modificadores del defensor (post-Resistencia)
     *
     * @param attacker  Pokémon atacante
     * @param defender  Pokémon defensor
     * @param baseDamage  Daño base de la carta
     * @param modifiers   Modificadores activos (de Entrenadores u otros efectos)
     * @return Daño final como entero (mínimo 0)
     */
    public int calculate(ActivePokemon attacker, ActivePokemon defender,
                         int baseDamage, List<DamageModifier> modifiers) {
        if (baseDamage == 0) return 0;

        int damage = baseDamage;

        // TODO: paso 2 — aplicar modificadores del atacante
        // TODO: paso 3 — aplicar Debilidad
        // TODO: paso 4 — aplicar Resistencia (mínimo 0)
        // TODO: paso 5 — aplicar modificadores del defensor

        return Math.max(0, damage);
    }

    /**
     * Convierte puntos de daño a contadores (÷10).
     */
    public int toCounters(int damage) {
        return damage / 10;
    }
}

// domain/engine/DamageModifier.java  (usado por DamageCalculator)
public record DamageModifier(
    String source,          // descripción de origen (para log)
    int amount,             // puede ser negativo
    boolean beforeWeakness  // true = se aplica antes de Debilidad/Resistencia
) {}

// domain/engine/StatusEffectManager.java
@Component
public class StatusEffectManager {

    private final CoinFlipService coinFlipService;

    public StatusEffectManager(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    /**
     * Procesa todas las condiciones especiales de un Pokémon en el orden fijo del rulebook:
     * Envenenado → Quemado → Dormido → Paralizado
     *
     * @return El Pokémon con contadores y condiciones actualizados
     */
    public ActivePokemon processBetweenTurns(ActivePokemon pokemon) {
        // TODO: procesar en orden fijo
        return pokemon;
    }

    /**
     * Aplica una condición especial a un Pokémon.
     * Dormido, Confundido y Paralizado son mutuamente excluyentes.
     * Quemado y Envenenado son independientes y coexisten.
     */
    public ActivePokemon applyCondition(ActivePokemon pokemon, SpecialCondition condition) {
        // TODO: si la condición es exclusiva, eliminar la anterior
        return pokemon;
    }

    /**
     * Elimina todas las condiciones especiales (al pasar a Banca o evolucionar).
     */
    public ActivePokemon clearAllConditions(ActivePokemon pokemon) {
        // TODO
        return pokemon;
    }
}

// domain/engine/VictoryConditionChecker.java
@Component
public class VictoryConditionChecker {

    /**
     * Verifica si alguna condición de victoria/derrota se cumple.
     * Se llama después de cada acción relevante.
     *
     * @return Optional con el ID del ganador, o empty si la partida continúa.
     *         Si retorna un GameEvent de tipo SUDDEN_DEATH_STARTED, el caller
     *         debe iniciar una nueva partida con 1 Premio.
     */
    public Optional<GameEvent> check(BoardState state) {
        // TODO: verificar en orden:
        // 1. ¿Algún jugador tomó su última carta de Premio?
        // 2. ¿El jugador cuyo Pokémon fue KO no tiene Pokémon en Banca?
        // 3. ¿El mazo está vacío al intentar robar?
        // 4. ¿Ambas condiciones simultáneas? → Muerte Súbita
        return Optional.empty();
    }

    public boolean isKnockedOut(ActivePokemon pokemon, int maxHp) {
        return pokemon.getDamageCounters() * 10 >= maxHp;
    }
}

// domain/engine/SetupManager.java
@Component
public class SetupManager {

    private final CoinFlipService coinFlipService;

    public SetupManager(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    /**
     * Baraja el mazo y roba 7 cartas iniciales.
     * Retorna true si hay al menos 1 Pokémon Básico en la mano.
     */
    public boolean drawInitialHand(PlayerState playerState) {
        // TODO: barajar deck, robar 7, retornar si tiene Básico
        return false;
    }

    /**
     * Gestiona el flujo de mulligan para ambos jugadores.
     * Modifica los PlayerState en el BoardState dado.
     */
    public BoardState handleMulligan(BoardState state) {
        // TODO: implementar flujo completo de mulligan según rulebook
        return state;
    }

    /**
     * Separa las primeras 6 cartas del mazo como cartas de Premio.
     */
    public PlayerState setupPrizes(PlayerState playerState) {
        // TODO
        return playerState;
    }

    /**
     * Lanza moneda para determinar quién empieza.
     * @return ID del jugador que comienza
     */
    public String determineFirstPlayer(String player1Id, String player2Id) {
        // TODO: usar coinFlipService
        return player1Id;
    }
}

// domain/engine/CoinFlipService.java
@Component
public class CoinFlipService {

    private final Random random = new Random();

    public CoinResult flip() {
        return random.nextBoolean() ? CoinResult.HEADS : CoinResult.TAILS;
    }

    /** Para tests: permite inyectar resultados predeterminados. */
    public CoinResult flip(boolean forceHeads) {
        return forceHeads ? CoinResult.HEADS : CoinResult.TAILS;
    }
}
```

---

## Patrón State — estados de partida

```java
// domain/state/GameStateHandler.java
public interface GameStateHandler {

    /**
     * Procesa una acción en el estado actual de la partida.
     * Cada handler valida solo las acciones permitidas en su estado.
     */
    EngineResult handle(BoardState state, GameAction action);

    /** Retorna el estado de partida que este handler gestiona. */
    GameState getState();
}

// domain/state/WaitingState.java
@Component
public class WaitingState implements GameStateHandler {

    @Override
    public EngineResult handle(BoardState state, GameAction action) {
        if (action.getType() != GameActionType.JOIN_GAME) {
            return new EngineResult(state, List.of(
                GameEvent.builder()
                    .type(GameEventType.GAME_OVER)
                    .data(Map.of("error", "Acción no válida en estado WAITING"))
                    .build()
            ));
        }
        // TODO: lógica de unirse a la partida
        return EngineResult.of(state, List.of());
    }

    @Override
    public GameState getState() {
        return GameState.WAITING;
    }
}

// domain/state/SetupState.java
@Component
public class SetupState implements GameStateHandler {

    private final SetupManager setupManager;

    public SetupState(SetupManager setupManager) {
        this.setupManager = setupManager;
    }

    @Override
    public EngineResult handle(BoardState state, GameAction action) {
        return switch (action.getType()) {
            case MULLIGAN_CONFIRM -> {
                // TODO: confirmar mulligan y continuar
                yield EngineResult.of(state, List.of());
            }
            case SETUP_PLACE_ACTIVE -> {
                // TODO: colocar Pokémon Activo
                yield EngineResult.of(state, List.of());
            }
            case SETUP_PLACE_BENCH -> {
                // TODO: colocar Pokémon en Banca
                yield EngineResult.of(state, List.of());
            }
            default -> new EngineResult(state, List.of(
                GameEvent.builder()
                    .type(GameEventType.GAME_OVER)
                    .data(Map.of("error", "Acción no válida en estado SETUP"))
                    .build()
            ));
        };
    }

    @Override
    public GameState getState() {
        return GameState.SETUP;
    }
}

// domain/state/ActiveState.java
@Component
public class ActiveState implements GameStateHandler {

    private final TurnManager turnManager;
    private final AttackPipeline attackPipeline;

    public ActiveState(TurnManager turnManager, AttackPipeline attackPipeline) {
        this.turnManager = turnManager;
        this.attackPipeline = attackPipeline;
    }

    @Override
    public EngineResult handle(BoardState state, GameAction action) {
        // TODO: validar fase actual con TurnManager
        // Si es ataque, delegar en AttackPipeline
        // Si es otra acción, delegar en TurnManager
        return EngineResult.of(state, List.of());
    }

    @Override
    public GameState getState() {
        return GameState.ACTIVE;
    }
}

// domain/state/FinishedState.java
@Component
public class FinishedState implements GameStateHandler {

    @Override
    public EngineResult handle(BoardState state, GameAction action) {
        return new EngineResult(state, List.of(
            GameEvent.builder()
                .type(GameEventType.GAME_OVER)
                .data(Map.of("error", "La partida ya ha finalizado"))
                .build()
        ));
    }

    @Override
    public GameState getState() {
        return GameState.FINISHED;
    }
}
```

---

## Pipeline de ataque — Chain of Responsibility

```java
// domain/engine/attack/AttackContext.java
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttackContext {
    private BoardState boardState;
    private GameAction action;
    private String attackName;
    private Attack attack;
    private boolean cancelled;
    private String cancellationReason;
    private int damageToApply;
    private List<DamageModifier> modifiers;
    private List<GameEvent> events;

    public void cancel(String reason) {
        this.cancelled = true;
        this.cancellationReason = reason;
    }

    public void addEvent(GameEvent event) {
        if (events == null) events = new ArrayList<>();
        events.add(event);
    }
}

// domain/engine/attack/AttackStep.java
public interface AttackStep {
    void execute(AttackContext ctx, AttackChain chain);
}

// domain/engine/attack/AttackChain.java
public class AttackChain {

    private final List<AttackStep> steps;
    private int currentIndex = 0;

    public AttackChain(List<AttackStep> steps) {
        this.steps = steps;
    }

    public void next(AttackContext ctx) {
        if (!ctx.isCancelled() && currentIndex < steps.size()) {
            AttackStep step = steps.get(currentIndex++);
            step.execute(ctx, this);
        }
    }
}

// domain/engine/attack/AttackPipeline.java
@Component
public class AttackPipeline {

    private final EnergyCheckStep      energyCheckStep;
    private final ConfusionCheckStep   confusionCheckStep;
    private final SelectionStep        selectionStep;
    private final PreAttackStep        preAttackStep;
    private final AttackModifierStep   attackModifierStep;
    private final DamageApplicationStep damageApplicationStep;
    private final PostDamageEffectStep postDamageEffectStep;

    public AttackPipeline(EnergyCheckStep energyCheckStep,
                          ConfusionCheckStep confusionCheckStep,
                          SelectionStep selectionStep,
                          PreAttackStep preAttackStep,
                          AttackModifierStep attackModifierStep,
                          DamageApplicationStep damageApplicationStep,
                          PostDamageEffectStep postDamageEffectStep) {
        this.energyCheckStep       = energyCheckStep;
        this.confusionCheckStep    = confusionCheckStep;
        this.selectionStep         = selectionStep;
        this.preAttackStep         = preAttackStep;
        this.attackModifierStep    = attackModifierStep;
        this.damageApplicationStep = damageApplicationStep;
        this.postDamageEffectStep  = postDamageEffectStep;
    }

    public AttackContext execute(AttackContext ctx) {
        List<AttackStep> steps = List.of(
            energyCheckStep,
            confusionCheckStep,
            selectionStep,
            preAttackStep,
            attackModifierStep,
            damageApplicationStep,
            postDamageEffectStep
        );
        new AttackChain(steps).next(ctx);
        return ctx;
    }
}

// domain/engine/attack/steps/EnergyCheckStep.java
@Component
public class EnergyCheckStep implements AttackStep {

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        // TODO: verificar que el Pokémon Activo tiene la Energía requerida para el ataque
        // Si no tiene: ctx.cancel("No puedes atacar: te falta X Energía de Y")
        chain.next(ctx);
    }
}

// domain/engine/attack/steps/ConfusionCheckStep.java
@Component
public class ConfusionCheckStep implements AttackStep {

    private final CoinFlipService coinFlipService;

    public ConfusionCheckStep(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        // TODO: si el Pokémon Activo está Confundido, lanzar moneda
        // Cruz → ctx.cancel("Confusión: el ataque falló") + 3 contadores de daño al atacante
        // Cara → continuar
        chain.next(ctx);
    }
}

// domain/engine/attack/steps/SelectionStep.java
@Component
public class SelectionStep implements AttackStep {

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        // TODO: validar que el payload de la acción incluye las selecciones
        // requeridas por el ataque (ej: objetivo en Banca del rival)
        chain.next(ctx);
    }
}

// domain/engine/attack/steps/PreAttackStep.java
@Component
public class PreAttackStep implements AttackStep {

    private final CoinFlipService coinFlipService;

    public PreAttackStep(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        // TODO: ejecutar requisitos previos indicados en el texto de la carta
        // (ej: "Lanza 1 moneda. Si sale cruz, este ataque no hace nada.")
        chain.next(ctx);
    }
}

// domain/engine/attack/steps/AttackModifierStep.java
@Component
public class AttackModifierStep implements AttackStep {

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        // TODO: aplicar efectos que modifiquen o cancelen el ataque
        // (efectos del turno anterior del rival que afectan este ataque)
        chain.next(ctx);
    }
}

// domain/engine/attack/steps/DamageApplicationStep.java
@Component
public class DamageApplicationStep implements AttackStep {

    private final DamageCalculator damageCalculator;

    public DamageApplicationStep(DamageCalculator damageCalculator) {
        this.damageCalculator = damageCalculator;
    }

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        // TODO:
        // 1. Obtener daño base del ataque
        // 2. Llamar a damageCalculator.calculate(...)
        // 3. Colocar contadores de daño sobre el defensor
        // 4. Registrar evento DAMAGE_APPLIED
        chain.next(ctx);
    }
}

// domain/engine/attack/steps/PostDamageEffectStep.java
@Component
public class PostDamageEffectStep implements AttackStep {

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        // TODO: aplicar efectos posteriores al daño:
        // - Condiciones especiales infligidas
        // - Descartes de Energía
        // - Daño a Pokémon en Banca
        // - Curación
        chain.next(ctx);
    }
}
```

---

## Strategy — efectos de cartas de Entrenador

```java
// domain/strategy/TrainerEffect.java
public interface TrainerEffect {

    /**
     * Aplica el efecto de la carta de Entrenador al estado del juego.
     *
     * @param state   Estado actual del tablero
     * @param action  Acción con el payload de la carta jugada
     * @return Nuevo BoardState con el efecto aplicado + eventos generados
     */
    EngineResult apply(BoardState state, GameAction action);

    /**
     * Verifica si este efecto puede aplicarse en el estado actual.
     */
    ValidationResult canApply(BoardState state, GameAction action);
}

// domain/strategy/item/PotionEffect.java
@Component
public class PotionEffect implements TrainerEffect {

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        // TODO: curar 30 puntos de daño a 1 Pokémon propio
        return EngineResult.of(state, List.of());
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        // TODO: verificar que hay un Pokémon con daño
        return ValidationResult.ok();
    }
}

// domain/strategy/item/SwitchEffect.java
@Component
public class SwitchEffect implements TrainerEffect {

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        // TODO: cambiar Pokémon Activo por uno de Banca sin pagar Coste de Retirada
        return EngineResult.of(state, List.of());
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        // TODO: verificar que hay Pokémon en Banca
        return ValidationResult.ok();
    }
}

// domain/strategy/supporter/ProfessorSycamoreEffect.java
@Component
public class ProfessorSycamoreEffect implements TrainerEffect {

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        // TODO: descartar la mano completa y robar 7 cartas
        return EngineResult.of(state, List.of());
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }
}

// domain/strategy/stadium/StadiumEffect.java
@Component
public class StadiumEffect implements TrainerEffect {

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        // TODO: aplicar efecto de Estadio (permanece en juego hasta ser reemplazado)
        return EngineResult.of(state, List.of());
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        // TODO: verificar que no se jugó un Estadio este turno
        return ValidationResult.ok();
    }
}
```

---

## Servicios de aplicación

```java
// application/GameService.java
@Service
@Transactional
public class GameService {

    private final GameEngineFacade engine;
    private final GameRepository gameRepository;
    private final GameStateRepository stateRepository;
    private final GameLogRepository logRepository;
    private final GameEventPublisher eventPublisher;
    private final CardRepository cardRepository;

    public GameService(GameEngineFacade engine,
                       GameRepository gameRepository,
                       GameStateRepository stateRepository,
                       GameLogRepository logRepository,
                       GameEventPublisher eventPublisher,
                       CardRepository cardRepository) {
        this.engine           = engine;
        this.gameRepository   = gameRepository;
        this.stateRepository  = stateRepository;
        this.logRepository    = logRepository;
        this.eventPublisher   = eventPublisher;
        this.cardRepository   = cardRepository;
    }

    public Game createGame(UUID playerId, UUID deckId) {
        // TODO: crear partida en estado WAITING, registrar player1
        return null;
    }

    public Game joinGame(UUID gameId, UUID playerId, UUID deckId) {
        // TODO: validar que la partida está WAITING, registrar player2,
        // pasar a estado SETUP e inicializar el BoardState
        return null;
    }

    public EngineResult processAction(UUID gameId, UUID playerId, GameAction action) {
        // TODO:
        // 1. Cargar el último BoardState desde la BD
        // 2. Verificar que es el turno del jugador
        // 3. Delegar al engine
        // 4. Persistir el nuevo estado
        // 5. Registrar en el log
        // 6. Publicar eventos por WebSocket
        // 7. Retornar el resultado
        return null;
    }

    public BoardState getCurrentState(UUID gameId) {
        // TODO: obtener el snapshot más reciente
        return null;
    }

    public List<GameLogEntry> getLog(UUID gameId) {
        return logRepository.findByGameIdOrderByCreatedAtAsc(gameId);
    }

    public List<Game> listOpenGames() {
        return gameRepository.findByStateOrderByCreatedAtDesc(GameState.WAITING);
    }
}

// application/DeckService.java
@Service
@Transactional
public class DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final PlayerRepository playerRepository;

    public DeckService(DeckRepository deckRepository,
                       CardRepository cardRepository,
                       PlayerRepository playerRepository) {
        this.deckRepository  = deckRepository;
        this.cardRepository  = cardRepository;
        this.playerRepository = playerRepository;
    }

    public Deck createDeck(UUID playerId, String name, String description) {
        // TODO
        return null;
    }

    public Deck addCard(UUID deckId, UUID playerId, String cardId, int quantity) {
        // TODO: verificar ownership, agregar carta, re-validar el mazo
        return null;
    }

    public DeckValidationResult validate(UUID deckId) {
        // TODO: aplicar todas las reglas de construcción de mazos
        return null;
    }

    public List<Deck> listByPlayer(UUID playerId) {
        return deckRepository.findByPlayerIdOrderByCreatedAtDesc(playerId);
    }

    public void deleteDeck(UUID deckId, UUID playerId) {
        // TODO: verificar ownership antes de eliminar
    }
}

// application/CardCacheService.java
@Service
public class CardCacheService {

    private final CardRepository cardRepository;
    private final PokemonTcgApiClient apiClient;

    public CardCacheService(CardRepository cardRepository,
                            PokemonTcgApiClient apiClient) {
        this.cardRepository = cardRepository;
        this.apiClient      = apiClient;
    }

    @PostConstruct
    public void initCache() {
        // TODO: si no hay cartas del set xy1, cargarlas desde la API
    }

    public void loadSet(String setId) {
        // TODO: llamar a la API, mapear respuesta, persistir con upsert
    }

    public Page<Card> searchCards(String setId, String name,
                                   CardType supertype, Pageable pageable) {
        // TODO: buscar en el caché local
        return Page.empty();
    }
}

// application/MatchmakingService.java
@Service
public class MatchmakingService {

    private final GameRepository gameRepository;
    private final GameService gameService;

    public MatchmakingService(GameRepository gameRepository,
                               GameService gameService) {
        this.gameRepository = gameRepository;
        this.gameService    = gameService;
    }

    /**
     * Busca una partida en estado WAITING para emparejar al jugador.
     * Si no hay, retorna null.
     */
    public Game findAvailableGame() {
        List<Game> openGames = gameRepository.findByStateOrderByCreatedAtDesc(GameState.WAITING);
        return openGames.isEmpty() ? null : openGames.get(0);
    }

    /**
     * Empareja a un jugador con una partida existente o crea una nueva.
     */
    public Game matchPlayer(UUID playerId, UUID deckId) {
        Game available = findAvailableGame();
        if (available != null) {
            return gameService.joinGame(available.getId(), playerId, deckId);
        }
        return gameService.createGame(playerId, deckId);
    }
}
```

---

## Controllers REST

```java
// api/rest/GameController.java
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public ResponseEntity<List<Game>> listOpenGames() {
        return ResponseEntity.ok(gameService.listOpenGames());
    }

    @PostMapping
    public ResponseEntity<Game> createGame(@RequestBody CreateGameRequest request) {
        // TODO: obtener playerId del contexto de seguridad
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<Game> joinGame(@PathVariable UUID gameId,
                                          @RequestBody JoinGameRequest request) {
        // TODO
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{gameId}/state")
    public ResponseEntity<BoardState> getState(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.getCurrentState(gameId));
    }

    @GetMapping("/{gameId}/log")
    public ResponseEntity<List<GameLogEntry>> getLog(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.getLog(gameId));
    }
}

// api/rest/DeckController.java
@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping
    public ResponseEntity<List<Deck>> listDecks() {
        // TODO: obtener playerId del contexto de seguridad
        return ResponseEntity.ok(List.of());
    }

    @PostMapping
    public ResponseEntity<Deck> createDeck(@RequestBody CreateDeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{deckId}")
    public ResponseEntity<Deck> updateDeck(@PathVariable UUID deckId,
                                            @RequestBody UpdateDeckRequest request) {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> deleteDeck(@PathVariable UUID deckId) {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{deckId}/validate")
    public ResponseEntity<DeckValidationResult> validate(@PathVariable UUID deckId) {
        return ResponseEntity.ok(deckService.validate(deckId));
    }
}

// api/rest/CardController.java
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardCacheService cardCacheService;

    public CardController(CardCacheService cardCacheService) {
        this.cardCacheService = cardCacheService;
    }

    @GetMapping
    public ResponseEntity<Page<Card>> searchCards(
            @RequestParam(defaultValue = "xy1") String set,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CardType type,
            Pageable pageable) {
        return ResponseEntity.ok(cardCacheService.searchCards(set, name, type, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Card> getCard(@PathVariable String id) {
        // TODO
        return ResponseEntity.notFound().build();
    }
}
```

---

## WebSocket — handler y publisher

```java
// api/websocket/GameActionHandler.java
@Controller
public class GameActionHandler {

    private final GameService gameService;

    public GameActionHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/game/{gameId}/action")
    public void handleAction(@DestinationVariable String gameId,
                              @Payload GameAction action,
                              Principal principal) {
        // TODO:
        // 1. Obtener UUID del jugador desde principal
        // 2. Llamar a gameService.processAction(...)
        // Los eventos ya se publican dentro de GameService
    }
}

// api/websocket/GameEventPublisher.java
public interface GameEventPublisher {

    /** Envía el estado público a ambos jugadores. */
    void publishBoardState(String gameId, BoardState state);

    /** Envía la mano y estado privado a un jugador específico. */
    void publishPrivateState(String gameId, String playerId, PlayerState playerState);

    /** Envía un evento a todos los suscriptores de la partida. */
    void publishEvent(String gameId, GameEvent event);

    /** Envía actualizaciones del lobby. */
    void publishLobbyUpdate(GameEvent event);
}

// infrastructure/websocket/StompEventPublisher.java
@Component
public class StompEventPublisher implements GameEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public StompEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishBoardState(String gameId, BoardState state) {
        messagingTemplate.convertAndSend(
            "/topic/game/" + gameId + "/state", state);
    }

    @Override
    public void publishPrivateState(String gameId, String playerId, PlayerState playerState) {
        messagingTemplate.convertAndSendToUser(
            playerId,
            "/queue/game/" + gameId + "/player",
            playerState);
    }

    @Override
    public void publishEvent(String gameId, GameEvent event) {
        messagingTemplate.convertAndSend(
            "/topic/game/" + gameId + "/events", event);
    }

    @Override
    public void publishLobbyUpdate(GameEvent event) {
        messagingTemplate.convertAndSend("/topic/lobby", event);
    }
}
```

---

## Configuración

```java
// config/WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
}

// config/SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Configuración temporal — permite todo hasta implementar JWT
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// config/OpenApiConfig.java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Pokémon TCG API")
                .description("Backend del juego Pokémon TCG — TPI Programación III")
                .version("1.0.0"));
    }
}

// config/CorsConfig.java
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
```
