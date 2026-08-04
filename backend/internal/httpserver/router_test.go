package httpserver

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

type fakePinger struct {
	err error
}

func (f fakePinger) Ping(ctx context.Context) error {
	return f.err
}

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
	resp := performRequest(t, NewRouter(fakePinger{}), http.MethodGet, "/health/live")

	if resp.Code != http.StatusOK {
		t.Fatalf("ошибка сервера %d", resp.Code)
	}

	var respStatus healthResponse

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
	wantContentType := "application/json"
	if !strings.HasPrefix(contentType, wantContentType) {
		t.Errorf(
			"неверный Content-Type: получили %q, ожидали %q",
			contentType,
			wantContentType,
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
			resp := performRequest(t, NewRouter(fakePinger{}), test.method, test.path)

			if resp.Code != test.wantStatus {
				t.Fatalf("ошибка теста, получили %v, ожидали %v", resp.Code, test.wantStatus)
			}
		})
	}
}

func TestHealthReady(t *testing.T) {
	tests := []struct {
		name       string
		pingErr    error
		wantStatus int
		wantBody   healthResponse
	}{
		{
			name:       "database is available",
			pingErr:    nil,
			wantStatus: http.StatusOK,
			wantBody: healthResponse{
				Status: "ok",
			},
		},
		{
			name:       "database is unavailable",
			pingErr:    errors.New("database unavailable"),
			wantStatus: http.StatusServiceUnavailable,
			wantBody: healthResponse{
				Status: "unavailable",
			},
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			router := NewRouter(fakePinger{
				err: test.pingErr,
			})

			recorder := performRequest(
				t,
				router,
				http.MethodGet,
				"/health/ready",
			)

			contentType := recorder.Header().Get("Content-Type")
			wantContentType := "application/json"
			if !strings.HasPrefix(contentType, wantContentType) {
				t.Fatalf(
					"ошибка заголовка: получили %s, ожидали %s",
					contentType,
					wantContentType,
				)
			}

			if recorder.Code != test.wantStatus {
				t.Fatalf(
					"неверный HTTP status: получили %d, ожидали %d",
					recorder.Code,
					test.wantStatus,
				)
			}

			var got healthResponse

			if err := json.NewDecoder(recorder.Body).Decode(&got); err != nil {
				t.Fatalf("не удалось прочитать json - %v", err)
			}

			if got != test.wantBody {
				t.Errorf(
					"статус json не совпадает: получили %v, ожидали %v",
					got,
					test.wantBody,
				)
			}
		})
	}
}
