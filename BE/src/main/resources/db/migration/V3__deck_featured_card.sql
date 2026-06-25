-- Add featured_card_id to decks.
-- Nullable: a deck may have no featured card selected.
-- FK references card_cache so only cached cards can be selected.
ALTER TABLE decks
    ADD COLUMN featured_card_id VARCHAR(20)
    REFERENCES card_cache(id)
    ON DELETE SET NULL;
