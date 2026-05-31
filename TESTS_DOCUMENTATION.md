# Dokumentacja Testów Integracyjnych API (Backend)
## Projekt: Costs Split (Podział Kosztów Grupowych)

Niniejszy plik stanowi szczegółowy opis i dokumentację techniczną automatycznego zestawu testów integracyjnych zaimplementowanych w skrypcie `test_backend.sh`. Zestaw ten służy do kompleksowej weryfikacji poprawności działania interfejsów API backendu, mechanizmów autoryzacji JWT, logiki biznesowej podziału wydatków oraz reguł bezpieczeństwa.

---

## 1. Założenia Projektowe i Architektura Testów

Testy zostały zaprojektowane jako **skrypt integracyjny Bash (`test_backend.sh`)** wykonujący rzeczywiste zapytania HTTP za pomocą narzędzia `curl` do kontenera z backendem (`http://localhost:8080`).

### Kluczowe cechy skryptu testowego:
1. **Idempotentność (Tear-down / Cleanup):** Skrypt po wykonaniu całego scenariusza usuwa wszystkie utworzone przez siebie zasoby (wydatki, członkostwa, grupy). Po zakończeniu testu baza danych wraca dokładnie do swojego pierwotnego stanu.
2. **Odporność na brak zależności:** Skrypt potrafi dynamicznie wykryć obecność narzędzia `jq` (konsolowego parsera JSON). W przypadku jego braku automatycznie przełącza się na **wbudowany parser oparty na wyrażeniach regularnych (regex powłoki Bash)**, dzięki czemu skrypt uruchomi się na dowolnym komputerze bez instalowania dodatkowego oprogramowania.
3. **Weryfikacja ścieżek negatywnych (Sad Paths):** Skrypt testuje nie tylko prawidłowe zachowania (*happy paths*), ale również próby włamań, nieautoryzowanego dostępu oraz próby oszustwa finansowego (zatwierdzenie własnego długu).
4. **Kolorowy interfejs konsoli:** Skrypt czytelnie wizualizuje przebieg testu w terminalu za pomocą kolorów ANSI (`PASS` na zielono, `FAIL` na czerwono, sekcje na niebiesko).

---

## 2. Szczegółowy Opis Scenariusza Testowego (16 Testów)

Skrypt podzielony jest na **6 logicznych faz**, z których każda odpowiada za inny aspekt działania systemu:

### Faza I: Bezpieczeństwo i Autoryzacja (Sad Paths)
Faza ta sprawdza odporność API na próby nieautoryzowanego odpytywania bez prawidłowych poświadczeń JWT.

*   **Test 1: Próba pobrania grup bez podania nagłówka autoryzacji**
    *   *Opis:* Wysyła żądanie `GET /api/groups` bez nagłówka `Authorization`.
    *   *Oczekiwany rezultat:* Serwer musi odrzucić żądanie, zwracając kod statusu **401 Unauthorized** lub **403 Forbidden**.
*   **Test 2: Próba pobrania grup ze zmanipulowanym tokenem JWT**
    *   *Opis:* Wysyła żądanie `GET /api/groups` z nagłówkiem `Authorization: Bearer zly_token_jwt_12345`.
    *   *Oczekiwany rezultat:* Serwer musi odrzucić żądanie, zwracając kod statusu **401 Unauthorized** lub **403 Forbidden**.

### Faza II: Profil Użytkownika i Logowanie (CRUD)
Faza ta weryfikuje poprawność procesu rejestracji, uwierzytelniania i pobierania danych profilowych zalogowanego użytkownika.

*   **Test 3: Logowanie / Rejestracja Jana Testowego (`jan_test`)**
    *   *Opis:* Wysyła żądanie `POST /api/auth/register` (lub loguje, jeśli użytkownik już istnieje w bazie).
    *   *Oczekiwany rezultat:* Serwer zwraca status **200/201** wraz ze świeżym tokenem JWT niezbędnym do kolejnych kroków.
