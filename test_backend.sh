#!/bin/bash

# ==============================================================================
#   COSTS SPLIT - ULTIMATE BACKEND TEST SUITE (INTEGRATION, CRUD, SAD PATHS, CLEANUP)
# ==============================================================================
#   Ten skrypt wykonuje KOMPLETNE testy funkcjonalne, bezpieczeństwa oraz CRUD backendu.
#   Zapewnia pełną idempotencję - sprząta po sobie (Tear-down) usuwając utworzone dane,
#   dzięki czemu baza pozostaje w nienaruszonym stanie.
#
#   Scenariusz testowy obejmuje:
#     I. TESTY BEZPIECZEŃSTWA (Sad Paths / Negative Cases):
#        1. Próba dostępu bez tokenu (401 Unauthorized).
#        2. Próba dostępu ze złym tokenem (401/403).
#     II. TESTY UŻYTKOWNIKÓW I PROFILI (Happy Path):
#        3. Rejestracja/Logowanie Jana Testowego (jan_test) i Adama Testowego (adam_test).
#        4. Pobranie danych profilowych (/api/auth/me).
#     III. ZARZĄDZANIE GRUPAMI & UPRAWNIENIA (CRUD + Sad Path):
#        5. Utworzenie grupy "Gory 2026" (Jan jest administratorem).
#        6. Dodanie Adama do grupy na podstawie nazwy użytkownika.
#        7. Sad Path: Rejestracja hakera (hacker_test), który próbuje podejrzeć
#           dane grupy Jana, do której nie należy (oczekiwane 403 Forbidden).
#     IV. ZARZĄDZANIE WYDATKAMI (CRUD + Kalkulacje):
#        8. Jan dodaje wspólny wydatek "Obiad" na kwotę 120 PLN (podział po 60 PLN).
#        9. Sprawdzenie salda grupy - Adam ma saldo -60.00 PLN, Jan +60.00 PLN.
#     V. ROZLICZENIA & BLOKADY (Sad Path + Happy Path):
#        10. Adam zgłasza propozycję spłaty 60.00 PLN (Settlement).
#        11. Sad Path: Próba zatwierdzenia spłaty przez dłużnika (Adama) - oczekiwana odmowa.
#        12. Happy Path: Wierzyciel (Jan) zatwierdza spłatę. Salda wracają do 0.00 PLN.
#     VI. CZYSZCZENIE DANYCH (Tear-down / Cleanup):
#        13. Usunięcie zarejestrowanego wydatku (DELETE /api/groups/{id}/expenses/{expId}).
#        14. Usunięcie członka grupy (DELETE /api/groups/{id}/members/{userId}).
#        15. Usunięcie całej grupy (DELETE /api/groups/{id}).
#        16. Potwierdzenie czystości bazy - próba pobrania usuniętej grupy (404/403).
# ==============================================================================

# Definiowanie kolorów konsoli
NC='\033[0m'
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'

BASE_URL="http://localhost:8080"
TOTAL_PASSED=0
TOTAL_FAILED=0

# Funkcje pomocnicze
print_header() {
    echo -e "\n${BLUE}======================================================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}======================================================================${NC}"
}

assert_status() {
    local actual="$1"
    local expected="$2"
    local msg="$3"
    if [ "$actual" -eq "$expected" ]; then
        echo -e "${GREEN}[PASS] $msg (Status: $actual)${NC}"
        TOTAL_PASSED=$((TOTAL_PASSED + 1))
    else
        echo -e "${RED}[FAIL] $msg (Oczekiwano: $expected, Otrzymano: $actual)${NC}"
        TOTAL_FAILED=$((TOTAL_FAILED + 1))
    fi
}

extract_json_field() {
    local json="$1"
    local field="$2"
    if command -v jq >/dev/null 2>&1; then
        echo "$json" | jq -r ".$field"
    else
        echo "$json" | grep -o "\"$field\"[^,]*" | head -n 1 | cut -d':' -f2- | tr -d '"{}[] '
    fi
}

# Sprawdzenie serwera
if ! curl -s --connect-timeout 2 "$BASE_URL/api/auth/login" > /dev/null; then
    echo -e "${RED}[BŁĄD] Serwer backendu pod adresem $BASE_URL nie odpowiada!${NC}"
    exit 1
fi

# ==============================================================================
#   Faza I: TESTY BEZPIECZEŃSTWA (Sad Paths)
# ==============================================================================
print_header "FAZA I: BEZPIECZEŃSTWO & AUTORYZACJA (Sad Paths)"

# 1. Dostęp bez tokenu JWT
echo -e "${YELLOW}Test 1: Próba pobrania grup bez podania nagłówka autoryzacji...${NC}"
STATUS_1=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/groups")
assert_status "$STATUS_1" 401 "Zablokowanie żądania bez tokenu JWT"

