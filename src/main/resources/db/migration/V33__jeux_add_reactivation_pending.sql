-- Demande de réactivation d'un jeu désactivé (suite à signalement), corrigé par l'éducateur,
-- en attente de décision admin (accepter -> actif=true, refuser -> reste désactivé).
ALTER TABLE jeux ADD COLUMN IF NOT EXISTS reactivation_pending BOOLEAN NOT NULL DEFAULT FALSE;
