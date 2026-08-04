package main

import (
	"context"
	"errors"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/kokocuk268/BrainBit/backend/internal/config"
	"github.com/kokocuk268/BrainBit/backend/internal/httpserver"
	"github.com/kokocuk268/BrainBit/backend/internal/postgres"
)

func main() {
	ctx, stop := signal.NotifyContext(
		context.Background(),
		os.Interrupt,
		syscall.SIGTERM,
	)
	defer stop()

	errCh := make(chan error, 1)

	cfg := config.Load()

	dbCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	pool, err := postgres.NewPool(dbCtx, cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("не удалось создать PostgreSQL pool: %v", err)
	}
	defer pool.Close()

	serv := http.Server{
		Addr:              cfg.HTTPAddr,
		Handler:           httpserver.NewRouter(pool),
		ReadHeaderTimeout: cfg.ReadHeaderTimeout,
		ReadTimeout:       cfg.ReadTimeout,
		WriteTimeout:      cfg.WriteTimeout,
		IdleTimeout:       cfg.IdleTimeout,
	}

	go func() {
		if err := serv.ListenAndServe(); err != nil {
			if errors.Is(err, http.ErrServerClosed) {
				return
			}

			errCh <- err
		}
	}()

	select {
	case <-ctx.Done():
		// пришёл ctrl + c или sigterm
		fmt.Println("Получен сигнал завершения")
	case err := <-errCh:
		// cервер упал с ошибкой
		fmt.Println(err)
		return
	}

	shutdownCtx, cancel := context.WithTimeout(context.Background(), cfg.ShutdownTimeout)
	defer cancel()
	if err := serv.Shutdown(shutdownCtx); err != nil {
		fmt.Println(err)
		return
	}
	fmt.Println("Сервер корректно остановлен!👌")
}
