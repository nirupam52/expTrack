CREATE INDEX idx_expenses_search
    ON expenses
    USING GIN (
        to_tsvector(
            'simple'::regconfig,
            coalesce(title, '') || ' ' || coalesce(note, '')
        )
    );
