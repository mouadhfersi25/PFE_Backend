-- Supprime les colonnes en double de cartes_memoire :
-- contenu (doublon de symbole) et paire (doublon de pair_key).
-- Exécuter une seule fois, après avoir redémarré l'app avec la nouvelle entité.

ALTER TABLE cartes_memoire DROP COLUMN IF EXISTS contenu;
ALTER TABLE cartes_memoire DROP COLUMN IF EXISTS paire;
