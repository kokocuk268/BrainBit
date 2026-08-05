package domain

import (
	"errors"
)

var (
	ErrUserNotFound       = errors.New("ошибка пользователь не найден")
	ErrEmailAlreadyExists = errors.New("ошибка такой Email уже существует")
)
