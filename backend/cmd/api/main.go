package main

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/kokocuk268/BrainBit/backend/internal/config"
	"github.com/kokocuk268/BrainBit/backend/internal/httpserver"
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
	serv := http.Server{
		Addr:              cfg.HTTPAddr,
		Handler:           httpserver.NewRouter(),
		ReadHeaderTimeout: time.Second,
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
