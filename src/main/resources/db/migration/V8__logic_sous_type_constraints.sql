UPDATE puzzles_logiques
SET sous_type = 'DEDUCTION'
WHERE sous_type IS NULL OR sous_type = '';

UPDATE puzzles_logiques
SET sous_type = 'DEDUCTION'
WHERE sous_type NOT IN ('SUITE_LOGIQUE', 'INTRUS', 'DEDUCTION');

ALTER TABLE puzzles_logiques
ALTER COLUMN sous_type SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_puzzles_logiques_sous_type'
          AND table_name = 'puzzles_logiques'
    ) THEN
        ALTER TABLE puzzles_logiques
        ADD CONSTRAINT chk_puzzles_logiques_sous_type
        CHECK (sous_type IN ('SUITE_LOGIQUE', 'INTRUS', 'DEDUCTION'));
    END IF;
END $$;

