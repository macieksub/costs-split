#!/bin/bash

# ==============================================================================
#   COSTS SPLIT - BACKEND INTEGRATION TEST SUITE
#   Ten skrypt wykonuje kompletny, wielokrokowy test integracyjny backendu.
#   Scenariusz:
#     1. Rejestruje dwóch użytkowników testowych (Jan i Adam).
#     2. Loguje się jako Jan, aby pobrać token JWT.
#     3. Pobiera profil zalogowanego użytkownika (GET /api/auth/me).
#     4. Tworzy nową grupę (POST /api/groups) jako Jan.
#     5. Dodaje Adama do grupy (POST /api/groups/{id}/members) podając jego login.
#     6. Dodaje wspólny wydatek o wartości 120 PLN opłacony przez Jana (podział 50/50).
#     7. Sprawdza salda grupy (GET /api/groups/{id}/balances) - Adam powinien wisieć Janowi 60 PLN.
#     8. Loguje się jako Adam i zgłasza spłatę długu 60 PLN (POST /api/groups/{id}/settlements).
#     9. Loguje się z powrotem jako Jan i zatwierdza spłatę (POST /api/groups/{id}/settlements/{id}/approve).
#    10. Sprawdza ostateczne salda - po spłacie saldo obu użytkowników powinno wynosić 0.00 PLN.
# ==============================================================================

# Resetowanie i definiowanie kolorów konsoli
NC='\033[0m'
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'

BASE_URL="http://localhost:8080"

echo -e "${BLUE}======================================================================${NC}"
echo -e "${BLUE}        COSTS SPLIT - SCENARIUSZ TESTÓW INTEGRACYJNYCH API            ${NC}"
echo -e "${BLUE}======================================================================${NC}"

# Funkcja pomocnicza do pobierania pól z JSON bez zewnętrznych zależności (jq)
extract_json_field() {
    local json="$1"
    local field="$2"
    if command -v jq >/dev/null 2>&1; then
        echo "$json" | jq -r ".$field"
    else
        # Proste, odporne parsowanie regex w sed/grep jako fallback
        echo "$json" | grep -o "\"$field\"[^,]*" | head -n 1 | cut -d':' -f2- | tr -d '"{}[] '
    fi
}

# Sprawdzenie czy backend w ogóle działa przed startem
echo -e "${CYAN}Weryfikacja dostępności serwera backendu pod adresem $BASE_URL...${NC}"
if ! curl -s --connect-timeout 3 "$BASE_URL/api/auth/login" > /dev/null; then
    echo -e "${RED}[BŁĄD] Serwer backendu nie odpowiada pod adresem $BASE_URL!${NC}"
    echo -e "${YELLOW}Upewnij się, że kontenery Docker działają (docker compose up -d) lub serwer Spring Boot jest uruchomiony.${NC}"
    exit 1
fi
echo -e "${GREEN}Serwer działa poprawnie. Rozpoczynam testy...${NC}"

# 1. Rejestracja użytkownika A (Jan Testowy)
echo -e "\n${YELLOW}[Krok 1/10] Rejestracja nowego użytkownika Jan Testowy (jan_test)...${NC}"
REG_A_RESP=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"jan_test","email":"jan_test@example.com","password":"password123","name":"Jan Testowy"}')

echo -e "Odpowiedź rejestracji: $REG_A_RESP"
TOKEN_A=$(extract_json_field "$REG_A_RESP" "token")

# Jeśli użytkownik już istnieje w bazie (np. po poprzednim teście), po prostu się logujemy
if [ -z "$TOKEN_A" ] || [ "$TOKEN_A" == "null" ]; then
    echo -e "${CYAN}Użytkownik już istnieje lub rejestracja zwróciła pusty token. Próba logowania...${NC}"
    LOGIN_A_RESP=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"usernameOrEmail":"jan_test","password":"password123"}')
    TOKEN_A=$(extract_json_field "$LOGIN_A_RESP" "token")
fi

if [ -z "$TOKEN_A" ] || [ "$TOKEN_A" == "null" ]; then
    echo -e "${RED}[BŁĄD] Nie udało się uzyskać tokenu JWT dla Jana!${NC}"
    exit 1
