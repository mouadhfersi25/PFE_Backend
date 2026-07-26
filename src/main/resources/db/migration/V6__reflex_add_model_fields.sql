ALTER TABLE parametres_reflexe
ADD COLUMN IF NOT EXISTS modele_reflexe VARCHAR(50);

ALTER TABLE parametres_reflexe
ADD COLUMN IF NOT EXISTS no_go_ratio INTEGER;

ALTER TABLE parametres_reflexe
ADD COLUMN IF NOT EXISTS choice_target_count INTEGER;

UPDATE parametres_reflexe
SET modele_reflexe = COALESCE(modele_reflexe, 'CLASSIC'),
    no_go_ratio = COALESCE(no_go_ratio, 30),
    choice_target_count = COALESCE(choice_target_count, 3);
