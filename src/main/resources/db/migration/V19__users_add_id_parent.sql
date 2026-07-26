-- Lien tuteur (PARENT) → joueur (JOUEUR). Un joueur a au plus un parent référencé.
ALTER TABLE users
    ADD COLUMN id_parent BIGINT NULL REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX idx_users_id_parent ON users (id_parent);
