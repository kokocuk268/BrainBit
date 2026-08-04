package httpserver

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

type healthResponse struct {
	Status string `json:"status"`
}

type databasePinger interface {
	Ping(context.Context) error
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	js := healthResponse{Status: "ok"}
	jsonByte, err := json.Marshal(js)
	if err != nil {
		http.Error(w, "ошибка 500", http.StatusInternalServerError)
		return
	}
	fmt.Fprintln(w, string(jsonByte))
}

func readinessHandler(db databasePinger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
		defer cancel()

		w.Header().Set("Content-Type", "application/json")

		if err := db.Ping(ctx); err != nil {
			w.WriteHeader(http.StatusServiceUnavailable)

			_ = json.NewEncoder(w).Encode(healthResponse{
				Status: "unavailable",
			})

			return
		}

		w.WriteHeader(http.StatusOK)

		_ = json.NewEncoder(w).Encode(healthResponse{
			Status: "ok",
		})
	}
}
