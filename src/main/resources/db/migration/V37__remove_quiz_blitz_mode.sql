-- Suppression du mode de partie Quiz "Blitz 60 secondes" : tous les quiz redeviennent
-- Classique par défaut, le champ quiz_play_mode n'existe plus (colonne + enum retirés).
ALTER TABLE jeux DROP COLUMN IF EXISTS quiz_play_mode;
