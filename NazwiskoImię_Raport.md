# DOKUMENTACJA PROJEKTOWA I RAPORT TECHNICZNY

---

## 1. KARTA INFORMACYJNA PROJEKTU (METRYCZKA)

### 1.1. Informacje o Autorze i Toku Studiów
* **Autor Projektu:** `[Wpisz Imię i Nazwisko]`
* **Numer Indeksu:** `[Wpisz Numer Indeksu]`
* **Kierunek Studiów:** `[Wpisz Kierunek Studiów, np. Informatyka]`
* **Specjalność:** `[Wpisz Specjalność, np. Inżynieria Oprogramowania]`
* **Tok Studiów:** Studia `[Stacjonarne / Niestacjonarne]`, Stopień `[I / II]`, Semestr `[Wpisz Semestr]`
* **Grupa Dziekańska:** `[Wpisz Numer Grupy]`
* **Rok Akademicki:** `2025/2026`

### 1.2. Informacje o Przedmiocie i Prowadzącym
* **Nazwa Przedmiotu:** `[Wpisz Nazwę Przedmiotu, np. Programowanie Aplikacji Webowych]`
* **Katedra / Instytut:** `[Wpisz Katedrę/Instytut, np. Katedra Informatyki]`
* **Wydział:** `[Wpisz Wydział, np. Wydział Elektrotechniki i Informatyki]`
* **Uczelnia:** `[Wpisz Nazwę Uczelni, np. Politechnika Lubelska]`
* **Prowadzący Projekt:** `[Wpisz Tytuł/Stopień Naukowy, Imię i Nazwisko Prowadzącego]`

---

## 2. TYTUŁ PROJEKTU
**Projekt i implementacja rozproszonego systemu ewidencjonowania, podziału oraz optymalizacji rozliczeń finansowych w grupach celowych**

---

## 3. CEL I ZAKRES PROJEKTU

Celem niniejszego projektu było zaprojektowanie, zaimplementowanie i przetestowanie wieloplatformowego, rozproszonego systemu webowego służącego do ewidencjonowania kosztów ponoszonych w ramach zamkniętych grup użytkowników oraz automatycznej optymalizacji sald wzajemnych. 

Zakres funkcjonalny zrealizowanego oprogramowania obejmuje:
1. **Bezpieczne uwierzytelnianie i autoryzację:** Wdrożenie bezstanowego mechanizmu autoryzacji bazującego na tokenach JWT (JSON Web Tokens) oraz szyfrowaniu jednokierunkowym haseł.
2. **Zarządzanie strukturą grup:** Możliwość definiowania grup celowych przez administratora, dynamiczne zapraszanie użytkowników (poprzez unikalny identyfikator lub adres e-mail) oraz autoryzację operacji modyfikujących strukturę grupy.
3. **Ewidencjonowanie transakcji kosztowych:** Rejestrowanie wydatków z uwzględnieniem kwoty, opisu, waluty transakcyjnej oraz płatnika.
4. **Wielowariantowy podział kosztów:**
   * **Podział proporcjonalny (równy):** Automatyczna alokacja kwoty na wszystkich członków grupy z algorytmiczną korektą groszową zaokrągleń numerycznych.
   * **Podział dyskretny (niestandardowy):** Ręczna alokacja określonych kwot na poszczególnych użytkowników z walidacją sumaryczną po stronie klienta oraz serwera.
5. **Algorytmiczną optymalizację sald (Simplify Debts):** Zastosowanie zachłannego algorytmu redukcji grafu transakcji w celu zminimalizowania liczby operacji finansowych niezbędnych do uregulowania wszystkich zobowiązań wewnątrz grupy.
6. **Rejestrację i weryfikację spłat (Settlements):** Wdrożenie dwufazowego protokołu rozliczeń (zgłoszenie spłaty przez dłużnika oraz formalne potwierdzenie odbioru środków przez wierzyciela).

---

## 4. ARCHITEKTURA TECHNICZNA I STOS TECHNOLOGICZNY

System został zaprojektowany w architekturze trójwarstwowej z wyraźnym podziałem na warstwę prezentacji (Client), warstwę logiki biznesowej (Application Server) oraz warstwę trwałości danych (Database Server).

