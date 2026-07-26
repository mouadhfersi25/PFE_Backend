UPDATE parametres_reflexe
SET modele_reflexe = 'CLASSIC'
WHERE modele_reflexe IS NULL
   OR upper(modele_reflexe) NOT IN ('CLASSIC', 'GO_NO_GO', 'CHOICE_REACTION');

UPDATE parametres_reflexe
SET type_stimuli = 'TARGET_ICON'
WHERE type_stimuli IS NULL
   OR upper(type_stimuli) NOT IN ('TARGET_ICON', 'COLOR_FLASH', 'MIXED');

ALTER TABLE parametres_reflexe
DROP CONSTRAINT IF EXISTS ck_param_reflex_modele_reflexe;

ALTER TABLE parametres_reflexe
ADD CONSTRAINT ck_param_reflex_modele_reflexe
CHECK (modele_reflexe IN ('CLASSIC', 'GO_NO_GO', 'CHOICE_REACTION'));

ALTER TABLE parametres_reflexe
DROP CONSTRAINT IF EXISTS ck_param_reflex_type_stimuli;

ALTER TABLE parametres_reflexe
ADD CONSTRAINT ck_param_reflex_type_stimuli
CHECK (type_stimuli IN ('TARGET_ICON', 'COLOR_FLASH', 'MIXED'));
