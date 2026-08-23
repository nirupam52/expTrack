CREATE VIRTUAL TABLE expenses_search USING fts5(title, note);

INSERT INTO expenses_search(rowid, title, note)
    SELECT id, title, coalesce(note, '') FROM expenses;

CREATE TRIGGER expenses_search_after_insert AFTER INSERT ON expenses BEGIN
    INSERT INTO expenses_search(rowid, title, note)
        VALUES (new.id, new.title, coalesce(new.note, ''));
END;

CREATE TRIGGER expenses_search_after_delete AFTER DELETE ON expenses BEGIN
    DELETE FROM expenses_search WHERE rowid = old.id;
END;

CREATE TRIGGER expenses_search_after_search_update
AFTER UPDATE OF title, note ON expenses BEGIN
    DELETE FROM expenses_search WHERE rowid = old.id;
    INSERT INTO expenses_search(rowid, title, note)
        VALUES (new.id, new.title, coalesce(new.note, ''));
END;
