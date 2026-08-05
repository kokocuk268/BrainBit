-- +goose Up

-- ТАБЛИЦА ПОЛЬЗОВАТЕЛЕЙ
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- +goose Down

-- УДАЛЕНИЕ ТАБЛИЦЫ ПОЛЬЗОВАТЕЛЕЙ
DROP TABLE IF EXISTS users;