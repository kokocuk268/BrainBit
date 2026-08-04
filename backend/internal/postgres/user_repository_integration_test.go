//go:build integration

package postgres

import (
	"context"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/kokocuk268/BrainBit/backend/internal/domain"
)

func TestUserRepositoryCreateAndFindByEmail(t *testing.T) {
	databaseURL := os.Getenv("DATABASE_URL")
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
			t.Fatalf("не удалось удалить тестового пользователя: %v", err)
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