```
┌────────────────────────────────────────────────────────┐
│             Warstwa Prezentacji (Frontend)              │
│       Kotlin/Wasm & Compose Multiplatform (UI)         │
│          Ktor Client (Komunikacja HTTP REST)           │
└──────────────────────────┬─────────────────────────────┘
                           │
                           │ Zapytania REST (JSON + JWT)
                           ▼
┌────────────────────────────────────────────────────────┐
│         Warstwa Logiki Biznesowej (Backend)            │
│         Spring Boot (Kotlin) & Spring Security         │
│       JPA / Hibernate ORM (Zarządzanie Stanem)         │
└──────────────────────────┬─────────────────────────────┘
                           │
                           │ SQL / Połączenie JDBC
                           ▼
┌────────────────────────────────────────────────────────┐
│             Warstwa Trwałości Danych                   │
│          Relacyjna Baza Danych PostgreSQL              │
└────────────────────────────────────────────────────────┘
```

### 4.1. Specyfikacja Technologiczna Warstwy Serwerowej (Backend)
* **Środowisko uruchomieniowe:** Java Virtual Machine (JVM 21 / JVM 25).
* **Szkielet aplikacyjny:** Spring Boot 4.0.6 (Kotlin DSL).
* **Zabezpieczenia i Kontrola Dostępu:** Spring Security. Bezstanowa autoryzacja zaimplementowana została przy użyciu tokenów JWT (biblioteka JJWT 0.12.6) generowanych z podpisem cyfrowym HMAC-SHA256. Hasła użytkowników są bezpiecznie haszowane za pomocą algorytmu BCrypt przed zapisem w bazie danych.
* **Trwałość danych:** Spring Data JPA (Hibernate 7.2.12) realizujący mapowanie obiektowo-relacyjne (ORM).
* **Sterownik bazy danych:** PostgreSQL JDBC Driver.

### 4.2. Specyfikacja Technologiczna Warstwy Klenckiej (Frontend)
* **Technologia renderowania:** Compose Multiplatform 1.11.0 (JetBrains) skompilowany do formatu WebAssembly (WasmJs) przy użyciu kompilatora Kotlin 2.3.21. Aplikacja jest renderowana bezpośrednio na elemencie Canvas przeglądarki, co zapewnia natywną wydajność.
* **Klient HTTP:** Ktor Client 3.0.1 (z silnikiem asynchronicznym dedykowanym dla środowisk JavaScript/Wasm) realizujący komunikację za pomocą metod GET, POST, DELETE.
* **Serializacja:** Kotlinx Serialization Json 1.7.3 służąca do deterministycznego parsowania obiektów DTO.
* **Serwer WWW:** Serwer Nginx (wersja stable-alpine) serwujący skompilowane zasoby statyczne (HTML, JS, CSS oraz Wasm) w kontenerze produkcyjnym.

### 4.3. Warstwa Danych i Konteneryzacja
* **Baza danych:** PostgreSQL 16 (Alpine) zapewniająca pełną spójność transakcyjną (ACID).
* **Konteneryzacja:** Docker & Docker Compose w celu standaryzacji środowiska deweloperskiego i produkcyjnego.

---

## 5. ZASTOSOWANE ALGORYTMY OPTYMALIZACYJNE I NUMERYCZNE

### 5.1. Algorytm Uproszczania Długów (Debt Simplification Algorithm)
W celu minimalizacji liczby transakcji finansowych koniecznych do rozliczenia grupy celowej, w serwisie `BalanceService` zaimplementowano zachłanny algorytm redukcji powiązań w grafie skierowanym.

#### Model matematyczny:
Niech $U$ będzie zbiorem użytkowników w grupie celowej. Dla każdego użytkownika $u \in U$ obliczany jest bilans netto wejściowy $B(u)$ jako:

$$B(u) = P(u) - C(u) + S_{in}(u) - S_{out}(u)$$

Gdzie:
* $P(u)$ – suma kwot wydatków opłaconych przez użytkownika $u$.
* $C(u)$ – suma kwot (splitów) przypisanych do użytkownika $u$ jako jego część kosztów.
* $S_{in}(u)$ – suma zatwierdzonych spłat, w których $u$ był dłużnikiem (kwota przekazana).
* $S_{out}(u)$ – suma zatwierdzonych spłat, w których $u$ był wierzycielem (kwota odebrana).

