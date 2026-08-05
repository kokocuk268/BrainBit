.PHONY: db-up db-down migrate-up migrate-down-one migrate-status

DATABASE_URL ?= postgres://brainfocus:brainfocus@localhost:5432/brainfocus?sslmode=disable

db-up:
	docker compose up -d postgres

db-down:
	docker compose down

migrate-up:
	goose -dir migrations postgres "$(DATABASE_URL)" up

migrate-down-one:
	goose -dir migrations postgres "$(DATABASE_URL)" down

migrate-status:
	goose -dir migrations postgres "$(DATABASE_URL)" status