*   **Test 4: Pobieranie własnego profilu zalogowanego użytkownika**
    *   *Opis:* Wysyła żądanie `GET /api/auth/me` z tokenem JWT Jana.
    *   *Oczekiwany rezultat:* Serwer zwraca status **200 OK** oraz poprawny obiekt JSON reprezentujący Jana (weryfikacja pobrania poprawnego ID użytkownika).

*(Uwaga: W tle rejestrowany/logowany jest również drugi użytkownik testowy - Adam Testowy `adam_test`, którego ID jest zapisywane do zmiennej).*

### Faza III: Uprawnienia i Członkostwo w Grupach (CRUD + Sad Path)
Faza ta sprawdza operacje na grupach rozliczeniowych oraz weryfikuje, czy użytkownicy spoza grupy nie mają dostępu do jej poufnych danych.

*   **Test 5: Tworzenie nowej grupy rozliczeniowej "Gory 2026"**
    *   *Opis:* Jan tworzy grupę poprzez `POST /api/groups`. Jan staje się automatycznie administratorem tej grupy.
    *   *Oczekiwany rezultat:* Status **201 Created**, serwer zwraca obiekt nowo utworzonej grupy wraz z jej unikalnym identyfikatorem ID.
*   **Test 6: Dodawanie użytkownika Adam do grupy przez administratora**
    *   *Opis:* Jan wysyła zapytanie `POST /api/groups/{id}/members` podając nazwę użytkownika Adama (`adam_test`).
    *   *Oczekiwany rezultat:* Status **200 OK**, Adam zostaje pomyślnie dopisany do bazy jako członek grupy.
*   **Test 7 (Sad Path): Próba kradzieży danych przez nieautoryzowanego użytkownika**
    *   *Opis:* Rejestrowany jest trzeci użytkownik (`hacker_test`), który nie należy do grupy "Gory 2026". Próbuje on odpytać endpoint `GET /api/groups/{id}` o szczegóły tej grupy.
    *   *Oczekiwany rezultat:* Serwer musi zablokować to żądanie, zwracając status **403 Forbidden** (blokada naruszenia prywatności danych).

### Faza IV: Procesowanie Wydatków i Kalkulator Sald (CRUD)
Faza ta weryfikuje poprawność algorytmów rozliczania wydatków grupowych.

*   **Test 8: Dodanie wspólnego wydatku przez Jana (120 PLN za Obiad)**
    *   *Opis:* Jan rejestruje wydatek poprzez `POST /api/groups/{id}/expenses` z podziałem wagowym 50/50 (60 PLN Jan, 60 PLN Adam).
    *   *Oczekiwany rezultat:* Status **201 Created**, wydatek zostaje pomyślnie rozksięgowany w bazie.
*   **Test 9: Sprawdzenie poprawności i weryfikacja kalkulacji sald grupy**
    *   *Opis:* Jan pobiera aktualne saldo grupy za pomocą `GET /api/groups/{id}/balances`.
    *   *Oczekiwany rezultat:* Status **200 OK**. Skrypt weryfikuje, czy silnik matematyczny backendu poprawnie obliczył bilans: Adam musi być winny Janowi dokładnie **60.00 PLN**.

### Faza V: Spłata Długów i Zabezpieczenia Rozliczeń (Sad Path + Happy Path)
Faza ta sprawdza proces rozliczania (spłat) oraz zapobiega próbom oszustw polegających na samodzielnym zatwierdzaniu własnych spłat.

*   **Test 10: Adam zgłasza spłatę długu (Settlement) do Jana**
    *   *Opis:* Adam loguje się na swoje konto i wysyła propozycję spłaty długu o wartości 60.00 PLN do Jana (`POST /api/groups/{id}/settlements`).
    *   *Oczekiwany rezultat:* Status **201 Created**. Powstaje spłata o statusie `approved: false` (oczekująca).