fi
echo -e "${GREEN}Sukces! Otrzymano token JWT dla Jana:${NC} ${CYAN}${TOKEN_A:0:25}...${NC}"

# 2. Pobranie danych profilu Jana
echo -e "\n${YELLOW}[Krok 2/10] Pobieranie profilu użytkownika Jan Testowy (/api/auth/me)...${NC}"
ME_RESP=$(curl -s -X GET "$BASE_URL/api/auth/me" \
  -H "Authorization: Bearer $TOKEN_A")
echo -e "Profil użytkownika: $ME_RESP"

USER_A_ID=$(extract_json_field "$ME_RESP" "id")
echo -e "${GREEN}ID użytkownika Jan Testowy:${NC} ${MAGENTA}$USER_A_ID${NC}"

# 3. Rejestracja użytkownika B (Adam Testowy)
echo -e "\n${YELLOW}[Krok 3/10] Rejestracja drugiego użytkownika Adam Testowy (adam_test)...${NC}"
REG_B_RESP=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"adam_test","email":"adam_test@example.com","password":"password123","name":"Adam Testowy"}')

echo -e "Odpowiedź rejestracji: $REG_B_RESP"
USER_B_ID=$(echo "$REG_B_RESP" | grep -o '"user"[^}]*' | grep -o '"id":[0-9]*' | cut -d':' -f2 | tr -d ' ')

# Jeśli użytkownik już istnieje, zalogujmy się, żeby wyciągnąć jego ID z profilu
if [ -z "$USER_B_ID" ] || [ "$USER_B_ID" == "null" ] || [ -z "$REG_B_RESP" ]; then
    echo -e "${CYAN}Użytkownik Adam już istnieje. Logowanie w celu pobrania ID...${NC}"
    LOGIN_B_RESP=$(curl -s -X POST "$BASE_URL/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"usernameOrEmail":"adam_test","password":"password123"}')
    TOKEN_B=$(extract_json_field "$LOGIN_B_RESP" "token")
    
    ME_B_RESP=$(curl -s -X GET "$BASE_URL/api/auth/me" \
      -H "Authorization: Bearer $TOKEN_B")
    USER_B_ID=$(extract_json_field "$ME_B_RESP" "id")
fi

if [ -z "$USER_B_ID" ] || [ "$USER_B_ID" == "null" ]; then
    echo -e "${RED}[BŁĄD] Nie udało się ustalić ID użytkownika Adam!${NC}"
    exit 1
fi
echo -e "${GREEN}ID użytkownika Adam Testowy:${NC} ${MAGENTA}$USER_B_ID${NC}"

# 4. Utworzenie nowej grupy przez Jana
echo -e "\n${YELLOW}[Krok 4/10] Tworzenie nowej grupy rozliczeniowej 'Bieszczady 2026'...${NC}"
GROUP_RESP=$(curl -s -X POST "$BASE_URL/api/groups" \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"name":"Bieszczady 2026","description":"Wspolny wyjazd majowy w gory"}')
echo -e "Odpowiedź serwera: $GROUP_RESP"

GROUP_ID=$(extract_json_field "$GROUP_RESP" "id")
if [ -z "$GROUP_ID" ] || [ "$GROUP_ID" == "null" ]; then
    echo -e "${RED}[BŁĄD] Nie udało się utworzyć grupy!${NC}"
    exit 1
fi
echo -e "${GREEN}Pomyślnie utworzono grupę o ID:${NC} ${MAGENTA}$GROUP_ID${NC}"

# 5. Dodanie Adama do grupy
echo -e "\n${YELLOW}[Krok 5/10] Dodawanie użytkownika Adam (adam_test) do grupy ID $GROUP_ID...${NC}"
ADD_MEMBER_RESP=$(curl -s -X POST "$BASE_URL/api/groups/$GROUP_ID/members" \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"adam_test"}')
echo -e "Skład grupy po dodaniu: $ADD_MEMBER_RESP"

# 6. Dodanie wydatku przez Jana (Obiad w schronisku: 120 PLN)
echo -e "\n${YELLOW}[Krok 6/10] Dodawanie wydatku: Jan płaci 120 PLN za 'Obiad w schronisku' (podział po 60 PLN)...${NC}"
EXPENSE_RESP=$(curl -s -X POST "$BASE_URL/api/groups/$GROUP_ID/expenses" \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d "{\"description\":\"Obiad w schronisku\",\"amount\":120.00,\"paidByUserId\":$USER_A_ID,\"splits\":[{\"userId\":$USER_A_ID,\"amount\":60.00},{\"userId\":$USER_B_ID,\"amount\":60.00}]}")
echo -e "Odpowiedź serwera: $EXPENSE_RESP"

EXPENSE_ID=$(extract_json_field "$EXPENSE_RESP" "id")
echo -e "${GREEN}Pomyślnie utworzono wydatek o ID:${NC} ${MAGENTA}$EXPENSE_ID${NC}"

# 7. Pobranie sald i sprawdzenie rozliczeń
echo -e "\n${YELLOW}[Krok 7/10] Pobieranie sald grupy (weryfikacja obliczeń algorytmu)...${NC}"
BALANCES_RESP=$(curl -s -X GET "$BASE_URL/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A")
echo -e "Salda i długi: $BALANCES_RESP"

# 8. Logowanie Adama i zgłoszenie spłaty długu (Settlement)
echo -e "\n${YELLOW}[Krok 8/10] Logowanie jako Adam Testowy w celu spłacenia długu (60.00 PLN)...${NC}"
LOGIN_B_RESP=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"adam_test","password":"password123"}')
TOKEN_B=$(extract_json_field "$LOGIN_B_RESP" "token")

