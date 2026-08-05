//go:build integration

package postgres

import (
	"context"
	"errors"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/kokocuk268/BrainBit/backend/internal/domain"
)

var (
	databaseURL = os.Getenv("DATABASE_URL")
)

func TestUserRepositoryCreateAndFindByEmail(t *testing.T) {
	if strings.TrimSpace(databaseURL) == "" {
		t.Skip("DATABASE_URL не задан")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	pool, err := NewPool(ctx, databaseURL)
	if err != nil {
		t.Fatalf("ошибка пула: %v", err)
	}
	defer pool.Close()

	userRepo := NewUserRepository(pool)

	user := domain.User{
		ID:           "11111111-1111-4111-8111-111111111111",
		Email:        "repository-integration-test@example.com",
		PasswordHash: "jf7sa-ska813-d81k1d",
	}

	deleteQuery := `
		DELETE FROM users WHERE id = $1
	`

	if _, err := pool.Exec(ctx, deleteQuery, user.ID); err != nil {
		t.Fatalf("не удалось очистить данные перед тестом: %v", err)
	}

	defer func() {
		cleanupCtx, cleanupCancel := context.WithTimeout(
			context.Background(),
			5*time.Second,
		)
		defer cleanupCancel()

		if _, cleanupErr := pool.Exec(
			cleanupCtx,
			deleteQuery,
			user.ID,
		); cleanupErr != nil {
			t.Errorf("не удалось удалить тестового пользователя: %v", cleanupErr)
		}
	}()

	if err := userRepo.Create(ctx, user); err != nil {
		t.Fatalf("ошибка создание пользователя: %v", err)
	}

	testUser, err := userRepo.FindByEmail(ctx, user.Email)
	if err != nil {
		t.Fatalf("ошибка нахождения пользователя: %v", err)
	}

	if testUser.ID != user.ID {
		t.Errorf("ошибка разный ID пользователей: получили %v, ожидали %v", testUser.ID, user.ID)
	}
	if testUser.Email != user.Email {
		t.Errorf("ошибка разный Email пользователей: получили %v, ожидали %v", testUser.Email, user.Email)
	}
	if testUser.PasswordHash != user.PasswordHash {
		t.Errorf("ошибка разный PasswordHash пользователей: получили %v, ожидали %v", testUser.PasswordHash, user.PasswordHash)
	}
	if testUser.CreatedAt.IsZero() {
		t.Errorf("ошибка CreatedAt: %v", testUser.CreatedAt)
	}
}

func TestDuplicateEmail(t *testing.T) {
	if strings.TrimSpace(databaseURL) == "" {
		t.Skip("DATABASE_URL не задан")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	pool, err := NewPool(ctx, databaseURL)
	if err != nil {
		t.Fatalf("ошибка пула: %v", err)
	}
	defer pool.Close()

	users := []domain.User{
		{
			ID:           "22222222-2222-4111-8111-222222222222",
			Email:        "duplicate-email-integration0test@example.com",
			PasswordHash: "h1sd3-dk3la9-kcj4l5-kfa09",
		},
		{
			ID:           "33333333-3333-4111-8111-333333333333",
			Email:        "duplicate-email-integration0test@example.com",
			PasswordHash: "lb7mn2-pq109k-zxc98m-kaq123",
		},
	}

	deleteQuery := `DELETE FROM users WHERE id = $1`

	for _, user := range users {
		if _, err := pool.Exec(
			ctx,
			deleteQuery,
			user.ID,
		); err != nil {
			t.Fatalf(
				"не удалось подготовить чистую базу %s: %v",
				user.ID,
				err,
			)
		}
	}

	defer func() {
		cleanupCtx, cleanupCancel := context.WithTimeout(
			context.Background(),
			5*time.Second,
		)
		defer cleanupCancel()

		for _, user := range users {
			if _, cleanupErr := pool.Exec(
				cleanupCtx,
				deleteQuery,
				user.ID,
			); cleanupErr != nil {
				t.Errorf(
					"не удалось убрать за завершившимся тестом %s: %v",
					user.ID,
					cleanupErr,
				)
			}
		}
	}()

	usRep := NewUserRepository(pool)

	t.Run("first user", func(t *testing.T) {
		if err := usRep.Create(ctx, users[0]); err != nil {
			t.Fatalf("ошибка создания пользователя: %v", err)
		}
	})

	t.Run("second user duplicate email", func(t *testing.T) {
		err := usRep.Create(ctx, users[1])
		if !errors.Is(err, domain.ErrEmailAlreadyExists) {
			t.Fatalf("ожидали ErrEmailAlreadyExists, получили %v",
				err,
			)
		}
	})

	usCheck, err := usRep.FindByEmail(ctx, users[0].Email)
	if err != nil {
		t.Fatalf("ошибка поиска по Email: %v", err)
	}

	if usCheck.ID != users[0].ID {
		t.Errorf("ошибка не тот пользователь: получили %v, ожидали %v", usCheck.ID, users[0].ID)
	}
}

func TestUserRepositoryFindByEmailNotFound(t *testing.T) {
	if strings.TrimSpace(databaseURL) == "" {
		t.Skip("DATABASE_URL не задан")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	pool, err := NewPool(ctx, databaseURL)
	if err != nil {
		t.Fatalf("ошибка пула: %v", err)
	}
	defer pool.Close()

	userRepo := NewUserRepository(pool)

	email := "missing-user-integration-test@example.com"

	deleteQuery := "DELETE FROM users WHERE email = $1"

	if _, err = pool.Exec(
		ctx,
		deleteQuery,
		email,
	); err != nil {
		t.Fatalf("не удалось подготовить данные: %v", err)
	}

	foundUser, err := userRepo.FindByEmail(ctx, email)

	if !errors.Is(err, domain.ErrUserNotFound) {
		t.Fatalf(
			"ожидали ErrUserNotFound, получили: %v",
			err,
		)
	}

	if foundUser != (domain.User{}) {
		t.Errorf(
			"ожидали пустого пользователя, получили %+v",
			foundUser,
		)
	}
}
