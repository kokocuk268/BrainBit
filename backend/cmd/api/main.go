package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"
)

type Resp struct {
	Status string `json:"status"`
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	js := Resp{Status: "ok"}
	jsonByte, err := json.Marshal(js)
	if err != nil {
		http.Error(w, "ошибка 500", http.StatusInternalServerError)
		return
	}
	fmt.Fprintln(w, string(jsonByte))
}

func newRouter() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health/live", healthHandler)
	return mux
}

func main() {
	ctx, stop := signal.NotifyContext(
		context.Background(),
		os.Interrupt,
		syscall.SIGTERM,
	)
	defer stop()

	errCh := make(chan error, 1)

	serv := http.Server{
		Addr:              ":8080",
		Handler:           newRouter(),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       10 * time.Second,
		WriteTimeout:      10 * time.Second,
		IdleTimeout:       60 * time.Second,
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
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := serv.Shutdown(shutdownCtx); err != nil {
		fmt.Println(err)
		return
	}
	fmt.Println("Сервер корректно остановлен!👌")
}
