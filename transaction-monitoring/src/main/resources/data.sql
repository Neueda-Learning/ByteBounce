-- Default amount-threshold rule.
INSERT INTO rules (
    name,
    type,
    description,
    threshold,
    time_window,
    max_count,
    severity,
    enabled,
    created_time,
    updated_time
)
SELECT
    'Large Amount Transaction',
    'AMOUNT_THRESHOLD',
    'Trigger when transaction amount exceeds threshold',
    10000,
    NULL,
    NULL,
    'HIGH',
    TRUE,
    UTC_TIMESTAMP(),
    UTC_TIMESTAMP()
WHERE NOT EXISTS (
    SELECT 1 FROM rules WHERE name = 'Large Amount Transaction'
);

-- Default velocity rule.
INSERT INTO rules (
    name,
    type,
    description,
    threshold,
    time_window,
    max_count,
    severity,
    enabled,
    created_time,
    updated_time
)
SELECT
    'High Frequency Transaction',
    'VELOCITY',
    'Too many transactions within a short period',
    NULL,
    10,
    5,
    'HIGH',
    TRUE,
    UTC_TIMESTAMP(),
    UTC_TIMESTAMP()
WHERE NOT EXISTS (
    SELECT 1 FROM rules WHERE name = 'High Frequency Transaction'
);

-- Default new-payee rule.
INSERT INTO rules (
    name,
    type,
    description,
    threshold,
    time_window,
    max_count,
    severity,
    enabled,
    created_time,
    updated_time
)
SELECT
    'New Payee Transaction',
    'NEW_PAYEE',
    'First transaction to a new payee',
    NULL,
    NULL,
    NULL,
    'MEDIUM',
    TRUE,
    UTC_TIMESTAMP(),
    UTC_TIMESTAMP()
WHERE NOT EXISTS (
    SELECT 1 FROM rules WHERE name = 'New Payee Transaction'
);

-- Default daily-limit rule.
INSERT INTO rules (
    name,
    type,
    description,
    threshold,
    time_window,
    max_count,
    severity,
    enabled,
    created_time,
    updated_time
)
SELECT
    'Daily Transaction Limit',
    'DAILY_LIMIT',
    'Daily transaction amount exceeds limit',
    50000,
    NULL,
    NULL,
    'HIGH',
    TRUE,
    UTC_TIMESTAMP(),
    UTC_TIMESTAMP()
WHERE NOT EXISTS (
    SELECT 1 FROM rules WHERE name = 'Daily Transaction Limit'
);
