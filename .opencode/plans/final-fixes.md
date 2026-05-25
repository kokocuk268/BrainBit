# Final Fixes Plan

## 🔴 Critical — GameView memory visibility

### 1. GameView.kt:46 — @Volatile on currentTilt
```kotlin
@Volatile
private var currentTilt = 0f
```
Reason: written from Dispatchers.Main (coroutine), read from GameThread. Without @Volatile the write may never be visible to the game thread → player can't steer.

### 2. GameView.kt:146 — @Volatile on GameThread.running
```kotlin
@Volatile
var running = false
```
Reason: `running = false` set from main thread in `stopGame()`, read in `while(running)` inside GameThread. Without @Volatile the game thread may loop forever → `join()` hangs permanently, thread + memory leak.

---

## 🟡 Medium — BrainBitManager robustness

### 3. BrainBitManager.kt:81 — clearBleCache SecurityException on API 34+
Add `catch (e: NoSuchMethodException)` and `catch (e: SecurityException)` before the generic catch. The method uses reflection on a private API which is blocked on Android 14+.

### 4. BrainBitManager.kt:386 — disconnect() skip resistance stop on error
Split into two try-catch blocks:
- First: `execCommand(StopSignal)` — if it fails, `stopResistanceTest()` still runs
- Second: `disconnect()` + `close()`

---

## 🟢 Low — ConcentrationProcessor cleanup

### 5. Remove dead field `sampleCount`
Only incremented, never read. Remove the field and its reset.

### 6. Replace `samples.map { (it - mean).pow(2) }.average()`
→ manual loop `for (v in samples) { val d = v - mean; sum += d*d }` then `avg = sum / size`. Avoids temporary list allocation.

### 7. Add `runningSum` for concentrationHistory
Same pattern as GameViewModel — track `concentrationSum` and use `concentrationSum / concentrationHistory.size` instead of `.average()` which iterates the whole list.

---

## Files to modify (3 files, 5 edits)
1. `app/src/main/java/com/brainfocus/app/game/GameView.kt` — 2 @Volatile additions
2. `app/src/main/java/com/brainfocus/app/brainbit/BrainBitManager.kt` — 2 error handling fixes
3. `app/src/main/java/com/brainfocus/app/brainbit/ConcentrationProcessor.kt` — 3 cleanup optimizations