Zbiór użytkowników dzielony jest na dwa podzbiory:
1. Zbiór dłużników: $D = \{d \in U : B(d) < 0\}$
2. Zbiór wierzycieli: $C = \{c \in U : B(c) > 0\}$

Algorytm wykonuje iteracyjne parowanie w pętli:
1. Pobierany jest dłużnik $d_{max} \in D$ o najniższej wartości ujemnej bilansu (największy dłużnik) oraz wierzyciel $c_{max} \in C$ o najwyższej wartości dodatniej (największy wierzyciel).
2. Wyznaczana jest kwota transakcji optymalizacyjnej:
   $$T = \min(|B(d_{max})|, B(c_{max}))$$
3. Rejestrowana jest sugerowana transakcja: dłużnik $d_{max}$ przekazuje wierzycielowi $c_{max}$ kwotę $T$.
4. Dokonywana jest aktualizacja bilansów w pamięci roboczej:
   $$B(d_{max}) \leftarrow B(d_{max}) + T$$
   $$B(c_{max}) \leftarrow B(c_{max}) - T$$
5. Użytkownicy, których zaktualizowany bilans osiągnął wartość $0$ (z dokładnością do $0.01$), są usuwani z dalszego przetwarzania.
6. Pętla wykonuje się do momentu wyzerowania wszystkich bilansów.

Dzięki temu liczba transakcji zostaje zredukowana z pesymistycznej złożoności $O(N^2)$ (gdzie każdy płaci każdemu) do maksymalnie $N - 1$ transakcji, co stanowi optymalne rozwiązanie problemu przepływu w sieciach rozliczeniowych.

---

### 5.2. Algorytm Korekty Groszowej (Penny Rounding Adjustment)
Przy podziałach proporcjonalnych (np. kwota $A$ dzielona równo na $N$ członków grupy) zachodzi problem zaokrągleń numerycznych wartości zmiennoprzecinkowych. Reprezentacja walutowa wymaga dokładności do dwóch miejsc po przecinku (skala 2).

Standardowe dzielenie $A / N$ może generować ułamek nieskończony (np. $100.00 / 3 = 33.3333...$). Zaokrąglenie w dół daje sumę splitów $99.99$ PLN (brak $0.01$ PLN), natomiast w górę daje $100.02$ PLN (nadmiar $0.02$ PLN).

W celu eliminacji błędów braku spójności transakcyjnej zaimplementowano algorytm korekty reszty:
1. Wyznaczana jest kwota bazowa pojedynczego splitu:
   $$A_{base} = \text{Zaokrąglij}(A / N, 2, \text{RoundingMode.HALF_UP})$$
2. Dla pierwszych $N - 1$ członków grupy przypisywany jest dokładnie split o wartości $A_{base}$. Wyliczana jest suma częściowa:
   $$S_{part} = A_{base} \times (N - 1)$$
3. Dla ostatniego ($N$-tego) członka grupy przypisywany jest split o wartości skorygowanej:
   $$A_{last} = A - S_{part}$$
4. W ten sposób zachowana jest tożsamość:
   $$\sum_{i=1}^{N} A_i = A$$

Rozwiązanie to gwarantuje pełną integralność danych finansowych w bazie danych i zapobiega błędom walidacji transakcji na poziomie silnika Hibernate.

---

## 6. STRUKTURA PROJEKTU I IMPLEMENTACJA KODU

Poniżej przedstawiono wykaz kluczowych modułów i plików oprogramowania wraz z analizą ich przeznaczenia.

### 6.1. Warstwa Serwerowa (Moduł `backend`)
* `com/splitcosts/backend/model/`
  * `User.kt`: Model danych użytkownika z unikalnym mapowaniem klucza głównego.
  * `Group.kt`: Definicja grupy z relacjami powiązań członków i administratora.
  * `Expense.kt` & `ExpenseSplit.kt`: Struktury transakcyjne wydatków i alokacji kosztów.
  * `Settlement.kt`: Encja rejestrująca spłaty z mechanizmem akceptacji dwufazowej.
