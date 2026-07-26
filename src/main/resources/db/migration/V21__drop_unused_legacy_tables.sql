-- Tables correspondant à d'anciennes entités JPA non utilisées par l'application (aucun repository / flux métier).
-- Ordre : enfants avant parents (FK).

DROP TABLE IF EXISTS participations_tournoi CASCADE;
DROP TABLE IF EXISTS membres_equipe CASCADE;
DROP TABLE IF EXISTS tournois CASCADE;
DROP TABLE IF EXISTS equipes CASCADE;
DROP TABLE IF EXISTS statistiques_performance CASCADE;
DROP TABLE IF EXISTS interactions_jeu CASCADE;
DROP TABLE IF EXISTS evenements_reflexe CASCADE;
DROP TABLE IF EXISTS recommandations_ia CASCADE;
DROP TABLE IF EXISTS profils_educatifs CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS points_user CASCADE;
DROP TABLE IF EXISTS localisations CASCADE;
DROP TABLE IF EXISTS abonnements_sponsor CASCADE;
