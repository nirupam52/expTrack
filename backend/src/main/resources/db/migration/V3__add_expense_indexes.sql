CREATE INDEX idx_expenses_user_category_date_id
    ON expenses(user_id, category_id, expense_date DESC, id DESC);

CREATE INDEX idx_expenses_user_date_id
    ON expenses(user_id, expense_date DESC, id DESC);
