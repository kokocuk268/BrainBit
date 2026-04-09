# BrainFocus - Руководство по сборке и запуску

## Описание проекта

BrainFocus — это Android-приложение, которое подключается к гарнитуре BrainBit EEG для анализа концентрации пользователя и использует её для управления сложностью игры.

## Требования для сборки

- Java JDK 21 (или совместимая версия)
- Android SDK (API 34)
- Gradle 8.9+
- Android Studio (рекомендуется)

## Инструкции по сборке

### Через командную строку

1. Укажите путь к Java 21:
```bash
set JAVA_HOME=C:\hometask\jdk21\jdk-21.0.10+7
set ANDROID_HOME=C:\Users\<ваш-пользователь>\AppData\Local\Android\Sdk
```

2. Соберите debug APK:
```bash
gradlew.bat assembleDebug
```

3. APK будет находиться здесь: `app\build\outputs\apk\debug\app-debug.apk`

### Через Android Studio

1. Откройте Android Studio
2. File -> Open -> Выберите папку `hometask`
3. Дождитесь завершения синхронизации Gradle
4. Build -> Make Project (Ctrl+F9)
5. Run -> Run 'app' (Shift+F10)

## Установка на устройство

1. Включите отладку по USB на Android-устройстве
2. Подключите устройство через USB
3. Установите APK:
```bash
adb install app\build\outputs\apk\debug\app-debug.apk
```

## Структура проекта

```
app/
├── src/main/
│   ├── java/com/brainfocus/app/
│   │   ├── brainbit/           # Интеграция с BrainBit SDK
│   │   │   ├── BrainBitManager.kt
│   │   │   ├── ConcentrationProcessor.kt
│   │   │   ├── ConnectionState.kt
│   │   │   └── MockConcentrationGenerator.kt
│   │   ├── game/               # Игровой движок
│   │   │   ├── GameView.kt
│   │   │   └── models/
│   │   ├── ui/                # Компоненты интерфейса
│   │   │   ├── MainActivity.kt
│   │   │   ├── connection/
│   │   │   ├── game/
│   │   │   └── results/
│   │   └── utils/             # Утилиты
│   │       ├── PermissionHelper.kt
│   │       └── SensorHelper.kt
│   └── res/                   # Ресурсы
└── build.gradle.kts
```

## Игровая механика

- **Управление игроком**: Наклоняйте устройство влево/вправо
- **Влияние концентрации**:
  - Высокая (70-100%): Препятствия падают на 50% медленнее
  - Средняя (30-70%): Препятствия падают на 75% медленнее
  - Низкая (0-30%): Препятствия падают с обычной скоростью
- **Подсчёт очков**: На основе времени выживания
- **Конец игры**: При столкновении с препятствием

## Режим тестирования

Приложение поддерживает **режим тестирования**, который позволяет играть без реальной гарнитуры BrainBit:

1. На экране подключения включите переключатель "Тестовый режим"
2. Нажмите "Начать игру"
3. Концентрация будет симулироваться плавными волнами (0.2-0.8)

## Интеграция с реальным BrainBit SDK

Для подключения реального устройства BrainBit:
1. Добавьте зависимость: `implementation("com.github.BrainbitLLC:neurosdk2:1.0.6.34")`
2. Реализуйте BrainBitManager с использованием SDK API
3. Обновите файлы ConnectionState.kt и BrainBitManager.kt

## Необходимые разрешения

- BLUETOOTH
- BLUETOOTH_ADMIN
- ACCESS_FINE_LOCATION
- BLUETOOTH_SCAN (Android 12+)
- BLUETOOTH_CONNECT (Android 12+)

### Bluetooth не работает
Проверьте, что:
- Разрешения Bluetooth предоставлены
- Bluetooth включён на устройстве
- Геолокация включена (требуется для BLE на Android 10+)