*   **Test 11 (Sad Path): Dłużnik (Adam) próbuje sam sobie zatwierdzić spłatę**
    *   *Opis:* Adam próbuje wywołać endpoint `POST /api/groups/{id}/settlements/{settlementId}/approve` w celu zatwierdzenia własnej spłaty.
    *   *Oczekiwany rezultat:* Serwer musi odrzucić to żądanie (status **>= 400** / **403 Forbidden**), ponieważ tylko wierzyciel (odbiorca pieniędzy) ma prawo potwierdzić otrzymanie środków.
*   **Test 12: Wierzyciel (Jan) zatwierdza otrzymanie spłaty długu**
    *   *Opis:* Jan loguje się na swoje konto i zatwierdza spłatę Adama poprzez `POST /api/groups/{id}/settlements/{settlementId}/approve`.
    *   *Oczekiwany rezultat:* Status **200 OK**, spłata zostaje oznaczona w bazie jako `approved: true`. Salda grupy po ponownym przeliczeniu wynoszą dokładnie **0.00 PLN** dla obu użytkowników.

### Faza VI: Czyszczenie Danych / Tear-down (CRUD / Idempotentność)
Faza ta realizuje procedurę automatycznego sprzątania bazy danych po teście, przy użyciu kaskadowego usuwania JPA zaimplementowanego w encjach backendowych.

*   **Test 13: Usunięcie wydatku z grupy**
    *   *Opis:* Wywołanie `DELETE /api/groups/{id}/expenses/{expenseId}`.
    *   *Oczekiwany rezultat:* Status **200 OK** lub **204 No Content**, wydatek i powiązane z nim podziały (`ExpenseSplit`) zostają trwale usunięte z bazy.
*   **Test 14: Usunięcie Adama ze składu grupy**
    *   *Opis:* Wywołanie `DELETE /api/groups/{id}/members/{userId}`.
    *   *Oczekiwany rezultat:* Status **200 OK**, członkostwo Adama w grupie zostaje usunięte.
*   **Test 15: Usunięcie całej grupy rozliczeniowej**
    *   *Opis:* Wywołanie `DELETE /api/groups/{id}`.
    *   *Oczekiwany rezultat:* Status **200/204**. Dzięki kaskadom JPA (`cascade = [CascadeType.ALL]` w pliku `Group.kt`), Hibernate automatycznie usuwa z bazy powiązane spłaty (`Settlement`) i oczyszcza powiązania, eliminując ryzyko błędów więzów spójności bazy.
*   **Test 16: Weryfikacja czystości bazy danych**
    *   *Opis:* Próba pobrania szczegółów usuniętej grupy (`GET /api/groups/{id}`).
    *   *Oczekiwany rezultat:* Serwer musi zwrócić kod **404 Not Found** lub **403 Forbidden**, co potwierdza, że grupa i wszystkie jej dane zostały w 100% poprawnie i trwale wyczyszczone z systemu.

---

## 3. Instrukcja Uruchomienia Testów

Aby pomyślnie uruchomić pełny zestaw testów integracyjnych:

1.  **Upewnij się, że aplikacja działa w kontenerach Docker:**
    ```powershell
    docker compose up -d
    ```
2.  **Otwórz powłokę Bash (np. Git Bash na Windows lub terminal w WSL/Linux) w głównym katalogu projektu.**
3.  **Nadaj uprawnienia do uruchamiania pliku skryptu (wymagane tylko raz):**
    ```bash
    chmod +x test_backend.sh
    ```
4.  **Uruchom skrypt testowy:**
    ```bash
    ./test_backend.sh
    ```

Po zakończeniu działania na ekranie terminala ukaże się podsumowanie:
```text
======================================================================
  PODSUMOWANIE KOŃCOWE TESTÓW INTEGRACYJNYCH
======================================================================
  Zakończone powodzeniem (PASSED): 16
  Zakończone błędem     (FAILED): 0

======================================================================
    GRATULACJE! Wszystkie testy (w tym Sad Paths, CRUD i Tear-down)
    zakończyły się pełnym sukcesem. Baza danych pozostała czysta!
======================================================================
```
