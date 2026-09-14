-- La colonne recompenses.id_sponsor et la table sponsors avaient été créées
-- silencieusement par Hibernate (ddl-auto=update) pour une entité Sponsor jamais
-- utilisée par le code (aucun repository/service ne la référençait, colonne
-- toujours NULL). On nettoie cet état incohérent puis on relie proprement
-- chaque récompense au compte User (Role.SPONSOR) qui l'a créée.

ALTER TABLE recompenses DROP COLUMN IF EXISTS id_sponsor;
DROP TABLE IF EXISTS sponsors;

ALTER TABLE recompenses ADD COLUMN id_sponsor BIGINT;
ALTER TABLE recompenses
    ADD CONSTRAINT fk_recompenses_sponsor FOREIGN KEY (id_sponsor) REFERENCES users(id);
