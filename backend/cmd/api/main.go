package main

import (
	"encoding/json"
	"fmt"
	"net/http"
)

type Resp struct {
	Status string `json:"status"`
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	js := Resp{Status: "ok"}
	jsonByte, err := json.Marshal(js)
	if err != nil {
		fmt.Fprintln(w, http.StatusInternalServerError)
		return
	}
	fmt.Fprintln(w, string(jsonByte))
}

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health/live", healthHandler)

	if err := http.ListenAndServe(":8080", mux); err != nil {
		fmt.Println(err)
		return
	}
}