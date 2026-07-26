-- PostgreSQL (manuel si besoin)
-- Même logique que db/migration/V2__jeux_etat_check_add_brouillon.sql.
-- Au démarrage, Flyway applique automatiquement V2 sur la base configurée.

ALTER TABLE jeux
DROP CONSTRAINT IF EXISTS jeux_etat_check;

ALTER TABLE jeux
ADD CONSTRAINT jeux_etat_check
CHECK (etat IN ('BROUILLON', 'EN_ATTENTE', 'ACCEPTE', 'REFUSE'));

