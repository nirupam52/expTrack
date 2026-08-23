CREATE INDEX idx_expenses_user_date_id
    ON expenses(user_id, expense_date DESC, id DESC);
