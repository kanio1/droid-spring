# Kompletna Lista Problemów - BSS Test Framework

## Status Napraw (na 2025-11-07 09:55)

### ✅ NAPRAWIONE
1. **Lombok dependency** - DODANO do pom.xml
2. **BssMetrics.java** - NAPRAWIONE (API Micrometer)
3. **PerformanceMonitoringAspect.java** - NAPRAWIONE (manual Logger)
4. **KafkaOffsetManager.java** - NAPRAWIONE (manual Logger)
5. **EventReplayController.java** - NAPRAWIONE (manual Logger)
6. **EventReplayService.java** - NAPRAWIONE (manual Logger)
7. **Duplikaty monitoring** - USUNIĘTE

### ⏳ DO NAPRAWY

#### 1. Event Module
**Problemy:**
- EventReplayController.java: Brakujące klasy (Operation, ReplayRequest)
- KafkaOffsetManager.java: Błędne interfejsy ConsumerSeekAware
- Brakujące klasy: Callback, SeekPosition

**Pliki:**
- `/infrastructure/event/EventReplayController.java` - linie 40, 61, 81, 101, 119, 133, 152, 172, 193
- `/infrastructure/event/KafkaOffsetManager.java` - linie 85, 170, 194

**Czas naprawy:** ~30 min

#### 2. OpenTelemetry Module
**Problemy:**
- Brak zależności: `io.opentelemetry.exporter.jaeger`
- Błędne klasy i metody

**Pliki:**
- `/infrastructure/tracing/OpenTelemetryConfig.java`

**Czas naprawy:** ~15 min

#### 3. Cache Module
**Problemy:**
- Usunięte pliki cache
- Brak klas: CacheInvalidationType, CacheInvalidator
- Błędne importy: javax.annotation vs jakarta.annotation
- Błędne API Jackson: BasicPolymorphicTypeValidator
- Brakujące klasy utility: Executors, ConcurrentHashMap

**Pliki do przywrócenia:**
- CacheInvalidationListener.java.bak
- PostgresNotificationService.java.bak
- RedisCacheInvalidator.java.bak
- CacheWarmingService.java.bak
- ProbabilisticExpirationService.java.bak
- CustomerCacheService.java.bak

**Czas naprawy:** ~60 min

#### 4. Benchmarking Module
**Problemy:**
- Usunięte pliki benchmarking
- Brak klas bazowych: BenchmarkResult, PerformanceTestResult
- Błędne interfejsy: @Max, @Min
- Niespójne zależności

**Pliki do przywrócenia:**
- Wszystkie pliki z `/infrastructure/benchmarking/`

**Czas naprawy:** ~45 min

#### 5. Test Compilation
**Problemy:**
- Testy odwołują się do usuniętych klas
- TestContainers zależności mogą być niepełne
- AllureBasicTest nie może zostać znaleziony przez Maven

**Czas naprawy:** ~30 min

## Szacowany Całkowity Czas Napraw: 3-4 godziny

## Alternatywne Podejście

Zamiast naprawiać wszystkie moduły, można:

1. **Oznaczyć moduły jako nieaktywne** (@Disabled w testach, @ConditionalOnProperty w konfiguracji)
2. **Skupić się tylko na testach** które działają
3. **Wygenerować raporty Allure** z działających testów
4. **Dokumentować** że inne moduły są w trakcie refaktoryzacji

## Zalecane Priorytety

### P0 (Krytyczne - 1h)
1. Naprawa Event Module
2. Naprawa OpenTelemetry
3. Test kompilacji i uruchomienie Allure

### P1 (Ważne - 2h)
1. Przywrócenie Cache Module (bez pełnej funkcjonalności)
2. Naprawa testów
3. Uruchomienie pełnego test suite

### P2 (Niskie - 1h+)
1. Benchmarking Module
2. Optymalizacje
3. Dokumentacja

## Rekomendacja

**Wykonaj tylko P0** aby:
- Framework Allure mógł generować raporty
- Kod się kompilował
- Testy mogły być uruchomione

Pozostałe moduły można naprawiać w kolejnych iteracjach.
