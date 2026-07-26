-- Associe chaque jeu à l'éducateur qui l'a créé (si présent).
-- Idempotent : n'altère rien si la table jeux n'existe pas.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'jeux'
    ) THEN
        -- Colonne nullable pour rester compatible avec les jeux existants / jeux créés par admin.
        ALTER TABLE jeux
            ADD COLUMN IF NOT EXISTS id_educateur BIGINT;

        -- Index pour accélérer les filtres/joins par éducateur.
        CREATE INDEX IF NOT EXISTS idx_jeux_id_educateur ON jeux(id_educateur);

        -- FK vers users(id), suppression de l'éducateur => on garde le jeu et on met null.
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_schema = current_schema()
              AND table_name = 'jeux'
              AND constraint_name = 'fk_jeux_educateur'
        ) THEN
            ALTER TABLE jeux
                ADD CONSTRAINT fk_jeux_educateur
                    FOREIGN KEY (id_educateur) REFERENCES users(id) ON DELETE SET NULL;
        END IF;
    END IF;
END $$;