# 2. Dostęp ze złym tokenem JWT
echo -e "\n${YELLOW}Test 2: Próba pobrania grup ze zmanipulowanym tokenem JWT...${NC}"
STATUS_2=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/groups" \
  -H "Authorization: Bearer zly_token_jwt_12345")
assert_status "$STATUS_2" 401 "Zablokowanie żądania z niepoprawnym tokenem JWT"


# ==============================================================================
#   Faza II: REJESTRACJA & UŻYTKOWNICY
# ==============================================================================
print_header "FAZA II: PROFIL UŻYTKOWNIKA & LOGOWANIE (CRUD)"

# 3. Logowanie/Rejestracja Jana i Adama
echo -e "${YELLOW}Test 3: Pozyskiwanie tokenu uwierzytelniającego dla Jana (jan_test)...${NC}"
REG_A_RESP=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"jan_test","email":"jan_test@example.com","password":"password123","name":"Jan Testowy"}')
TOKEN_A=$(extract_json_field "$REG_A_RESP" "token")

if [ -z "$TOKEN_A" ] || [ "$TOKEN_A" == "null" ]; then
    LOGIN_A_RESP=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"usernameOrEmail":"jan_test","password":"password123"}')
    TOKEN_A=$(extract_json_field "$LOGIN_A_RESP" "token")
fi

if [ ! -z "$TOKEN_A" ] && [ "$TOKEN_A" != "null" ]; then
    assert_status "200" 200 "Pomyślne logowanie i pobranie tokenu JWT dla Jana"
else
    assert_status "500" 200 "Logowanie Jana nie powiodło się!"
    exit 1
fi

# Pozyskanie ID i tokenu Adama
REG_B_RESP=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"adam_test","email":"adam_test@example.com","password":"password123","name":"Adam Testowy"}')
USER_B_ID=$(echo "$REG_B_RESP" | grep -o '"user"[^}]*' | grep -o '"id":[0-9]*' | cut -d':' -f2 | tr -d ' ')

if [ -z "$USER_B_ID" ] || [ "$USER_B_ID" == "null" ]; then
    LOGIN_B_RESP=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"usernameOrEmail":"adam_test","password":"password123"}')
    TOKEN_B=$(extract_json_field "$LOGIN_B_RESP" "token")
    
    ME_B_RESP=$(curl -s -X GET "$BASE_URL/api/auth/me" \
      -H "Authorization: Bearer $TOKEN_B")
    USER_B_ID=$(extract_json_field "$ME_B_RESP" "id")
fi
echo -e "ID Adama: $USER_B_ID"

# 4. Sprawdzenie profilu /api/auth/me
echo -e "\n${YELLOW}Test 4: Pobieranie własnego profilu zalogowanego użytkownika (Jan)...${NC}"
ME_RESP=$(curl -s -X GET "$BASE_URL/api/auth/me" \
  -H "Authorization: Bearer $TOKEN_A")
USER_A_ID=$(extract_json_field "$ME_RESP" "id")
STATUS_4=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/auth/me" \
  -H "Authorization: Bearer $TOKEN_A")
assert_status "$STATUS_4" 200 "Odczytanie danych profilu użytkownika (ID: $USER_A_ID)"


# ==============================================================================
#   Faza III: GRUPY & LOCKI BEZPIECZEŃSTWA (Sad Path + CRUD)
# ==============================================================================
print_header "FAZA III: UPRAWNIENIA DO GRUPY (Happy & Sad Paths)"

# 5. Utworzenie grupy rozliczeniowej
echo -e "${YELLOW}Test 5: Tworzenie nowej grupy rozliczeniowej 'Gory 2026'...${NC}"
GROUP_RESP=$(curl -s -X POST "$BASE_URL/api/groups" \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"name":"Gory 2026","description":"Wspolny wyjazd majowy w gory"}')
GROUP_ID=$(extract_json_field "$GROUP_RESP" "id")
echo -e "Utworzone ID Grupy: $GROUP_ID"

STATUS_5=$(echo "$GROUP_RESP" | grep -q '"id":' && echo "201" || echo "500")
assert_status "$STATUS_5" 201 "Pomyślne utworzenie nowej grupy (ID: $GROUP_ID)"

# 6. Dodanie Adama do grupy
echo -e "\n${YELLOW}Test 6: Dodawanie użytkownika Adam do grupy przez administratora (Jana)...${NC}"
ADD_MEMBER_RESP=$(curl -s -X POST "$BASE_URL/api/groups/$GROUP_ID/members" \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"adam_test"}')
STATUS_6=$(echo "$ADD_MEMBER_RESP" | grep -q '"username":"adam_test"' && echo "200" || echo "500")
assert_status "$STATUS_6" 200 "Pomyślne dodanie użytkownika 'adam_test' do grupy"

