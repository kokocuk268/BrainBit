package postgres

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/kokocuk268/BrainBit/backend/internal/domain"
)

type UserRepository struct {
	pool *pgxpool.Pool
}

func NewUserRepository(pool *pgxpool.Pool) *UserRepository {
	return &UserRepository{
		pool: pool,
	}
}

func (u *UserRepository) Create(
	ctx context.Context,
	user domain.User,
) error {
	query := `
		INSERT INTO users (id, email, password_hash)
		VALUES ($1, $2, $3)
	`
	_, err := u.pool.Exec(
		ctx,
		query,
		user.ID,
		user.Email,
		user.PasswordHash,
	)
	if err != nil {
		return fmt.Errorf("создание пользователя: %w", err)
	}

	return nil
}

func (u *UserRepository) FindByEmail(
	ctx context.Context,
	email string,
) (domain.User, error) {
	query := `
		SELECT id,
			email,
			password_hash,
			created_at
		FROM users
		WHERE email = $1
	`

	var user domain.User

	row := u.pool.QueryRow(
		ctx,
		query,
		email,
	)
	err := row.Scan(
		&user.ID,
		&user.Email,
		&user.PasswordHash,
		&user.CreatedAt,
	)
	if err != nil {
		return domain.User{}, fmt.Errorf("ошибка получения данных: %w", err)
	}

	return user, nil
}
