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

func TestUnknownRouteStatusCode(t *testing.T) {
	resp := performRequest(t, newRouter(), http.MethodGet, "/unknown")

	if resp.Code != http.StatusNotFound {
		t.Errorf(
			"неверный StatusCode: получили %d, ожидали %d",
			resp.Code,
			http.StatusNotFound,
		)
	}
}

func TestHealthLiveRejectsPost(t *testing.T) {
	resp := performRequest(t, newRouter(), http.MethodPost, "/health/live")

	if resp.Code != http.StatusMethodNotAllowed {
		t.Errorf(
			"неверный StatusCode: получили %d, ожидали %d",
			resp.Code,
			http.StatusMethodNotAllowed,
		)
	}
}
