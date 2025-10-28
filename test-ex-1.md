# Test-Ex-1 – Ćwiczenia z testowania Spring Boot (Java 21)

Poniżej znajdziesz osiem krótkich zadań. Każde zadanie ma gotowy przykład testu, który już znajduje się w repozytorium, oraz opis krok po kroku napisany tak, by zrozumiała go osoba zaczynająca przygodę z Javą. Scenariusze łączą przypadki pozytywne (kiedy wszystko działa) oraz negatywne (kiedy coś idzie nie tak). Kod źródłowy testów możesz znaleźć odpowiednio w plikach `backend/src/test/java/com/droid/bss/application/HelloServiceTest.java` oraz `backend/src/test/java/com/droid/bss/api/HelloControllerWebTest.java`.

> **Jak uruchomić testy:** w katalogu `backend/` wykonaj `JAVA_HOME=$HOME/.local/liberica/jdk-21.0.8 ../mvnw test`.

---

## Zadanie 1 – Pozytywnie: tworzymy powitanie z rolami

**Cel:** Sprawdzić, czy `HelloService` buduje poprawną wiadomość i przepisuje role użytkownika.

### Kod testu
```java
@Test
@DisplayName("should build greeting message with subject and roles (positive scenario)")
void shouldBuildGreetingWithSubjectAndRoles() {
    Jwt principal = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("alice")
        .claim("realm_access.roles", List.of("student", "mentor"))
        .build();

    HelloResponse response = service.greet(principal);

    assertThat(response.message()).isEqualTo("Hello, alice");
    assertThat(response.subject()).isEqualTo("alice");
    assertThat(response.roles()).containsExactly("student", "mentor");
}
```

### Wyjaśnienie linia po linii
- `@Test` – mówimy JUnitowi, że ta metoda to test.
- `@DisplayName(...)` – przyjazna nazwa testu wyświetlana w raportach.
- `Jwt principal = ...` – budujemy atrapę tokenu logowania: nadajemy mu tekstową wartość (`"token"`), nagłówek (`alg`), ustawiamy użytkownika (`subject("alice")`) i dopisujemy listę ról.
- `.build();` – kończymy budowanie tokenu i zapisujemy go w zmiennej `principal`.
- `HelloResponse response = service.greet(principal);` – uruchamiamy metodę, którą testujemy.
- `assertThat(...)` – trzy kolejne linie sprawdzają, czy wynik zawiera oczekany tekst, identyfikator użytkownika i dokładnie dwie podane role.

### Samodzielne modyfikacje
- Zmień imię na inne i zobacz, czy test nadal przechodzi.
- Dodaj trzecią rolę i dopasuj asercję.

---

## Zadanie 2 – Pozytywnie: pusta lista ról nie psuje wyniku

**Cel:** Upewnić się, że brak ról w tokenie nie powoduje błędów.

### Kod testu
```java
@Test
@DisplayName("should return empty roles when claim exists but list is empty (positive resilience)")
void shouldReturnEmptyRolesWhenClaimEmptyList() {
    Jwt principal = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("bob")
        .claim("realm_access.roles", List.of())
        .build();

    HelloResponse response = service.greet(principal);

    assertThat(response.roles()).isEmpty();
    assertThat(response.message()).isEqualTo("Hello, bob");
}
```

### Wyjaśnienie
- Budujemy token identycznie jak wcześniej, ale przekazujemy pustą listę ról (`List.of()`).
- Wywołujemy metodę `greet` i sprawdzamy, że zwrócone role to pusta lista, a przywitanie nadal zawiera imię użytkownika.

### Samodzielne modyfikacje
- Spróbuj zmienić `List.of()` na `Collections.emptyList()` i uruchom test ponownie.

---

## Zadanie 3 – Negatywnie: brak claimu z rolami

**Cel:** Zobaczyć, co się stanie, gdy token w ogóle nie zawiera informacji o rolach.

### Kod testu
```java
@Test
@DisplayName("should return empty roles when claim is missing (negative input)")
void shouldReturnEmptyRolesWhenClaimMissing() {
    Jwt principal = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("carol")
        .build();

    HelloResponse response = service.greet(principal);

    assertThat(response.roles()).isEmpty();
    assertThat(response.message()).isEqualTo("Hello, carol");
}
```

### Wyjaśnienie
- Token powstaje bez dodatkowego claimu `realm_access.roles`.
- Metoda `greet` radzi sobie z tym – dostajemy pustą listę ról i poprawne przywitanie.

### Samodzielne modyfikacje
- Usuń `subject("carol")` i obserwuj, co zwróci metoda (test powinien się wyłożyć, bo wiadomość stanie się `"Hello, null"`).

---

## Zadanie 4 – Negatywnie: brak tokenu -> wyjątek

**Cel:** Sprawdzić, czy serwis jasno zgłasza błąd, kiedy ktoś zapomni podać tokenu.

### Kod testu
```java
@Test
@DisplayName("should throw NullPointerException when principal is null (negative scenario)")
void shouldThrowWhenPrincipalNull() {
    assertThrows(NullPointerException.class, () -> service.greet(null));
}
```

