# ⛵ Group Costs Split App (Kotlin Multiplatform & Spring Boot)

Nowoczesna aplikacja webowa do **podziału kosztów grupowych** (inspirowana Splitwise), zrealizowana w ujednoliconym ekosystemie języka Kotlin:
* **Backend:** Spring Boot (Kotlin), Spring Security (stateless JWT), Spring Data JPA, baza PostgreSQL.
* **Frontend:** Kotlin/Wasm & Compose Multiplatform (Material 3 z zaawansowaną, szklaną estetyką *Glassmorphism*).
* **Baza Danych:** PostgreSQL (z automatyczną inicjalizacją przykładowych danych).

---

## 🚀 Szybki Start (Docker Compose) - Rekomendowany

Dzięki pełnej konteneryzacji całego projektu, możesz uruchomić kompletną aplikację (bazę danych, serwer API oraz interfejs webowy) **jednym poleceniem**, bez potrzeby instalowania Javy, Gradle czy PostgreSQL na swoim systemie!

### Wymagania:
* Zainstalowany [Docker Desktop](https://www.docker.com/products/docker-desktop/) oraz Docker Compose.

### Instrukcja Uruchomienia:

1. Otwórz terminal w głównym folderze projektu i uruchom komendę:
   ```bash
   docker compose up --build -d
   ```
   *(Docker pobierze niezbędne obrazy, skompiluje backend Spring Boot do pliku JAR, zbuduje statyczną dystrybucję Kotlin/Wasm i osadzi ją na serwerze Nginx w tle)*.

2. Po zakończeniu budowania, aplikacje będą dostępne pod adresami:
   * **Interfejs Webowy (Frontend Wasm):** 👉 [http://localhost:8081](http://localhost:8081)
   * **API Backendowe (REST):** [http://localhost:8080](http://localhost:8080)
   * **Baza danych PostgreSQL:** port `5432` (dane dostępowe: `postgres`/`strongpassword123`)

3. Aby zatrzymać wszystkie kontenery, wpisz:
   ```bash
   docker compose down
   ```

---

## 🧪 Przykładowe Dane do Testów (Baza nie jest pusta!)

Przy pierwszym uruchomieniu projektu baza PostgreSQL jest automatycznie uzupełniana o zestaw zbalansowanych danych testowych przez nasz komponent `DatabaseInitializer`. Możesz od razu zalogować się na jedno z kont:

* **Loginy:** `janek`, `adam`, `kasia`
* **Hasło dla wszystkich:** `password123`

### Co znajduje się w bazie po starcie?
* **Grupa 1:** *Wyjazd na Mazury ⛵* (członkowie: Janek, Adam, Kasia).
  * Wydatek: "Paliwo do auta" (180 PLN, zapłacił Janek, podzielone po równo).
  * Wydatek: "Zakupy w Biedronce" (120 PLN, zapłacił Adam, podzielone po równo).
  * Wydatek: "Wypożyczenie łódki" (90 PLN, zapłaciła Kasia, podzielone po równo).
  * Rozliczenie: Adam oddał Jankowi 20 PLN (spłata zatwierdzona przez Janka).
  * **Status Bilanse:** Kasia wisi Jankowi 30 PLN oraz Kasia wisi Adamowi 10 PLN (wyliczone przez algorytm upraszczania długów).
* **Grupa 2:** *Wspólne Mieszkanie 🏠* (członkowie: Janek, Adam).
  * Wydatek: "Internet światłowodowy" (80 PLN, zapłacił Adam, podzielone po równo).

---

## 🛠️ Alternatywne Uruchomienie Deweloperskie (Manualne)

Jeśli chcesz modyfikować kod w czasie rzeczywistym z funkcją Hot-Reload:

### 1. Uruchomienie Bazy w Dockerze
```bash
docker compose up db -d
```

### 2. Uruchomienie Backendu (Spring Boot)
Przejdź do folderu `backend` i wpisz:
```bash
./gradlew bootRun
```
*Port: `8080`*

### 3. Uruchomienie Frontendu (Kotlin/Wasm Dev Server)
Przejdź do folderu `frontend` i wpisz:
```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```
*Port: `8081` (lub losowy wolny port wyświetlony na dole logów)*

---

## 📐 Struktura Projektu

```
costs-split-compose/
├── docker-compose.yml (Główna orkiestracja db, backendu i frontendu)
├── README.md (Ten przewodnik)
├── backend/ (Moduł Spring Boot)
│   ├── Dockerfile (Budowanie JAR i uruchomienie)
│   └── src/main/kotlin/com/splitcosts/backend/
│       ├── model/ (Encje JPA: User, Group, Expense, Split, Settlement)
│       ├── repository/ (Interfejsy Spring Data JPA)
│       ├── security/ (Spring Security z JWT oraz filtry autoryzacji)
│       ├── service/ (Usługi biznesowe: kalkulator bilansów i upraszczania długów)
│       └── controller/ (Kontrolery REST API oraz Global Exception Handler)
└── frontend/ (Projekt Kotlin Multiplatform & Compose Wasm)
    ├── Dockerfile (Zbudowanie dystrybucji Wasm i uruchomienie Nginx)
    ├── shared/ (Współdzielone ekrany Compose, UI i wywołania Ktor)
    │   └── src/commonMain/kotlin/org/example/frontend/
    │       ├── api/ (Serwis ApiClient oparty o Ktor z dołączaniem JWT)
    │       ├── ui/ (System Theme z Glassmorphism oraz definicje ekranów)
    │       └── App.kt (Główny kontroler nawigacji)
    └── webApp/ (Punkt wejścia dedykowany przeglądarce)
```

---

## 💎 Kluczowe Algorytmy i Funkcje

* **Algorytm Upraszczania Długów (Simplify Debts):** System automatycznie segreguje salda użytkowników na dłużników i wierzycieli osobno dla każdej waluty (PLN, EUR, USD itd.). Następnie zachłannie paruje największego dłużnika z największym wierzycielem i generuje **minimalną liczbę przelewów** potrzebnych do pełnego rozliczenia grupy.
* **Korekta Groszowa (Equal Split Adjustment):** Podczas dzielenia kwoty po równo (np. 100 PLN na 3 osoby), system automatycznie przydziela groszowe różnice z zaokrągleń (33.33, 33.33, 33.34) tak, aby suma splitów zgadzała się z kwotą wejściową co do grosza.
* **Stateless Security (JWT):** Zaimplementowano bezstanową obsługę tokenów z wygasaniem. Każde żądanie z poziomu Ktor Clienta na frontendzie ma automatycznie wstrzykiwany nagłówek autoryzacyjny `Authorization: Bearer <token>`.
* **Zatwierdzanie spłat:** Każda transakcja spłaty długu zgłoszona przez dłużnika oczekuje na kliknięcie przycisku "Potwierdź" przez wierzyciela. Dopiero po zatwierdzeniu odbioru gotówki, bilans netto w grupie ulega korekcie.
