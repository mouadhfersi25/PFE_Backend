-- Exécuter manuellement si la colonne id_ville existe encore sur users (après suppression de l'entité Ville).
-- Cela évite tout blocage à l'UPDATE lors de l'onboarding.

ALTER TABLE users DROP COLUMN IF EXISTS id_ville;
-- Optionnel : supprimer la table villes si elle n'est plus utilisée
-- DROP TABLE IF EXISTS villes;
