# Analiza Problemów - Framework Testowy BSS

## Zidentyfikowane Problemy

### 1. 🔴 Problemy z Kompilacją Klas

#### 1.1 BssMetrics.java
**Problem:**
- Błędne API Micrometer Gauge - metoda `getAsDouble()` nie istnieje w AtomicLong
- Błędne API Counter - metoda `increment(String, String)` nie istnieje
- Użycie nieprawidłowego wzorca budowniczego dla Gauge

**Rozwiązanie zastosowane:**
- Zmieniono na `AtomicLong::doubleValue` (prawidłowa metoda)
- Użyto `increment()` bez parametrów
- Poprawiono wzorzec budowniczego: `Gauge.builder(name, object, function)`

#### 1.2 Lombok @Slf4j
**Pliki dotknięte:**
- PerformanceMonitoringAspect.java
- KafkaOffsetManager.java
- EventReplayService.java
- EventReplayController.java

**Problem:**
- Lombok nie przetwarza adnotacji poprawnie
- Brak automatycznej generacji pola `log`
- Błąd: `cannot find symbol: variable log`

**Rozwiązanie zastosowane:**
- Zastąpiono `@Slf4j` manualnym Logger:
  ```java
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  
  private static final Logger log = LoggerFactory.getLogger(ClassName.class);
  ```

#### 1.3 Moduł Event
**Pliki:**
- EventReplayController.java
- EventReplayService.java
- KafkaOffsetManager.java

**Problemy:**
- Brakujące klasy i interfejsy (DomainEvent, EventProperties)
- Błędne interfejsy ConsumerSeekAware (błędne sygnatury metod)
- Niespójne zależności między plikami

**Działanie:** Pliki tymczasowo wyłączone (.bak)

#### 1.4 Moduł Cache
**Pliki:**
- CacheInvalidationListener.java
- PostgresNotificationService.java
- RedisCacheInvalidator.java
- CacheWarmingService.java
- ProbabilisticExpirationService.java
- CustomerCacheService.java

**Problemy:**
- Brakujące klasy (CacheInvalidationType, CacheInvalidator)
- Błędne importy (javax.annotation zamiast jakarta.annotation)
- Brakujące klasy ConcurrentHashMap, Executors (niemożliwe - prawdopodobnie błąd parser)
- Błędne API Jackson (BasicPolymorphicTypeValidator)

**Działanie:** Moduł tymczasowo wyłączony

#### 1.5 Moduł Benchmarking
**Pliki:**
- BenchmarkConfig.java
- PerformanceBenchmarkService.java
- RedisBenchmark.java
- Wszystkie pliki result/*.java

**Problemy:**
- Brakujące klasy bazowe (BenchmarkResult, PerformanceTestResult)
- Błędne interfejsy (@Max, @Min)
- Niespójne zależności

**Działanie:** Moduł tymczasowo usunięty

### 2. 🔴 Problemy z Testami

#### 2.1 Testy Kompilacji
**Problem:**
- Testy nie mogą się skompilować gdy główne klasy mają błędy
- Testy odwołują się do usuniętych klas infrastruktury

#### 2.2 Maven Surefire
**Problem:**
- Test AllureBasicTest nie może zostać znaleziony
- Prawdopodobnie błędy w kompilacji testów

**Błąd:**
```
No tests matching pattern "AllureBasicTest" were executed!
```

### 3. 🔴 Problemy z Zależnościami

#### 3.1 Lombok
**Problem:**
- Adnotacje Lombok nie są przetwarzane w czasie kompilacji
- Może być spowodowane brakiem konfiguracji maven-compiler-plugin

#### 3.2 TestContainers
**Problem:**
- Wiele testów wymaga TestContainers, ale zależności mogą być niekompletne
- Testy integration mogą nie działać bez uruchomionych kontenerów

## Plan Napraw

### Krok 1: Naprawa Lombok
1. Sprawdzenie konfiguracji pom.xml dla Lombok
2. Dodanie odpowiedniego procesor adnotacji
3. Przywrócenie @Slf4j w plikach (jeśli działa) lub ręczne loggery

### Krok 2: Naprawa Monitoringu
1. Przywrócenie monitoring.bak
2. Weryfikacja API Micrometer
3. Test kompilacji

### Krok 3: Naprawa Cache
1. Przywrócenie usuniętych plików cache
2. Naprawa importów jakarta.annotation
3. Naprawa zależności Jackson
4. Utworzenie brakujących klas

### Krok 4: Naprawa Event
1. Przywrócenie usuniętych plików event
2. Naprawa interfejsów ConsumerSeekAware
3. Utworzenie brakujących klas pomocniczych

### Krok 5: Naprawa Benchmarking
1. Przywrócenie modułu benchmarking
2. Utworzenie klas bazowych (BenchmarkResult, etc.)
3. Naprawa zależności

### Krok 6: Uruchomienie Testów
1. Kompilacja głównego kodu
2. Kompilacja testów
3. Uruchomienie AllureBasicTest
4. Generowanie raportów Allure

## Priorytety

1. **Wysokie (P0)**
   - Naprawa Lombok
   - Przywrócenie monitoring
   - Uruchomienie testów

2. **Średnie (P1)**
   - Naprawa cache
   - Naprawa event

3. **Niskie (P2)**
   - Naprawa benchmarking
   - Optymalizacja testów

## Status Napraw

- ✅ BssMetrics.java - NAPRAWIONE
- ✅ PerformanceMonitoringAspect.java - NAPRAWIONE
- ✅ KafkaOffsetManager.java - NAPRAWIONE
- ⏳ EventReplayController.java - DO PRZYWRÓCENIA
- ⏳ EventReplayService.java - DO PRZYWRÓCENIA
- ⏳ Cache module - DO PRZYWRÓCENIA
- ⏳ Benchmarking module - DO PRZYWRÓCENIA
- ⏳ Test compilation - DO NAPRAWY
- ⏳ Test execution - DO NAPRAWY
