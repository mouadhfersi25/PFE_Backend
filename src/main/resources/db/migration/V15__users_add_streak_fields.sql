ALTER TABLE users
    ADD COLUMN IF NOT EXISTS current_streak_days INTEGER,
    ADD COLUMN IF NOT EXISTS best_streak_days INTEGER,
    ADD COLUMN IF NOT EXISTS last_streak_date DATE,
    ADD COLUMN IF NOT EXISTS freeze_remaining INTEGER,
    ADD COLUMN IF NOT EXISTS freeze_period_start DATE;

UPDATE users
SET
    current_streak_days = COALESCE(current_streak_days, 0),
    best_streak_days = COALESCE(best_streak_days, 0),
    freeze_remaining = COALESCE(freeze_remaining, 1),
    freeze_period_start = COALESCE(freeze_period_start, CURRENT_DATE)
WHERE role = 'JOUEUR';
