CREATE TABLE purchase_request (
    id UUID PRIMARY KEY,
    business_key TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL,
    title TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT purchase_request_status_check CHECK (status IN ('DRAFT', 'APPROVED', 'ORDERED')),
    CONSTRAINT purchase_request_business_key_not_blank CHECK (length(btrim(business_key)) > 0),
    CONSTRAINT purchase_request_title_not_blank CHECK (length(btrim(title)) > 0)
);
