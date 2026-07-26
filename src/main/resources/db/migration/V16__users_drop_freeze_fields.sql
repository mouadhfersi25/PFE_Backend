ALTER TABLE users
    DROP COLUMN IF EXISTS freeze_remaining,
    DROP COLUMN IF EXISTS freeze_period_start;
