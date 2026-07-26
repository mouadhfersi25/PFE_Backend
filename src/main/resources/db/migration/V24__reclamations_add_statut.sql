ALTER TABLE reclamations
    ADD COLUMN IF NOT EXISTS statut VARCHAR(20) NOT NULL DEFAULT 'OUVERT';

UPDATE reclamations
SET statut = 'TRAITE'
WHERE id_admin IS NOT NULL
   OR (reponse_admin IS NOT NULL AND TRIM(reponse_admin) <> '');

CREATE INDEX IF NOT EXISTS idx_reclamations_statut ON reclamations(statut);