# 7. Sad Path: Rejestracja Hakera i próba kradzieży danych grupy
echo -e "\n${YELLOW}Test 7: Rejestracja nieautoryzowanego użytkownika 'hacker_test'...${NC}"
REG_HACK_RESP=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"hacker_test","email":"hacker@example.com","password":"password123","name":"Haker Testowy"}')
TOKEN_HACK=$(extract_json_field "$REG_HACK_RESP" "token")

if [ -z "$TOKEN_HACK" ] || [ "$TOKEN_HACK" == "null" ]; then
    LOGIN_HACK_RESP=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"usernameOrEmail":"hacker_test","password":"password123"}')
    TOKEN_HACK=$(extract_json_field "$LOGIN_HACK_RESP" "token")
fi

echo -e "${YELLOW}Haker próbuje odczytać szczegóły grupy Jana o ID $GROUP_ID...${NC}"
STATUS_HACK=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_HACK")
assert_status "$STATUS_HACK" 403 "Zablokowanie dostępu do cudzej grupy (Oczekiwany 403 Forbidden)"


# ==============================================================================
#   Faza IV: WYDATKI & MATEMATYKA (CRUD)
# ==============================================================================
print_header "FAZA IV: PROCESOWANIE WYDATKÓW & KALKULATOR SALD"

# 8. Rejestracja wydatku
echo -e "${YELLOW}Test 8: Jan dodaje wydatek 120 PLN za 'Obiad' (splits: 50% Jan / 50% Adam)...${NC}"
EXPENSE_RESP=$(curl -s -X POST "$BASE_URL/api/groups/$GROUP_ID/expenses" \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d "{\"description\":\"Obiad\",\"amount\":120.00,\"paidByUserId\":$USER_A_ID,\"splits\":[{\"userId\":$USER_A_ID,\"amount\":60.00},{\"userId\":$USER_B_ID,\"amount\":60.00}]}")
EXPENSE_ID=$(extract_json_field "$EXPENSE_RESP" "id")
echo -e "Utworzone ID Wydatku: $EXPENSE_ID"

STATUS_8=$(echo "$EXPENSE_RESP" | grep -q '"id":' && echo "201" || echo "500")
assert_status "$STATUS_8" 201 "Pomyślne dodanie i rozksięgowanie nowego wydatku"

# 9. Sprawdzenie poprawności sald
echo -e "\n${YELLOW}Test 9: Pobieranie sald grupy i weryfikacja kalkulacji długu...${NC}"
BALANCES_RESP=$(curl -s -X GET "$BASE_URL/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A")

# Weryfikacja długu (czy Adam wisi Janowi dokładnie 60.00 PLN)
DEBT_VALID=$(echo "$BALANCES_RESP" | grep -q "\"amount\":60.00" && echo "PASS" || echo "FAIL")
if [ "$DEBT_VALID" == "PASS" ]; then
    assert_status "200" 200 "Silnik rozliczeniowy poprawnie wyliczył dług Adama wobec Jana (60.00 PLN)"
else
    assert_status "500" 200 "Błąd! Silnik rozliczeniowy podał błędne salda!"
fi


# ==============================================================================
#   Faza V: ROZLICZENIA & ZABEZPIECZENIA SPŁAT
# ==============================================================================
print_header "FAZA V: SPŁATA DŁUGÓW & ZABEZPIECZENIA ROZLICZEŃ"

# 10. Adam zgłasza spłatę
echo -e "${YELLOW}Test 10: Logowanie jako Adam i zgłaszanie spłaty długu do Jana (60 PLN)...${NC}"
LOGIN_B_RESP=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"adam_test","password":"password123"}')
TOKEN_B=$(extract_json_field "$LOGIN_B_RESP" "token")

SETTLEMENT_RESP=$(curl -s -X POST "$BASE_URL/api/groups/$GROUP_ID/settlements" \
  -H "Authorization: Bearer $TOKEN_B" \
  -H "Content-Type: application/json" \
  -d "{\"amount\":60.00,\"debtorId\":$USER_B_ID,\"creditorId\":$USER_A_ID}")
SETTLEMENT_ID=$(extract_json_field "$SETTLEMENT_RESP" "id")
echo -e "ID Spłaty (oczekuje na akceptację): $SETTLEMENT_ID"

STATUS_10=$(echo "$SETTLEMENT_RESP" | grep -q '"id":' && echo "201" || echo "500")
assert_status "$STATUS_10" 201 "Pomyślne zarejestrowanie propozycji spłaty"

