package httpserver

import "net/http"

func NewRouter(db databasePinger) http.Handler {
	mux := http.NewServeMux()

	mux.HandleFunc("GET /health/live", healthHandler)
	mux.HandleFunc("GET /health/ready", readinessHandler(db))

	return mux
}
