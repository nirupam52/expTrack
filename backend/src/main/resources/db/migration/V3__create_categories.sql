CREATE TABLE categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    name TEXT NOT NULL COLLATE NOCASE,
    UNIQUE (user_id, name),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_categories_user_id ON categories(user_id);
