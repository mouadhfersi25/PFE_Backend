-- Genre : remplace la relation users.id_genre -> genres (table jamais alimentée par
-- l'application) par une colonne directe users.genre (HOMME/FEMME).
ALTER TABLE users DROP COLUMN IF EXISTS id_genre;
DROP TABLE IF EXISTS genres;
ALTER TABLE users ADD COLUMN genre VARCHAR(20);

-- Difficulté des jeux : passe d'une échelle numérique (0-10) à trois valeurs texte.
ALTER TABLE jeux ALTER COLUMN difficulte TYPE VARCHAR(20)
    USING (
        CASE
            WHEN difficulte IS NULL THEN NULL
            WHEN difficulte <= 3 THEN 'FACILE'
            WHEN difficulte <= 6 THEN 'MOYEN'
            ELSE 'DIFFICILE'
        END
    );

-- Icône de jeu : champ supprimé (jamais essentiel, toujours un fallback par type côté frontend).
ALTER TABLE jeux DROP COLUMN IF EXISTS icone;

-- Table niveaux : jamais alimentée par l'application (aucun endpoint d'insertion), la
-- courbe XP par défaut codée en dur est la seule utilisée en pratique.
DROP TABLE IF EXISTS niveaux;

-- Colonne fantôme laissée par une ancienne version de l'entité PuzzleLogique (remplacée
-- depuis par bonne_reponse) ; jamais mappée par le code actuel.
ALTER TABLE puzzles_logiques DROP COLUMN IF EXISTS solution;