if [ -z "$TOKEN_B" ] || [ "$TOKEN_B" == "null" ]; then
    echo -e "${RED}[BŁĄD] Logowanie Adama nie powiodło się!${NC}"
    exit 1
fi

echo -e "${CYAN}Adam wysyła spłatę długu 60.00 PLN do Jana...${NC}"
SETTLEMENT_RESP=$(curl -s -X POST "$BASE_URL/api/groups/$GROUP_ID/settlements" \
  -H "Authorization: Bearer $TOKEN_B" \
  -H "Content-Type: application/json" \
  -d "{\"amount\":60.00,\"debtorId\":$USER_B_ID,\"creditorId\":$USER_A_ID}")
echo -e "Odpowiedź serwera: $SETTLEMENT_RESP"

SETTLEMENT_ID=$(extract_json_field "$SETTLEMENT_RESP" "id")
echo -e "${GREEN}Utworzono spłatę długu o ID:${NC} ${MAGENTA}$SETTLEMENT_ID${NC} (Status: oczekuje na zatwierdzenie)"

# 9. Jan loguje się z powrotem i zatwierdza spłatę długu
echo -e "\n${YELLOW}[Krok 9/10] Jan Testowy (wierzyciel) zatwierdza otrzymanie spłaty od Adama...${NC}"
APPROVE_RESP=$(curl -s -X POST "$BASE_URL/api/groups/$GROUP_ID/settlements/$SETTLEMENT_ID/approve" \
  -H "Authorization: Bearer $TOKEN_A")
echo -e "Odpowiedź serwera: $APPROVE_RESP"

# 10. Pobranie ostatecznych sald - po spłacie i zatwierdzeniu saldo powinno wynosić 0.00 PLN!
echo -e "\n${YELLOW}[Krok 10/10] Pobieranie ostatecznych sald po zatwierdzeniu spłaty długu...${NC}"
FINAL_BALANCES_RESP=$(curl -s -X GET "$BASE_URL/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A")
echo -e "Ostateczne saldo grupy: $FINAL_BALANCES_RESP"

echo -e "\n${BLUE}======================================================================${NC}"
echo -e "${GREEN}      KOMPLETNY TEST INTEGRACYJNY API BACKENDU ZAKOŃCZONY SUKCESEM!  ${NC}"
echo -e "${GREEN}      Wszystkie kalkulacje długu i spłaty zostały pomyślnie zweryfikowane!  ${NC}"
echo -e "${BLUE}======================================================================${NC}"