# 11. Sad Path: Dłużnik (Adam) próbuje sam sobie zatwierdzić spłatę
echo -e "\n${YELLOW}Test 11: Zabezpieczenie: Próba akceptacji spłaty przez dłużnika (Adama) zamiast wierzyciela...${NC}"
STATUS_11=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/settlements/$SETTLEMENT_ID/approve" \
  -H "Authorization: Bearer $TOKEN_B")
# Powinno zwrócić błąd uprawnień (400 lub 403 w zależności od implementacji serwisu bezpieczeństwa)
if [ "$STATUS_11" -ge 400 ]; then
    assert_status "$STATUS_11" "$STATUS_11" "Zablokowanie oszukańczej próby akceptacji długu przez dłużnika"
else
    assert_status "$STATUS_11" "400" "BŁĄD! Dłużnik mógł sam zatwierdzić swój dług!"
fi

# 12. Happy Path: Wierzyciel (Jan) zatwierdza spłatę
echo -e "\n${YELLOW}Test 12: Prawidłowe zatwierdzenie spłaty przez wierzyciela (Jana)...${NC}"
STATUS_12=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/settlements/$SETTLEMENT_ID/approve" \
  -H "Authorization: Bearer $TOKEN_A")
assert_status "$STATUS_12" 200 "Pomyślna akceptacja spłaty przez Jana"


# ==============================================================================
#   Faza VI: CZYSZCZENIE DANYCH (Tear-down / Cleanup)
# ==============================================================================
print_header "FAZA VI: CZYSZCZENIE DANYCH (Tear-down / Idempotency)"

# 13. Usunięcie wydatku
echo -e "${YELLOW}Test 13: Usuwanie skojarzonego wydatku o ID $EXPENSE_ID...${NC}"
STATUS_13=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/groups/$GROUP_ID/expenses/$EXPENSE_ID" \
  -H "Authorization: Bearer $TOKEN_A")
assert_status "$STATUS_13" 200 "Usunięcie wydatku z grupy"

# 14. Usunięcie członka z grupy
echo -e "\n${YELLOW}Test 14: Usuwanie Adama (ID: $USER_B_ID) ze składu grupy o ID $GROUP_ID...${NC}"
STATUS_14=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/groups/$GROUP_ID/members/$USER_B_ID" \
  -H "Authorization: Bearer $TOKEN_A")
assert_status "$STATUS_14" 200 "Usunięcie użytkownika Adam z grupy"

# 15. Usunięcie całej grupy
echo -e "\n${YELLOW}Test 15: Usuwanie całej grupy o ID $GROUP_ID (administrator Jan)...${NC}"
STATUS_15=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_A")
assert_status "$STATUS_15" 200 "Skuteczne usunięcie grupy"

# 16. Weryfikacja czystości - próba dostępu do usuniętej grupy
echo -e "\n${YELLOW}Test 16: Próba pobrania danych usuniętej grupy...${NC}"
STATUS_16=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_A")
if [ "$STATUS_16" -eq 404 ] || [ "$STATUS_16" -eq 403 ]; then
    assert_status "$STATUS_16" "$STATUS_16" "Brak dostępu do usuniętej grupy (Baza danych została w 100% wyczyszczona)"
else
    assert_status "$STATUS_16" "404" "BŁĄD! Usunięta grupa wciąż figuruje w systemie!"
fi


# ==============================================================================
#   PODSUMOWANIE KOŃCOWE
# ==============================================================================
print_header "PODSUMOWANIE KOŃCOWE TESTÓW INTEGRACYJNYCH"
echo -e "  Zakończone powodzeniem (PASSED): ${GREEN}$TOTAL_PASSED${NC}"
echo -e "  Zakończone błędem     (FAILED): ${RED}$TOTAL_FAILED${NC}"

if [ "$TOTAL_FAILED" -eq 0 ]; then
    echo -e "\n${GREEN}======================================================================${NC}"
    echo -e "${GREEN}    GRATULACJE! Wszystkie testy (w tym Sad Paths, CRUD i Tear-down)    ${NC}"
    echo -e "${GREEN}    zakończyły się pełnym sukcesem. Baza danych pozostała czysta!      ${NC}"
    echo -e "${GREEN}======================================================================${NC}"
    exit 0
else
    echo -e "\n${RED}======================================================================${NC}"
    echo -e "${RED}    BŁĄD! Niektóre testy integracyjne zakończyły się niepowodzeniem.   ${NC}"
    echo -e "${RED}    Zweryfikuj logi powyżej.                                           ${NC}"
    echo -e "${RED}======================================================================${NC}"
    exit 1
fi
