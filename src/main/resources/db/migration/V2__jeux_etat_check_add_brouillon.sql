-- Recrée la contrainte CHECK sur jeux.etat pour inclure BROUILLON.
-- Idempotent : n’altère rien si la table jeux n’existe pas encore (premier démarrage sur BDD vide).

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'jeux'
    ) THEN
        ALTER TABLE jeux DROP CONSTRAINT IF EXISTS jeux_etat_check;
        ALTER TABLE jeux
            ADD CONSTRAINT jeux_etat_check
                CHECK (etat IN ('BROUILLON', 'EN_ATTENTE', 'ACCEPTE', 'REFUSE'));
    END IF;
END $$;
