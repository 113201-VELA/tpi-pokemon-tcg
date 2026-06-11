-- ============================================================
-- V2: player nickname, deck cosmetics, player matchups,
--     cancelled game state
-- ============================================================

-- ------------------------------------------------------------
-- 1. Add CANCELLED to games.state check constraint
--    Drop the existing constraint and recreate it with the
--    new value. The constraint name matches the one generated
--    by PostgreSQL from V1 — verify the exact name with:
--    \d games
--    and replace 'games_state_check' below if it differs.
-- ------------------------------------------------------------
ALTER TABLE games
    DROP CONSTRAINT IF EXISTS games_state_check;

ALTER TABLE games
    ADD CONSTRAINT games_state_check
    CHECK (state IN ('WAITING', 'SETUP', 'ACTIVE', 'FINISHED', 'CANCELLED'));

-- ------------------------------------------------------------
-- 2. Add nickname to players
--    Default matches username so existing rows are valid.
--    Unique constraint ensures no two players share a nickname.
--    Max length: 30 characters.
-- ------------------------------------------------------------
ALTER TABLE players
    ADD COLUMN nickname VARCHAR(30);

UPDATE players
    SET nickname = username
    WHERE nickname IS NULL;

ALTER TABLE players
    ALTER COLUMN nickname SET NOT NULL,
    ADD CONSTRAINT players_nickname_unique UNIQUE (nickname);

-- ------------------------------------------------------------
-- 3. Add card_back and coin to decks
--    Both use a string identifier that maps to a local asset
--    in the frontend. 'DEFAULT' is the baseline skin.
-- ------------------------------------------------------------
ALTER TABLE decks
    ADD COLUMN card_back VARCHAR(50) NOT NULL DEFAULT 'DEFAULT',
    ADD COLUMN coin      VARCHAR(50) NOT NULL DEFAULT 'DEFAULT';

-- ------------------------------------------------------------
-- 4. Drop description from decks
--    Column is unused. Safe to drop since no data is stored.
-- ------------------------------------------------------------
ALTER TABLE decks
    DROP COLUMN IF EXISTS description;

-- ------------------------------------------------------------
-- 5. Create player_matchups table
--    Tracks wins and losses per player-opponent pair.
--    Updated by GameService when a game reaches FINISHED state.
--    No draws: Sudden Death resolves to a winner.
--    Primary key is the (player_id, opponent_id) pair.
-- ------------------------------------------------------------
CREATE TABLE player_matchups (
    player_id   UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    opponent_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    wins        INTEGER NOT NULL DEFAULT 0,
    losses      INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (player_id, opponent_id)
);

CREATE INDEX idx_player_matchups_player_id
    ON player_matchups(player_id);

CREATE INDEX idx_player_matchups_opponent_id
    ON player_matchups(opponent_id);