### Wyjaśnienie
- `assertThrows` oczekuje, że przekazany fragment kodu zakończy się podanym wyjątkiem.
- Tutaj mówimy: „jeśli przekażę `null` zamiast tokenu, to serwis ma rzucić `NullPointerException`”.

### Samodzielne modyfikacje
- Spróbuj zmienić typ wyjątku na `IllegalArgumentException` – zobaczysz, że test zacznie się wywalać, bo oczekiwany typ nie pasuje do rzeczywistego błędu.

---

## Zadanie 5 – Pozytywnie: kontroler zwraca JSON przy poprawnym JWT

**Cel:** Na poziomie kontrolera upewnić się, że zalogowany użytkownik dostaje JSON-a z danymi.

### Kod testu
```java
@Test
@DisplayName("should return greeting JSON when request carries JWT (positive)")
void shouldReturnGreetingWhenAuthenticated() throws Exception {
    when(helloService.greet(any())).thenReturn(new HelloResponse("Hello, dana", "dana", List.of("student")));

    mockMvc.perform(get("/api/hello")
            .with(jwt().jwt(builder -> builder
                .subject("dana")
                .claim("realm_access.roles", List.of("student")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Hello, dana"))
        .andExpect(jsonPath("$.subject").value("dana"))
        .andExpect(jsonPath("$.roles[0]").value("student"));
}
```

### Wyjaśnienie
- `when(...).thenReturn(...)` – prosimy atrapę serwisu, żeby zwróciła gotową odpowiedź.
- `mockMvc.perform(get(...).with(jwt()...))` – symulujemy żądanie HTTP GET, do którego dorzucamy sztuczny token JWT (podstawia go biblioteka testowa).
- `.andExpect(status().isOk())` – oczekujemy kodu 200.
- Kolejne `.andExpect` sprawdzają pola z JSON-a (`message`, `subject`, `roles[0]`).

### Samodzielne modyfikacje
- Dodaj drugą rolę w odpowiedzi serwisu i rozszerz asercje o `roles[1]`.

---

## Zadanie 6 – Negatywnie: brak JWT oznacza 401

**Cel:** Upewnić się, że kontroler nie przepuści niezalogowanego użytkownika.

### Kod testu
```java
@Test
@DisplayName("should reject request without JWT (negative)")
void shouldReturnUnauthorizedWhenTokenMissing() throws Exception {
    mockMvc.perform(get("/api/hello"))
        .andExpect(status().isUnauthorized());
}
```

### Wyjaśnienie
- Wysyłamy żądanie GET bez tokenu.
- Spodziewamy się kodu 401 – to standardowy komunikat „musisz się zalogować”.

### Samodzielne modyfikacje
- Zamień `get` na `post` i zobacz, jaki kod zwróci aplikacja (powinien być 405, czyli „metoda niedozwolona”).

---

## Zadanie 7 – Pozytywnie: pusta lista ról w odpowiedzi kontrolera

**Cel:** Sprawdzić, czy kontroler poprawnie serializuje pustą listę ról.

### Kod testu
```java
@Test
@DisplayName("should return empty roles array when service reports none (positive resilience)")
void shouldReturnEmptyRolesArray() throws Exception {
    when(helloService.greet(any())).thenReturn(new HelloResponse("Hello, finn", "finn", List.of()));

    mockMvc.perform(get("/api/hello")
            .with(jwt().jwt(builder -> builder.subject("finn"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles").isArray())
        .andExpect(jsonPath("$.roles").isEmpty());
}
```

### Wyjaśnienie
- Mock serwisu zwraca odpowiedź z pustą listą ról.
- W JSON-ie `$.roles` musi być tablicą i ma być pusta – dokładnie to sprawdzają dwie ostatnie asercje.

### Samodzielne modyfikacje
- Spróbuj ustawić `subject` na `null` i zobacz, że JSON nadal przejdzie (otrzymasz `"subject": null`).

---

## Zadanie 8 – Negatywnie: zła metoda HTTP kończy się 405

**Cel:** Zademonstrować, że endpoint przyjmuje tylko GET.

### Kod testu
```java
@Test
@DisplayName("should reject unsupported HTTP method (negative)")
void shouldReturnMethodNotAllowedOnPost() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post("/api/hello")
            .with(jwt().jwt(builder -> builder.subject("grace"))))
        .andExpect(status().isMethodNotAllowed());
}
```

-### Wyjaśnienie
- `MockMvcRequestBuilders.post("/api/hello")` wysyła żądanie POST (to skrót z biblioteki Springa; używamy go z pełną nazwą klasy, żeby było jasne skąd pochodzi).
- Aplikacja odpowiada 405 – „metoda HTTP nieobsługiwana dla tego adresu”.

### Samodzielne modyfikacje
- Zastąp `post` metodą `delete` i sprawdź, że wynik jest identyczny.

---

## Podsumowanie
- Testy 1–4 to czyste testy jednostkowe – nie uruchamiamy całej aplikacji, tylko klasę `HelloService`.
- Testy 5–8 działają na warstwie kontrolera HTTP (`MockMvc`) i korzystają z atrap serwisu oraz tokenów JWT dostarczonych przez bibliotekę testową.
- Po każdej zmianie warto wykonać `./mvnw -f backend/pom.xml test`, żeby upewnić się, że wszystkie scenariusze nadal przechodzą.
