package config

import (
	"testing"
	"time"
)

func TestLoadHTTPAddr(t *testing.T) {

	t.Run("default address", func(t *testing.T) {
		t.Setenv("HTTP_ADDR", "")
		cfg := Load()
		if cfg.HTTPAddr != ":8080" {
			t.Errorf(
				"несовпадение HTTP_ADDR == \"\", но сервер имеет порт %v",
				cfg.HTTPAddr,
			)
		}
	})

	t.Run("address from env", func(t *testing.T) {
		t.Setenv("HTTP_ADDR", ":9090")
		cfg := Load()
		if cfg.HTTPAddr != ":9090" {
			t.Errorf(
				"несовпадение HTTP_ADDR == \":9090\", но сервер имеет порт %v",
				cfg.HTTPAddr,
			)
		}
	})
}

func TestLoadDefaults(t *testing.T) {
	t.Setenv("HTTP_ADDR", "")
	cfg := Load()
	if cfg.HTTPAddr != ":8080" {
		t.Errorf(
			"несовпадение HTTPAddr | получили %v, ожидали %v",
			cfg.HTTPAddr,
			":8080",
		)
	}

	if cfg.ReadTimeout != 10*time.Second {
		t.Errorf(
			"несовпадение времени таймаута от ReadTimeout | получили %v, ожидали %v",
			cfg.ReadTimeout,
			10*time.Second,
		)
	}

	if cfg.ReadHeaderTimeout != 5*time.Second {
		t.Errorf(
			"несовпадение времени таймаута от ReadHeaderTimeout | получили %v, ожидали %v",
			cfg.ReadHeaderTimeout,
			5*time.Second,
		)
	}

	if cfg.WriteTimeout != 10*time.Second {
		t.Errorf(
			"несовпадение времени таймаута от WriteTimeout | получили %v, ожидали %v",
			cfg.WriteTimeout,
			10*time.Second,
		)
	}

	if cfg.IdleTimeout != time.Minute {
		t.Errorf(
			"несовпадение времени таймаута от IdleTimeout | получили %v, ожидали %v",
			cfg.IdleTimeout,
			60*time.Second,
		)
	}

	if cfg.ShutdownTimeout != 10*time.Second {
		t.Errorf(
			"несовпадение времени таймаута от ShutdownTimeout | получили %v, ожидали %v",
			cfg.ShutdownTimeout,
			10*time.Second,
		)
	}
}