* `com/splitcosts/backend/repository/`
  * `GroupRepository.kt`: Zoptymalizowane zapytania HQL zapobiegające problemowi N+1.
  * `ExpenseRepository.kt`: Wydajne pobieranie wydatków wraz ze splitami w jednej transakcji JDBC.
* `com/splitcosts/backend/security/`
  * `SecurityConfig.kt`: Deklaracja filtrów bezpieczeństwa, wyłączenie stanu sesji, konfiguracja CORS.
  * `JwtAuthenticationFilter.kt` & `JwtService.kt`: Przechwytywanie i dekodowanie tokenów autoryzacyjnych.
* `com/splitcosts/backend/service/`
  * `BalanceService.kt`: Implementacja algorytmu upraszczania długów.
  * `ExpenseService.kt`: Walidacja podziałów kosztów oraz implementacja algorytmu korekty groszowej.
* `com/splitcosts/backend/config/`
  * `DatabaseInitializer.kt`: Automatyczny siewnik (*seeder*) danych testowych uruchamiany przy pustej bazie.

### 6.2. Warstwa Klencka (Moduł `frontend`)
* `shared/src/commonMain/kotlin/org/example/frontend/`
  * `api/ApiClient.kt`: Klasa komunikacji REST na bazie Ktor Client z obsługą bezstanowego JWT.
  * `ui/Theme.kt`: Definicja tokenów wizualnych ciemnego motywu, komponentów interfejsu (szklane karty, przyciski z poświatą neonową) i globalnego layoutu.
  * `ui/AppState.kt`: Reaktywne zarządzanie stosem nawigacyjnym aplikacji.
  * `ui/screens/`
    * `GroupListScreen.kt`: Dashboard grup użytkownika.
    * `GroupDetailsScreen.kt`: Zarządzanie strukturą grupy i przegląd historii kosztów.
    * `AddExpenseScreen.kt`: Formularz dodawania kosztu z dynamiczną walidacją sumy splitów w trybie ręcznym.
    * `BalancesScreen.kt`: Graficzna prezentacja sald członków oraz sugerowane spłaty długu z opcją zatwierdzania.
  * `App.kt`: Kontroler sterowania przepływem ekranów (router).

---

## 7. INSTRUKCJA WDROŻENIA I URUCHOMIENIA SYSTEMU

### 7.1. Uruchomienie Środowiska Produkcyjnego (Docker Compose)
Wdrożenie kompletnego systemu wraz z bazą danych, serwerem aplikacyjnym oraz serwerem WWW realizowane jest przy użyciu orkiestracji Docker Compose.

#### Wymagania:
* Zainstalowane środowisko **Docker Engine** oraz **Docker Compose**.

#### Procedura wdrożeniowa:
1. Przejdź do katalogu głównego projektu.
2. Wykonaj polecenie kompilacji i uruchomienia kontenerów w tle:
   ```bash
   docker compose up --build -d
   ```
3. Docker automatycznie:
   * Pobierze obraz bazy danych PostgreSQL 16.
   * Skompiluje kod backendu przy użyciu obrazu JDK 21 i uruchomi plik JAR na porcie `8080`.
   * Skompiluje zasoby frontendu Kotlin/Wasm i osadzi je w produkcyjnym serwerze Nginx na porcie `8081`.

