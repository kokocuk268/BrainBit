package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func performRequest(
	t *testing.T,
	handler http.Handler,
	method string,
	path string,
) *httptest.ResponseRecorder {
	t.Helper()
	resp := httptest.NewRecorder()
	req := httptest.NewRequest(method, path, nil)

	handler.ServeHTTP(resp, req)

	return resp
}

func TestHealthLiveStatusCode(t *testing.T) {
	resp := performRequest(t, newRouter(), http.MethodGet, "/health/live")

	if resp.Code != http.StatusOK {
		t.Fatalf("ошибка сервера %d", resp.Code)
	}

	var respStatus Resp

	if err := json.NewDecoder(resp.Body).Decode(&respStatus); err != nil {
		t.Fatalf("ошибка %v", err)
	}

	if respStatus.Status != "ok" {
		t.Errorf(
			"неверное тело ответа: получили %q, ожидали %q",
			respStatus.Status,
			"ok",
		)
	}

	contentType := resp.Header().Get("Content-Type")

	if contentType != "application/json" {
		t.Errorf(
			"неверный Content-Type: получили %q, ожидали %q",
			contentType,
			"application/json",
		)
	}
}

func TestHealthRouting(t *testing.T) {
	tests := []struct {
		name       string
		method     string
		path       string
		wantStatus int
	}{
		{
			name:       "unknown route",
			method:     http.MethodGet,
			path:       "/unknown",
			wantStatus: http.StatusNotFound,
		},
		{
			name:       "method post",
			method:     http.MethodPost,
			path:       "/health/live",
			wantStatus: http.StatusMethodNotAllowed,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			resp := performRequest(t, newRouter(), test.method, test.path)

			if resp.Code != test.wantStatus {
				t.Fatalf("ошибка теста, получили %v, ожидали %v", resp.Code, test.wantStatus)
			}
		})
	}
}
