ALTER TABLE sessions_jeu
ADD COLUMN IF NOT EXISTS room_expected_players INTEGER;

ALTER TABLE sessions_jeu
DROP CONSTRAINT IF EXISTS ck_sessions_room_expected_players;

ALTER TABLE sessions_jeu
ADD CONSTRAINT ck_sessions_room_expected_players
CHECK (room_expected_players IS NULL OR room_expected_players BETWEEN 2 AND 4);