#### Dostępność portów w środowisku lokalnym:
* **Interfejs użytkownika (Frontend):** [http://localhost:8081](http://localhost:8081)
* **Serwer aplikacyjny (Backend API):** `http://localhost:8080`
* **Baza danych (PostgreSQL):** port `5432`

### 7.2. Charakterystyka wbudowanych danych testowych i scenariuszy weryfikacji

W celu umożliwienia natychmiastowej ewaluacji i weryfikacji poprawności działania rozproszonych mechanizmów systemu (w tym algorytmów optymalizacyjnych i dwufazowych rozliczeń), baza danych jest automatycznie zasilana przy pierwszym uruchomieniu za pomocą komponentu klasy `DatabaseInitializer`. 

Do celów demonstracyjnych zdefiniowano **trzy konta testowe** (wszystkie zabezpieczone hasłem **`password123`**), z których każde reprezentuje inny stan i rolę w relacjach finansowych grupy:

1. **Użytkownik `janek` (Login: `janek`) - Wierzyciel Główny i Administrator**
   * **Charakterystyka:** Janek jest administratorem grupy *Wyjazd na Mazury*. Dokonał wpłat na kwotę wyższą niż jego udział w kosztach, co po zatwierdzeniu spłaty od Adama daje mu dodatnie saldo końcowe netto o wartości **`+30.00 PLN`**.
   * **Przeznaczenie weryfikacyjne:** Służy do demonstracji panelu administracyjnego (zapraszanie i usuwanie członków) oraz zatwierdzania spłat (tylko Janek, jako odbiorca przelewu, widzi w swojej historii aktywny przycisk pozwalający na autoryzację i zatwierdzenie spłaty od dłużnika).

2. **Użytkownik `adam` (Login: `adam`) - Płatnik Pośredni i Administrator Mieszkania**
   * **Charakterystyka:** Adam jest administratorem grupy *Wspólne Mieszkanie* (gdzie ma saldo dodatnie `+40.00 PLN` - Janek jest mu winien połowę kosztów internetu). W grupie *Mazury* posiada saldo `+10.00 PLN` (dokonał spłaty długu wobec Janka).
   * **Przeznaczenie weryfikacyjne:** Pokazuje poprawność wyliczania niezależnych sald wielowalutowych dla jednego użytkownika należącego do wielu grup rozliczeniowych jednocześnie.

3. **Użytkownik `kasia` (Login: `kasia`) - Dłużnik Główny**
   * **Charakterystyka:** Kasia posiada najwyższe saldo ujemne netto wynoszące **`-40.00 PLN`** w grupie *Mazury* (uczestniczyła w kosztach, dokonując najmniejszych wpłat własnych).
   * **Przeznaczenie weryfikacyjne:** Doskonale obrazuje działanie modułu szybkich spłat długów. Po zalogowaniu jako Kasia, w widoku rozliczeń pojawia się czerwony pasek zadłużenia oraz automatycznie wyznaczone przez algorytm Simplify Debts transakcje (Kasia wisi Jankowi 30.00 PLN oraz Adamowi 10.00 PLN). Użytkownik może kliknąć przycisk *Rozlicz*, wysyłając żądanie spłaty do bazy danych, które oczekuje na weryfikację przez wierzyciela.

#### Stan początkowy bazy danych w grupach celowych:
* **Grupa 1: "Wyjazd na Mazury"** (Administrator: Janek, Członkowie: Janek, Adam, Kasia)
  * **Wydatki (Łączna kwota: 390.00 PLN, udział jednostkowy: 130.00 PLN):**
    * Wydatek 1: "Paliwo do auta" opłacone przez **Janka** (kwota: `180.00 PLN`, podział równy).
    * Wydatek 2: "Zakupy w Biedronce" opłacone przez **Adama** (kwota: `120.00 PLN`, podział równy).
    * Wydatek 3: "Wypożyczenie łódki" opłacone przez **Kasią** (kwota: `90.00 PLN`, podział równy).
  * **Zatwierdzone spłaty:** Adam przelał Jankowi 20.00 PLN (zaakceptowane).
  * **Sugerowane spłaty optymalizacyjne (wyznaczone algorytmem):**
    * Kasia przekazuje **Jankowi**: `30.00 PLN`
    * Kasia przekazuje **Adamowi**: `10.00 PLN`
    *(Łączny dług Kasi `-40.00 PLN` rozbity na 2 optymalne transakcje).*

* **Grupa 2: "Wspólne Mieszkanie"** (Administrator: Adam, Członkowie: Janek, Adam)
  * **Wydatki:**
    * Wydatek 1: "Internet światłowodowy" opłacony przez **Adama** (kwota: `80.00 PLN`, podział równy po `40.00 PLN`).
  * **Sugerowane spłaty optymalizacyjne:**
    * Janek przekazuje **Adamowi**: `40.00 PLN`

---

## 8. PREZENTACJA WYNIKÓW DZIAŁANIA SYSTEMU (ZRZUTY EKRANU)

> *Uwaga: Poniższa sekcja służy jako szablony prezentacji wyników działania systemu. Po uruchomieniu projektu w środowisku deweloperskim należy wykonać zrzuty ekranu i wkleić je w wyznaczone miejsca (placeholdery).*

---

### 8.1. Wizualizacja Panelu Głównego Grup (Dashboard)
Ekran prezentuje interfejs użytkownika po pomyślnym zalogowaniu do systemu. Widoczna jest lista grup, do których należy użytkownik, informacja o liczbie członków oraz etykieta określająca uprawnienia ("Admin" lub "Członek"). Z lewej strony znajduje się szklany panel tworzenia nowej grupy.

```
┌────────────────────────────────────────────────────────────────────────┐
│                                                                        │
│                      [ PLACEHOLDER NA ZRZUT EKRANU ]                   │
│         (Zrzut ekranu przedstawiający Panel Główny Grup)               │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```
*Rysunek 1: Panel główny grup rozliczeniowych z panelem tworzenia grup.*

---

### 8.2. Widok Szczegółów Grupy i Ewidencji Wydatków
Ekran prezentuje strukturę wybranej grupy celowej. Widoczna jest lista zarejestrowanych członków z oznaczeniem administratora. W przypadku uprawnień administracyjnych dostępny jest formularz zaproszeń. Z prawej strony wyświetlana jest chronologiczna historia wydatków grupy wraz z ich płatnikami oraz szczegółowym podziałem kwot na poszczególne osoby.

```
┌────────────────────────────────────────────────────────────────────────┐
│                                                                        │
│                      [ PLACEHOLDER NA ZRZUT EKRANU ]                   │
│            (Zrzut ekranu przedstawiający Widok Szczegółów Grupy)       │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```
*Rysunek 2: Widok szczegółowy grupy z listą członków oraz historią wydatków.*

---

### 8.3. Formularz Dodawania Wydatku (Tryb Podziału Niestandardowego)
Ekran prezentuje interfejs dodawania nowego kosztu do grupy. Widoczne są pola opisu, kwoty, waluty oraz wyboru płatnika. Uruchomiony tryb podziału niestandardowego wyświetla pola ręcznej alokacji kwot na członków grupy wraz z dynamicznym paskiem walidacyjnym informującym o zgodności sumy splitów z całkowitą kwotą wydatku.

```
┌────────────────────────────────────────────────────────────────────────┐
│                                                                        │
│                      [ PLACEHOLDER NA ZRZUT EKRANU ]                   │
│          (Zrzut ekranu przedstawiający Formularz Dodawania Wydatku)    │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```
*Rysunek 3: Formularz rejestrowania wydatków z aktywnym podziałem ręcznym.*

---

### 8.4. Wizualizacja Salda Netto i Sugerowanych Rozliczeń
Ekran prezentuje wyniki obliczeniowe algorytmu optymalizacji sald. Z lewej strony widoczne są proporcjonalne, kolorowe paski salda netto członków grupy (kolor zielony reprezentuje saldo dodatnie - wierzyciel, kolor czerwony saldo ujemne - dłużnik). Z prawej strony wyświetlana jest zoptymalizowana ścieżka spłat długu wraz z funkcją szybkiego rozliczenia transakcji.

```
┌────────────────────────────────────────────────────────────────────────┐
│                                                                        │
│                      [ PLACEHOLDER NA ZRZUT EKRANU ]                   │
│       (Zrzut ekranu przedstawiający Salda Netto i Sugerowane Spłaty)   │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```
*Rysunek 4: Graficzna prezentacja sald członków grupy i zoptymalizowana lista spłat.*

---

### 8.5. Ewidencja Spłat i Moduł Zatwierdzania Transakcji przez Wierzyciela
Ekran prezentuje historię spłat długu zgłoszonych w grupie. Widoczne są statusy spłat. Zalogowany użytkownik będący wierzycielem danej transakcji ma dostęp do przycisku potwierdzenia odbioru gotówki, który inicjuje aktualizację sald w bazie danych.

```
┌────────────────────────────────────────────────────────────────────────┐
│                                                                        │
│                      [ PLACEHOLDER NA ZRZUT EKRANU ]                   │
│         (Zrzut ekranu przedstawiający Historię Spłat i Zatwierdzanie)  │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```
*Rysunek 5: Historia rozliczeń dłużnik-wierzyciel z systemem zatwierdzania spłat.*
