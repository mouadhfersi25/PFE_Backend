ALTER TABLE jeux
DROP CONSTRAINT IF EXISTS jeux_mode_jeu_check;

ALTER TABLE sessions_jeu
DROP CONSTRAINT IF EXISTS sessions_jeu_mode_jeu_lance_check;

UPDATE jeux
SET mode_jeu = 'EN_LIGNE'
WHERE mode_jeu = 'COLLECTIF';

UPDATE sessions_jeu
SET mode_jeu_lance = 'EN_LIGNE'
WHERE mode_jeu_lance = 'COLLECTIF';

ALTER TABLE jeux
ADD CONSTRAINT jeux_mode_jeu_check
CHECK (mode_jeu IN ('INDIVIDUEL', 'EN_LIGNE'));

ALTER TABLE sessions_jeu
DROP COLUMN IF EXISTS team_name;
