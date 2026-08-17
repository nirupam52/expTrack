CREATE TABLE expenses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    category_id INTEGER NOT NULL,
    expense_date DATE NOT NULL,
    currency TEXT NOT NULL,
    note TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_expenses_user_date ON expenses(user_id, expense_date DESC);
