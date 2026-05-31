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

## 🧪 Scenariusze Testowe i Dane Początkowe (Baza nie jest pusta!)

Przy pierwszym uruchomieniu projektu baza danych PostgreSQL jest automatycznie zasiedlana spójnym zestawem danych testowych za pomocą komponentu `DatabaseInitializer`. Dzięki temu aplikacja od razu po uruchomieniu prezentuje zaawansowane zależności finansowe, bilanse oraz uproszczone długi.

Do zalogowania służą **trzy dedykowane konta testowe** (hasło dla wszystkich to: **`password123`**):

### 🔑 Wykaz Kont Testowych i Ich Rola w Scenariuszach:

1. **`janek` (Login: `janek`) - GŁÓWNY WIERZYCIEL GRUPY "MAZURY" & ADMINISTRATOR**
   * **Rola:** Administrator grupy *Wyjazd na Mazury*. Osoba o największym saldzie dodatnim.
   * **Scenariusz testowy:** Zaloguj się jako `janek`, aby zweryfikować **panel zarządzania grupą** (opcja zapraszania i usuwania członków, usuwanie całej grupy) oraz **moduł akceptacji spłat** (Janek widzi spłaty oczekujące na jego zatwierdzenie i jako jedyny ma uprawnienia do kliknięcia przycisku "Potwierdź").

2. **`adam` (Login: `adam`) - DEBTER / WIERZYCIEL POŚREDNI & ADMINISTRATOR "MIESZKANIA"**
   * **Rola:** Płatnik części wydatków w grupie *Wyjazd na Mazury* (zakupy), a także administrator i jedyny płatnik w grupie *Wspólne Mieszkanie*.
   * **Scenariusz testowy:** Zaloguj się jako `adam`, aby zobaczyć, jak system poprawnie oblicza salda pośrednie (Adam ma mały dług u Janka, ale Kasia wisi mu pieniądze). Zobaczysz też pełny bilans i historię w grupie *Wspólne Mieszkanie*.

3. **`kasia` (Login: `kasia`) - GŁÓWNY DŁUŻNIK GRUPY "MAZURY"**
   * **Rola:** Osoba o najwyższym długu w grupie *Wyjazd na Mazury* (wydała najmniej, wisi pieniądze Jankowi i Adamowi).
   * **Scenariusz testowy:** Zaloguj się jako `kasia`, aby zobaczyć **czerwony pasek ujemnego bilansu** oraz **moduł szybkich spłat długów**. Kliknij przycisk **"Rozlicz"** przy sugerowanym długu wobec Janka (30 PLN), aby automatycznie wygenerować transakcję spłaty. Po wysłaniu spłaty zobaczysz, że transakcja ma status *Oczekuje* na potwierdzenie przez Janka.

---

### 📊 Dokładny Stan Początkowy Bazy Danych po Starcie:

* **Grupa 1: "Wyjazd na Mazury ⛵"** (Administrator: Janek, Członkowie: Janek, Adam, Kasia)
  * **Wydatki (koszt 180 + 120 + 90 = 390 PLN, czyli 130 PLN na osobę):**
    * *Paliwo do auta:* 180 PLN (opłacił **Janek**, podział równy po 60 PLN).
    * *Zakupy w Biedronce:* 120 PLN (opłacił **Adam**, podział równy po 40 PLN).
    * *Wypożyczenie łódki:* 90 PLN (opłaciła **Kasia**, podział równy po 30 PLN).
  * **Zatwierdzone spłaty:** Adam przelał Jankowi 20 PLN (spłata zaakceptowana przez Janka).
  * **Wpływ na bilanse netto:**
    * **Janek:** `+30.00 PLN` (Początkowy bilans `+50.00` pomniejszony o `20.00` otrzymanej spłaty).
    * **Adam:** `+10.00 PLN` (Początkowy bilans `-10.00` powiększony o `20.00` wykonanej spłaty).
    * **Kasia:** `-40.00 PLN` (Brak zarejestrowanych spłat).
  * **Uproszczona sieć długów (Simplify Debts):**
    * Kasia wisi **Jankowi**: `30.00 PLN`
    * Kasia wisi **Adamowi**: `10.00 PLN`
    *(Algorytm idealnie zredukował długi do zaledwie 2 transakcji!)*

* **Grupa 2: "Wspólne Mieszkanie 🏠"** (Administrator: Adam, Członkowie: Janek, Adam)
  * **Wydatki:**
    * *Internet światłowodowy:* 80 PLN (opłacił **Adam**, podział równy po 40 PLN).
  * **Uproszczona sieć długów:**
    * Janek wisi **Adamowi**: `40.00 PLN`

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
