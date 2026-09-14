-- Les publicités sponsor ne sont pas monétisées : suppression du budget.

ALTER TABLE publicites DROP COLUMN IF EXISTS budget_utilise;
