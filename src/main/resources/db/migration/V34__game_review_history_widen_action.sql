-- Les nouveaux statuts de réactivation (REACTIVATION_DEMANDEE, REACTIVATION_ACCEPTEE,
-- REACTIVATION_REFUSEE, REACTIVATION_ANNULEE) dépassent les 20 caractères initialement prévus.
ALTER TABLE game_review_history ALTER COLUMN action TYPE VARCHAR(40);
