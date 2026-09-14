-- CIN pour les comptes parent (unique si renseigné)
ALTER TABLE users ADD COLUMN IF NOT EXISTS cin VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_cin ON users (cin) WHERE cin IS NOT NULL;
