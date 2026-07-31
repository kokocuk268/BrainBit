package httpserver

import (
	"encoding/json"
	"fmt"
	"net/http"
)

type healthResponse struct {
	Status string `json:"status"`
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
