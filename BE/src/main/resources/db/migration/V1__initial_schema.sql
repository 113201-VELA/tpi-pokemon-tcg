-- ============================================================
-- EXTENSIONES
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- PLAYERS
-- ============================================================
CREATE TABLE players (
                         id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         username    VARCHAR(50)  NOT NULL UNIQUE,
                         email       VARCHAR(255) NOT NULL UNIQUE,
                         password    VARCHAR(255) NOT NULL,          -- BCrypt hash
                         created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                         updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_players_username ON players(username);

-- ============================================================
-- CARD CACHE  (cartas descargadas de pokemontcg.io)
-- ============================================================
CREATE TABLE card_cache (
                            id              VARCHAR(20)  PRIMARY KEY,   -- ej: "xy1-1"
                            set_id          VARCHAR(10)  NOT NULL,       -- ej: "xy1"
                            name            VARCHAR(100) NOT NULL,
                            supertype       VARCHAR(20)  NOT NULL,       -- Pokemon | Trainer | Energy
                            subtypes        TEXT[]       NOT NULL DEFAULT '{}',
    -- Pokémon específico
                            hp              INTEGER,
                            types           TEXT[]       DEFAULT '{}',
                            evolves_from    VARCHAR(100),
                            evolves_to      TEXT[]       DEFAULT '{}',
                            rules           TEXT[]       DEFAULT '{}',
                            abilities       JSONB        DEFAULT '[]',   -- [{name, text, type}]
                            attacks         JSONB        DEFAULT '[]',   -- [{name, cost[], damage, text}]
                            weaknesses      JSONB        DEFAULT '[]',   -- [{type, value}]
                            resistances     JSONB        DEFAULT '[]',   -- [{type, value}]
                            retreat_cost    TEXT[]       DEFAULT '{}',
    -- Energía específico
                            is_basic_energy BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Metadatos
                            image_small     VARCHAR(500),
                            image_large     VARCHAR(500),
                            rarity          VARCHAR(50),
                            number          VARCHAR(10),
                            raw_data        JSONB        NOT NULL,       -- respuesta completa de la API
                            cached_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_card_cache_set_id  ON card_cache(set_id);
CREATE INDEX idx_card_cache_name    ON card_cache(name);
CREATE INDEX idx_card_cache_supertype ON card_cache(supertype);
-- Índice GIN para búsqueda en arrays y JSONB
CREATE INDEX idx_card_cache_types   ON card_cache USING GIN(types);
CREATE INDEX idx_card_cache_subtypes ON card_cache USING GIN(subtypes);

-- ============================================================
-- DECKS
-- ============================================================
CREATE TABLE decks (
                       id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       player_id   UUID        NOT NULL REFERENCES players(id) ON DELETE CASCADE,
                       name        VARCHAR(100) NOT NULL,
                       description TEXT,
                       is_valid    BOOLEAN     NOT NULL DEFAULT FALSE,
                       created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_decks_player_id ON decks(player_id);

CREATE TABLE deck_cards (
                            id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            deck_id     UUID        NOT NULL REFERENCES decks(id) ON DELETE CASCADE,
                            card_id     VARCHAR(20) NOT NULL REFERENCES card_cache(id),
                            quantity    INTEGER     NOT NULL CHECK (quantity BETWEEN 1 AND 60),
                            UNIQUE (deck_id, card_id)
);

CREATE INDEX idx_deck_cards_deck_id ON deck_cards(deck_id);

-- ============================================================
-- GAMES
-- ============================================================
CREATE TABLE games (
                       id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       state           VARCHAR(20)  NOT NULL DEFAULT 'WAITING'
                           CHECK (state IN ('WAITING','SETUP','ACTIVE','FINISHED')),
                       winner_id       UUID         REFERENCES players(id),
                       finish_reason   VARCHAR(50), -- PRIZES, KNOCKOUT, DECK_OUT, SUDDEN_DEATH
                       is_sudden_death BOOLEAN      NOT NULL DEFAULT FALSE,
                       parent_game_id  UUID         REFERENCES games(id),  -- para Muerte Súbita
                       created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       finished_at     TIMESTAMPTZ
);

CREATE INDEX idx_games_state     ON games(state);
CREATE INDEX idx_games_created_at ON games(created_at DESC);

CREATE TABLE game_players (
                              id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              game_id     UUID        NOT NULL REFERENCES games(id) ON DELETE CASCADE,
                              player_id   UUID        NOT NULL REFERENCES players(id),
                              deck_id     UUID        NOT NULL REFERENCES decks(id),
                              player_number INTEGER   NOT NULL CHECK (player_number IN (1, 2)),
                              UNIQUE (game_id, player_id),
                              UNIQUE (game_id, player_number)
);

CREATE INDEX idx_game_players_game_id   ON game_players(game_id);
CREATE INDEX idx_game_players_player_id ON game_players(player_id);

-- ============================================================
-- GAME STATE  (snapshot del tablero después de cada acción)
-- ============================================================
CREATE TABLE game_states (
                             id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             game_id         UUID        NOT NULL REFERENCES games(id) ON DELETE CASCADE,
                             turn_number     INTEGER     NOT NULL,
                             turn_phase      VARCHAR(20) NOT NULL
                                 CHECK (turn_phase IN ('DRAW','MAIN','ATTACK','BETWEEN_TURNS','SETUP')),
                             current_player_id UUID      NOT NULL REFERENCES players(id),
                             board_state     JSONB       NOT NULL,    -- BoardState completo serializado
                             created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Solo el snapshot más reciente importa para el juego en curso.
-- Los históricos sirven para auditoría y reconexión.
CREATE INDEX idx_game_states_game_id    ON game_states(game_id);
CREATE INDEX idx_game_states_created_at ON game_states(game_id, created_at DESC);

-- ============================================================
-- GAME LOG  (inmutable — solo INSERT)
-- ============================================================
CREATE TABLE game_log (
                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          game_id         UUID        NOT NULL REFERENCES games(id) ON DELETE CASCADE,
                          turn_number     INTEGER     NOT NULL,
                          player_id       UUID        REFERENCES players(id),
                          action_type     VARCHAR(50) NOT NULL,   -- DRAW, PLACE_POKEMON, ATTACH_ENERGY, ATTACK, etc.
                          action_data     JSONB       NOT NULL DEFAULT '{}',  -- parámetros de la acción
                          result          VARCHAR(20) NOT NULL    -- SUCCESS, FAILED, CANCELLED
                              CHECK (result IN ('SUCCESS','FAILED','CANCELLED')),
                          result_data     JSONB       NOT NULL DEFAULT '{}',  -- qué ocurrió (daño, condición, etc.)
                          created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_game_log_game_id    ON game_log(game_id);
CREATE INDEX idx_game_log_created_at ON game_log(game_id, created_at);

-- ============================================================
-- RANKING / HISTORIAL  (opcional)
-- ============================================================
CREATE TABLE player_stats (
                              player_id   UUID PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
                              wins        INTEGER NOT NULL DEFAULT 0,
                              losses      INTEGER NOT NULL DEFAULT 0,
                              draws       INTEGER NOT NULL DEFAULT 0,  -- Muerte Súbita que no resuelve
                              rating      INTEGER NOT NULL DEFAULT 1000,
                              updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);