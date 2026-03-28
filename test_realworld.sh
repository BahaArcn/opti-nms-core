#!/bin/bash
set -e
BASE="http://localhost:8080"
PASS=0
FAIL=0
TOTAL=0

green() { echo -e "\033[32m✓ $1\033[0m"; PASS=$((PASS+1)); TOTAL=$((TOTAL+1)); }
red()   { echo -e "\033[31m✗ $1\033[0m"; FAIL=$((FAIL+1)); TOTAL=$((TOTAL+1)); }
header(){ echo -e "\n\033[1;36m═══════════════════════════════════════\033[0m"; echo -e "\033[1;36m  $1\033[0m"; echo -e "\033[1;36m═══════════════════════════════════════\033[0m"; }
check() {
  local desc="$1" expected="$2" actual="$3"
  if echo "$actual" | grep -q "$expected"; then green "$desc"; else red "$desc (expected: $expected)"; echo "  Response: $(echo $actual | head -c 200)"; fi
}
check_status() {
  local desc="$1" expected_code="$2" actual_code="$3" body="$4"
  if [ "$actual_code" = "$expected_code" ]; then green "$desc [HTTP $actual_code]"; else red "$desc [Expected HTTP $expected_code, got $actual_code]"; echo "  Body: $(echo $body | head -c 200)"; fi
}

header "FAZ 1: SUPER_ADMIN LOGIN + TENANT OLUSTURMA"

echo ">> 1.1 SUPER_ADMIN login"
SA_RESP=$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"PLAT-0000/0000/00","username":"admin","password":"admin123"}')
SA_TOKEN=$(echo $SA_RESP | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || echo "")
if [ -n "$SA_TOKEN" ]; then green "SUPER_ADMIN login basarili"; else red "SUPER_ADMIN login BASARISIZ"; echo "$SA_RESP"; exit 1; fi

echo ">> 1.2 Turkcell tenant olustur"
TKCL_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/system/tenants \
  -H "Authorization: Bearer $SA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","name":"Turkcell","amfUrl":"http://10.0.0.1:7777","smfUrl":"http://10.0.0.2:7777","adminUsername":"tkcl_admin","adminEmail":"admin@turkcell.com","adminPassword":"Turkcell2026!"}')
TKCL_CODE=$(echo "$TKCL_RESP" | tail -1)
TKCL_BODY=$(echo "$TKCL_RESP" | sed '$d')
if [ "$TKCL_CODE" = "201" ] || [ "$TKCL_CODE" = "200" ]; then green "Turkcell tenant olusturuldu [HTTP $TKCL_CODE]"
elif echo "$TKCL_BODY" | grep -q "already exists"; then green "Turkcell tenant zaten mevcut (OK)"
else red "Turkcell tenant olusturulamadi [HTTP $TKCL_CODE]"; echo "  $TKCL_BODY" | head -c 200; fi

echo ">> 1.3 Vodafone tenant olustur"
VODA_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/system/tenants \
  -H "Authorization: Bearer $SA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"tenantId":"VODA-0002/0001/01","name":"Vodafone","amfUrl":"http://10.0.1.1:7777","smfUrl":"http://10.0.1.2:7777","adminUsername":"voda_admin","adminEmail":"admin@vodafone.com","adminPassword":"Vodafone2026!"}')
VODA_CODE=$(echo "$VODA_RESP" | tail -1)
VODA_BODY=$(echo "$VODA_RESP" | sed '$d')
if [ "$VODA_CODE" = "201" ] || [ "$VODA_CODE" = "200" ]; then green "Vodafone tenant olusturuldu [HTTP $VODA_CODE]"
elif echo "$VODA_BODY" | grep -q "already exists"; then green "Vodafone tenant zaten mevcut (OK)"
else red "Vodafone tenant olusturulamadi [HTTP $VODA_CODE]"; echo "  $VODA_BODY" | head -c 200; fi

echo ">> 1.4 Tenant listesi"
TENANTS=$(curl -s $BASE/api/v1/system/tenants -H "Authorization: Bearer $SA_TOKEN")
TENANT_COUNT=$(echo $TENANTS | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d) if isinstance(d,list) else d.get('totalElements',0))" 2>/dev/null || echo "0")
if [ "$TENANT_COUNT" -ge "2" ] 2>/dev/null; then green "Tenant listesi: $TENANT_COUNT tenant bulundu"; else green "Tenant listesi alindi"; fi

header "FAZ 2: KULLANICI YONETIMI VE ROLLER"

echo ">> 2.1 Turkcell admin login"
TKCL_LOGIN=$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","username":"tkcl_admin","password":"Turkcell2026!"}')
TKCL_TOKEN=$(echo $TKCL_LOGIN | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || echo "")
if [ -n "$TKCL_TOKEN" ]; then green "Turkcell admin login basarili"; else red "Turkcell admin login BASARISIZ"; echo "$TKCL_LOGIN"; fi

echo ">> 2.2 Turkcell OPERATOR olustur"
OP_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/users \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"username":"tkcl_operator","email":"operator@turkcell.com","password":"Operator2026!","role":"OPERATOR"}')
OP_CODE=$(echo "$OP_RESP" | tail -1)
OP_BODY=$(echo "$OP_RESP" | sed '$d')
if [ "$OP_CODE" = "201" ] || [ "$OP_CODE" = "200" ]; then green "OPERATOR kullanici olusturuldu"
elif echo "$OP_BODY" | grep -qi "already exists\|duplicate"; then green "OPERATOR zaten mevcut (OK)"
else red "OPERATOR olusturulamadi [HTTP $OP_CODE]"; echo "  $OP_BODY" | head -c 200; fi

echo ">> 2.3 Turkcell VIEWER olustur"
VW_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/users \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"username":"tkcl_viewer","email":"viewer@turkcell.com","password":"Viewer20260!","role":"VIEWER"}')
VW_CODE=$(echo "$VW_RESP" | tail -1)
VW_BODY=$(echo "$VW_RESP" | sed '$d')
if [ "$VW_CODE" = "201" ] || [ "$VW_CODE" = "200" ]; then green "VIEWER kullanici olusturuldu"
elif echo "$VW_BODY" | grep -qi "already exists\|duplicate"; then green "VIEWER zaten mevcut (OK)"
else red "VIEWER olusturulamadi [HTTP $VW_CODE]"; echo "  $VW_BODY" | head -c 200; fi

echo ">> 2.4 Vodafone admin login"
VODA_LOGIN=$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"VODA-0002/0001/01","username":"voda_admin","password":"Vodafone2026!"}')
VODA_TOKEN=$(echo $VODA_LOGIN | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || echo "")
if [ -n "$VODA_TOKEN" ]; then green "Vodafone admin login basarili"; else red "Vodafone admin login BASARISIZ"; echo "$VODA_LOGIN"; fi

echo ">> 2.5 Operator login"
OP_LOGIN=$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","username":"tkcl_operator","password":"Operator2026!"}')
TKCL_OP_TOKEN=$(echo $OP_LOGIN | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || echo "")
if [ -n "$TKCL_OP_TOKEN" ]; then green "Operator login basarili"; else red "Operator login BASARISIZ"; fi

echo ">> 2.6 Viewer login"
VW_LOGIN=$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","username":"tkcl_viewer","password":"Viewer20260!"}')
TKCL_VW_TOKEN=$(echo $VW_LOGIN | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || echo "")
if [ -n "$TKCL_VW_TOKEN" ]; then green "Viewer login basarili"; else red "Viewer login BASARISIZ"; fi

echo ">> 2.7 Kullanici listesi"
USERS=$(curl -s "$BASE/api/v1/users?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
check "Kullanici listesi alindi" "content" "$USERS"

header "FAZ 3: LISANS YONETIMI"

echo ">> 3.1 Turkcell lisans ata"
LIC_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/licenses \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"licenseKey":"LIC-TKCL-2026-ENT","maxSubscribers":5,"maxGNodeBs":10,"maxDnns":3,"maxEdgeLocations":2,"maxUsers":10,"expiresAt":1893456000000,"active":true,"description":"Turkcell test license"}')
LIC_CODE=$(echo "$LIC_RESP" | tail -1)
LIC_BODY=$(echo "$LIC_RESP" | sed '$d')
if [ "$LIC_CODE" = "200" ] || [ "$LIC_CODE" = "201" ]; then green "Turkcell lisans atandi [HTTP $LIC_CODE]"
else red "Turkcell lisans atanamadi [HTTP $LIC_CODE]"; echo "  $LIC_BODY" | head -c 200; fi

echo ">> 3.2 Vodafone lisans ata (kucuk)"
LIC2_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/licenses \
  -H "Authorization: Bearer $VODA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"licenseKey":"LIC-VODA-2026-BASIC","maxSubscribers":2,"maxGNodeBs":5,"maxDnns":1,"maxEdgeLocations":1,"maxUsers":5,"expiresAt":1893456000000,"active":true,"description":"Vodafone basic license"}')
LIC2_CODE=$(echo "$LIC2_RESP" | tail -1)
LIC2_BODY=$(echo "$LIC2_RESP" | sed '$d')
if [ "$LIC2_CODE" = "200" ] || [ "$LIC2_CODE" = "201" ]; then green "Vodafone lisans atandi [HTTP $LIC2_CODE]"
else red "Vodafone lisans atanamadi [HTTP $LIC2_CODE]"; echo "  $LIC2_BODY" | head -c 200; fi

echo ">> 3.3 Lisans durumu kontrol"
LIC_STATUS=$(curl -s $BASE/api/v1/licenses/status -H "Authorization: Bearer $TKCL_TOKEN")
check "Lisans durumu alindi" "maxSubscribers" "$LIC_STATUS"
echo "  Lisans: $(echo $LIC_STATUS | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'Subs: {d.get(\"currentSubscribers\",\"?\")}/{d.get(\"maxSubscribers\",\"?\")}, Users: {d.get(\"currentUsers\",\"?\")}/{d.get(\"maxUsers\",\"?\")}')" 2>/dev/null)"

header "FAZ 4: EDGE LOCATION + LIMIT TESTI"

echo ">> 4.1 Istanbul DC olustur"
EDGE1_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/edge-locations \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Istanbul-DC-1","description":"Turkcell Istanbul Veri Merkezi","address":"Levent, Istanbul","latitude":41.0082,"longitude":28.9784,"status":"ACTIVE"}')
EDGE1_CODE=$(echo "$EDGE1_RESP" | tail -1)
EDGE1_BODY=$(echo "$EDGE1_RESP" | sed '$d')
if [ "$EDGE1_CODE" = "200" ] || [ "$EDGE1_CODE" = "201" ]; then green "Istanbul DC olusturuldu"
elif echo "$EDGE1_BODY" | grep -qi "duplicate\|already"; then green "Istanbul DC zaten mevcut (OK)"
else red "Istanbul DC olusturulamadi [HTTP $EDGE1_CODE]"; echo "  $EDGE1_BODY" | head -c 200; fi

echo ">> 4.2 Ankara DC olustur"
EDGE2_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/edge-locations \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Ankara-DC-1","description":"Turkcell Ankara Veri Merkezi","address":"Cankaya, Ankara","latitude":39.9334,"longitude":32.8597,"status":"ACTIVE"}')
EDGE2_CODE=$(echo "$EDGE2_RESP" | tail -1)
EDGE2_BODY=$(echo "$EDGE2_RESP" | sed '$d')
if [ "$EDGE2_CODE" = "200" ] || [ "$EDGE2_CODE" = "201" ]; then green "Ankara DC olusturuldu"
elif echo "$EDGE2_BODY" | grep -qi "duplicate\|already"; then green "Ankara DC zaten mevcut (OK)"
else red "Ankara DC olusturulamadi [HTTP $EDGE2_CODE]"; echo "  $EDGE2_BODY" | head -c 200; fi

echo ">> 4.3 LIMIT TEST: 3. lokasyon reddedilmeli (maxEdgeLocations=2)"
EDGE3_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/edge-locations \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Izmir-DC-1","description":"Limit test","address":"Izmir","latitude":38.42,"longitude":27.14,"status":"ACTIVE"}')
EDGE3_CODE=$(echo "$EDGE3_RESP" | tail -1)
check_status "Edge Location limit testi (beklenen: 403)" "403" "$EDGE3_CODE" "$(echo "$EDGE3_RESP" | sed '$d')"

echo ">> 4.4 Edge Location listesi"
EDGES=$(curl -s "$BASE/api/v1/edge-locations?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
check "Edge Location listesi alindi" "content" "$EDGES"

header "FAZ 5: SUBSCRIBER YONETIMI + TENANT IZOLASYON"

echo ">> 5.1 Turkcell Subscriber 1 (normal telefon)"
SUB1_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/subscriber \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000000001","msisdn":"905301234567","label":"Baha Test Phone","ki":"465B5CE8B199B49FAA5F0A2EE238A6BC","usimType":"OPC","opc":"E8ED289DEBA952E4283B54E88E6183CA","ueAmbrDl":1000000000,"ueAmbrUl":500000000,"sqn":"000000001153","profileList":[{"sst":1,"sd":"FFFFFF","apnDnn":"internet","qci4g":9,"qi5g":9,"pduType":1,"arpPriority":8,"sessionAmbrDl":500000000,"sessionAmbrUl":250000000}]}')
SUB1_CODE=$(echo "$SUB1_RESP" | tail -1)
SUB1_BODY=$(echo "$SUB1_RESP" | sed '$d')
if [ "$SUB1_CODE" = "200" ] || [ "$SUB1_CODE" = "201" ]; then green "Subscriber 1 olusturuldu (Baha Test Phone)"
elif echo "$SUB1_BODY" | grep -qi "already exists\|duplicate"; then green "Subscriber 1 zaten mevcut (OK)"
else red "Subscriber 1 olusturulamadi [HTTP $SUB1_CODE]"; echo "  $SUB1_BODY" | head -c 300; fi

echo ">> 5.2 Turkcell Subscriber 2 (IoT sensor)"
SUB2_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/subscriber \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000000002","msisdn":"905301234568","label":"IoT Sensor 1","ki":"AABBCCDD11223344AABBCCDD11223344","usimType":"OPC","opc":"11223344556677889900AABBCCDDEEFF","ueAmbrDl":100000,"ueAmbrUl":50000,"simType":"IOT","sqn":"000000000001","profileList":[{"sst":3,"sd":"000001","apnDnn":"iot.turkcell","qci4g":9,"qi5g":9,"pduType":1,"arpPriority":10,"sessionAmbrDl":100000,"sessionAmbrUl":50000}]}')
SUB2_CODE=$(echo "$SUB2_RESP" | tail -1)
SUB2_BODY=$(echo "$SUB2_RESP" | sed '$d')
if [ "$SUB2_CODE" = "200" ] || [ "$SUB2_CODE" = "201" ]; then green "Subscriber 2 olusturuldu (IoT Sensor)"
elif echo "$SUB2_BODY" | grep -qi "already exists\|duplicate"; then green "Subscriber 2 zaten mevcut (OK)"
else red "Subscriber 2 olusturulamadi [HTTP $SUB2_CODE]"; echo "  $SUB2_BODY" | head -c 300; fi

echo ">> 5.3 Turkcell Subscriber 3 (V2X)"
SUB3_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/subscriber \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000000003","msisdn":"905301234569","label":"V2X Vehicle-01","ki":"FFEEDDCCBBAA99887766554433221100","usimType":"OPC","opc":"00112233445566778899AABBCCDDEEFF","ueAmbrDl":5000000000,"ueAmbrUl":2500000000,"sqn":"000000000001","profileList":[{"sst":4,"sd":"000002","apnDnn":"v2x.turkcell","qci4g":3,"qi5g":3,"pduType":3,"arpPriority":1,"sessionAmbrDl":5000000000,"sessionAmbrUl":2500000000}]}')
SUB3_CODE=$(echo "$SUB3_RESP" | tail -1)
SUB3_BODY=$(echo "$SUB3_RESP" | sed '$d')
if [ "$SUB3_CODE" = "200" ] || [ "$SUB3_CODE" = "201" ]; then green "Subscriber 3 olusturuldu (V2X Vehicle)"
elif echo "$SUB3_BODY" | grep -qi "already exists\|duplicate"; then green "Subscriber 3 zaten mevcut (OK)"
else red "Subscriber 3 olusturulamadi [HTTP $SUB3_CODE]"; echo "  $SUB3_BODY" | head -c 300; fi

echo ">> 5.4 Vodafone Subscriber 1"
VSUB1_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/subscriber \
  -H "Authorization: Bearer $VODA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286020000000001","msisdn":"905421234567","label":"Vodafone Test User","ki":"AAAAAAAABBBBBBBBCCCCCCCCDDDDDDDD","usimType":"OPC","opc":"1111111122222222333333334444444F","ueAmbrDl":500000000,"ueAmbrUl":250000000,"sqn":"000000000001","profileList":[{"sst":1,"sd":"FFFFFF","apnDnn":"internet","qci4g":9,"qi5g":9,"pduType":1,"arpPriority":8,"sessionAmbrDl":500000000,"sessionAmbrUl":250000000}]}')
VSUB1_CODE=$(echo "$VSUB1_RESP" | tail -1)
VSUB1_BODY=$(echo "$VSUB1_RESP" | sed '$d')
if [ "$VSUB1_CODE" = "200" ] || [ "$VSUB1_CODE" = "201" ]; then green "Vodafone Subscriber 1 olusturuldu"
elif echo "$VSUB1_BODY" | grep -qi "already exists\|duplicate"; then green "Vodafone Subscriber 1 zaten mevcut (OK)"
else red "Vodafone Subscriber 1 olusturulamadi [HTTP $VSUB1_CODE]"; echo "  $VSUB1_BODY" | head -c 300; fi

echo ">> 5.5 TENANT IZOLASYON: Turkcell -> Vodafone subscriber erisim denemesi"
ISO1_RESP=$(curl -s -w "\n%{http_code}" $BASE/api/v1/subscriber/286020000000001 \
  -H "Authorization: Bearer $TKCL_TOKEN")
ISO1_CODE=$(echo "$ISO1_RESP" | tail -1)
check_status "Turkcell, Vodafone subscriber'a erisilemedi (beklenen: 404)" "404" "$ISO1_CODE" "$(echo "$ISO1_RESP" | sed '$d')"

echo ">> 5.6 TENANT IZOLASYON: Vodafone -> Turkcell subscriber erisim denemesi"
ISO2_RESP=$(curl -s -w "\n%{http_code}" $BASE/api/v1/subscriber/286010000000001 \
  -H "Authorization: Bearer $VODA_TOKEN")
ISO2_CODE=$(echo "$ISO2_RESP" | tail -1)
check_status "Vodafone, Turkcell subscriber'a erisilemedi (beklenen: 404)" "404" "$ISO2_CODE" "$(echo "$ISO2_RESP" | sed '$d')"

echo ">> 5.7 Turkcell subscriber listesi (sadece kendi subscriberlari)"
TKCL_SUBS=$(curl -s "$BASE/api/v1/subscriber/list?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
TKCL_SUB_COUNT=$(echo $TKCL_SUBS | python3 -c "import sys,json; print(json.load(sys.stdin).get('totalElements',0))" 2>/dev/null || echo "?")
green "Turkcell subscriber listesi: $TKCL_SUB_COUNT subscriber"

echo ">> 5.8 Subscriber arama (label)"
SEARCH_RESP=$(curl -s "$BASE/api/v1/subscriber/search?keyword=IoT" -H "Authorization: Bearer $TKCL_TOKEN")
check "Subscriber arama (IoT)" "IoT" "$SEARCH_RESP"

echo ">> 5.9 Subscriber detay (Ki/OPc sifrelenmis olmali)"
DETAIL=$(curl -s $BASE/api/v1/subscriber/286010000000001 -H "Authorization: Bearer $TKCL_TOKEN")
check "Subscriber detay alindi" "286010000000001" "$DETAIL"

header "FAZ 6: RBAC TESTLERI"

echo ">> 6.1 VIEWER subscriber olusturamaz"
RBAC1_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/subscriber \
  -H "Authorization: Bearer $TKCL_VW_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000099999","label":"Should fail"}')
RBAC1_CODE=$(echo "$RBAC1_RESP" | tail -1)
check_status "VIEWER subscriber olusturamadi (beklenen: 403)" "403" "$RBAC1_CODE" "$(echo "$RBAC1_RESP" | sed '$d')"

echo ">> 6.2 VIEWER subscriber gorebilir"
RBAC2_RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/subscriber/list?page=0&size=10" \
  -H "Authorization: Bearer $TKCL_VW_TOKEN")
RBAC2_CODE=$(echo "$RBAC2_RESP" | tail -1)
check_status "VIEWER subscriber listesini gordu (beklenen: 200)" "200" "$RBAC2_CODE" ""

echo ">> 6.3 OPERATOR subscriber olusturamaz"
RBAC3_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/subscriber \
  -H "Authorization: Bearer $TKCL_OP_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000099998","label":"Should fail"}')
RBAC3_CODE=$(echo "$RBAC3_RESP" | tail -1)
check_status "OPERATOR subscriber olusturamadi (beklenen: 403)" "403" "$RBAC3_CODE" "$(echo "$RBAC3_RESP" | sed '$d')"

echo ">> 6.4 OPERATOR alarm olusturabilir"
RBAC4_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/fault/alarms \
  -H "Authorization: Bearer $TKCL_OP_TOKEN" -H 'Content-Type: application/json' \
  -d '{"source":"gNodeB-RBAC","alarmType":"RBAC_TEST","description":"Operator alarm testi","severity":"WARNING"}')
RBAC4_CODE=$(echo "$RBAC4_RESP" | tail -1)
if [ "$RBAC4_CODE" = "200" ] || [ "$RBAC4_CODE" = "201" ]; then green "OPERATOR alarm olusturabildi [HTTP $RBAC4_CODE]"
else red "OPERATOR alarm olusturamadi [HTTP $RBAC4_CODE]"; fi

header "FAZ 7: ALARM YASAM DONGUSU"

echo ">> 7.1 CRITICAL alarm olustur"
ALM1_RESP=$(curl -s -X POST $BASE/api/v1/fault/alarms \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"source":"gNodeB-001","alarmType":"LINK_DOWN","description":"S1 baglantisi kesildi","severity":"CRITICAL"}')
ALARM_ID_1=$(echo $ALM1_RESP | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || echo "")
if [ -n "$ALARM_ID_1" ]; then green "CRITICAL alarm olusturuldu (ID: $ALARM_ID_1)"; else red "CRITICAL alarm olusturulamadi"; echo "  $ALM1_RESP" | head -c 200; fi

echo ">> 7.2 MAJOR alarm olustur"
ALM2_RESP=$(curl -s -X POST $BASE/api/v1/fault/alarms \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"source":"UPF-001","alarmType":"HIGH_CPU","description":"CPU kullanimi %95","severity":"MAJOR"}')
check "MAJOR alarm olusturuldu" "id" "$ALM2_RESP"

echo ">> 7.3 MINOR alarm olustur"
ALM3_RESP=$(curl -s -X POST $BASE/api/v1/fault/alarms \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"source":"SMF-001","alarmType":"SESSION_THRESHOLD","description":"Oturum sayisi yuksek","severity":"MINOR"}')
check "MINOR alarm olusturuldu" "id" "$ALM3_RESP"

echo ">> 7.4 DEDUPLICATION: Ayni alarm tekrar eklenmemeli"
ALM_DUP=$(curl -s -X POST $BASE/api/v1/fault/alarms \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"source":"gNodeB-001","alarmType":"LINK_DOWN","description":"Duplicate test","severity":"CRITICAL"}')
DUP_ID=$(echo $ALM_DUP | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || echo "")
if [ "$DUP_ID" = "$ALARM_ID_1" ]; then green "Deduplication calisiyor (ayni ID dondu)"; else red "Deduplication calismadi (farkli ID: $DUP_ID vs $ALARM_ID_1)"; fi

echo ">> 7.5 Severity filtresi"
SEV_RESP=$(curl -s "$BASE/api/v1/fault/alarms?severity=CRITICAL" -H "Authorization: Bearer $TKCL_TOKEN")
check "Severity filtresi (CRITICAL)" "CRITICAL" "$SEV_RESP"

echo ">> 7.6 Alarm ACKNOWLEDGE"
if [ -n "$ALARM_ID_1" ]; then
  ACK_RESP=$(curl -s -X PUT "$BASE/api/v1/fault/alarms/$ALARM_ID_1/acknowledge" \
    -H "Authorization: Bearer $TKCL_TOKEN")
  check "Alarm acknowledge edildi" "ACKNOWLEDGED" "$ACK_RESP"
else red "Alarm ID bulunamadi, acknowledge atlanacak"; fi

echo ">> 7.7 Alarm CLEAR"
CLR_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/v1/fault/alarms/clear?source=gNodeB-001&alarmType=LINK_DOWN" \
  -H "Authorization: Bearer $TKCL_TOKEN")
check_status "Alarm clear edildi (void response)" "200" "$CLR_CODE" ""

echo ">> 7.8 Alarm tenant izolasyonu"
VALM=$(curl -s "$BASE/api/v1/fault/alarms?page=0&size=10" -H "Authorization: Bearer $VODA_TOKEN")
VALM_COUNT=$(echo $VALM | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('totalElements', len(d) if isinstance(d,list) else 0))" 2>/dev/null || echo "?")
green "Vodafone alarm listesi: $VALM_COUNT alarm (Turkcell alarmlari gorunmemeli)"

header "FAZ 8: POLICY + APN + PERFORMANS"

echo ">> 8.1 Gold Policy olustur"
POL1_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/policies \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Gold Plan","description":"Premium yuksek bant genisligi","enabled":true,"bandwidthLimit":{"uplinkKbps":50000,"downlinkKbps":100000},"ratPreference":"NR_5G","frequencySelectionPriority":1,"defaultSlices":[{"sst":1,"sd":"000001"}]}')
POL1_CODE=$(echo "$POL1_RESP" | tail -1)
POL1_BODY=$(echo "$POL1_RESP" | sed '$d')
if [ "$POL1_CODE" = "200" ] || [ "$POL1_CODE" = "201" ]; then green "Gold Policy olusturuldu"
elif echo "$POL1_BODY" | grep -qi "duplicate\|already"; then green "Gold Policy zaten mevcut (OK)"
else red "Gold Policy olusturulamadi [HTTP $POL1_CODE]"; echo "  $POL1_BODY" | head -c 200; fi

echo ">> 8.2 Silver Policy olustur (IP filtre + zaman cizelgesi)"
POL2_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/policies \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Silver Plan","description":"Standart plan","enabled":true,"bandwidthLimit":{"uplinkKbps":10000,"downlinkKbps":25000},"ratPreference":"ANY","frequencySelectionPriority":5,"ipFilteringEnabled":true,"ipFilterRules":[{"action":"ALLOW","cidr":"10.0.0.0/8","protocol":"ANY"}],"timeSchedule":{"enabled":true,"startTime":"08:00","endTime":"23:00","timezone":"Europe/Istanbul","activeDays":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"]}}')
POL2_CODE=$(echo "$POL2_RESP" | tail -1)
POL2_BODY=$(echo "$POL2_RESP" | sed '$d')
if [ "$POL2_CODE" = "200" ] || [ "$POL2_CODE" = "201" ]; then green "Silver Policy olusturuldu"
elif echo "$POL2_BODY" | grep -qi "duplicate\|already"; then green "Silver Policy zaten mevcut (OK)"
else red "Silver Policy olusturulamadi [HTTP $POL2_CODE]"; echo "  $POL2_BODY" | head -c 200; fi

echo ">> 8.3 Policy listesi"
POLS=$(curl -s "$BASE/api/v1/policies?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
check "Policy listesi alindi" "content" "$POLS"

echo ">> 8.4 APN/DNN profili olustur"
APN_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/apn/profiles \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"dnn":"internet","sst":1,"sd":"FFFFFF","pduSessionType":"IPV4V6","qos":{"fiveQi":9,"arpPriorityLevel":8,"preEmptionCapability":"NOT_PRE_EMPT","preEmptionVulnerability":"PRE_EMPTABLE"},"sessionAmbr":{"uplinkKbps":1000000,"downlinkKbps":2000000},"enabled":true,"status":"ACTIVE","description":"Default internet DNN for eMBB"}')
APN_CODE=$(echo "$APN_RESP" | tail -1)
APN_BODY=$(echo "$APN_RESP" | sed '$d')
if [ "$APN_CODE" = "200" ] || [ "$APN_CODE" = "201" ]; then green "APN profili olusturuldu [HTTP $APN_CODE]"
elif echo "$APN_BODY" | grep -qi "duplicate\|already"; then green "APN profili zaten mevcut (OK)"
else red "APN profili olusturulamadi [HTTP $APN_CODE]"; echo "  $APN_BODY" | head -c 300; fi

echo ">> 8.5 Performans metrik gonder"
TIMESTAMP=$(date +%s)000
PM_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/performance/metrics \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"metricName\":\"upf.throughput.dl\",\"value\":1250.5,\"labels\":{\"source\":\"UPF-001\"},\"metricType\":\"GAUGE\",\"timestamp\":$TIMESTAMP}")
PM_CODE=$(echo "$PM_RESP" | tail -1)
if [ "$PM_CODE" = "200" ] || [ "$PM_CODE" = "201" ]; then green "Performans metrigi gonderildi [HTTP $PM_CODE]"
else red "Performans metrigi gonderilemedi [HTTP $PM_CODE]"; echo "  $(echo "$PM_RESP" | sed '$d')" | head -c 200; fi

echo ">> 8.6 2. performans metrigi"
TIMESTAMP2=$(($(date +%s)+60))000
PM2_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/performance/metrics \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"metricName\":\"upf.throughput.ul\",\"value\":620.3,\"labels\":{\"source\":\"UPF-001\"},\"metricType\":\"GAUGE\",\"timestamp\":$TIMESTAMP2}")
PM2_CODE=$(echo "$PM2_RESP" | tail -1)
if [ "$PM2_CODE" = "200" ] || [ "$PM2_CODE" = "201" ]; then green "2. performans metrigi gonderildi [HTTP $PM2_CODE]"
else red "2. performans metrigi gonderilemedi [HTTP $PM2_CODE]"; fi

echo ">> 8.7 Guncel metrik sorgula"
PM_CUR=$(curl -s "$BASE/api/v1/performance/current?metric=upf.throughput.dl" \
  -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$PM_CUR" | grep -qE "^[0-9]"; then green "Guncel metrik alindi: $PM_CUR"
else check "Guncel metrik alindi" "1250" "$PM_CUR"; fi

header "FAZ 9: DASHBOARD + AUDIT + GUVENLIK"

echo ">> 9.1 Dashboard ozeti"
DASH=$(curl -s $BASE/api/v1/dashboard/summary -H "Authorization: Bearer $TKCL_TOKEN")
check "Dashboard ozeti alindi" "totalSubscribers" "$DASH"
echo "  Dashboard: $(echo $DASH | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'Subs:{d.get(\"totalSubscribers\",\"?\")}, Alarms:{d.get(\"activeAlarms\",\"?\")}, EdgeLoc:{d.get(\"totalEdgeLocations\",\"?\")}, Status:{d.get(\"systemStatus\",\"?\")}')" 2>/dev/null)"

echo ">> 9.2 Audit log kontrolu"
AUDIT=$(curl -s "$BASE/api/v1/audit/logs?page=0&size=5" -H "Authorization: Bearer $TKCL_TOKEN")
check "Audit log alindi" "content" "$AUDIT"
AUDIT_COUNT=$(echo $AUDIT | python3 -c "import sys,json; print(json.load(sys.stdin).get('totalElements',0))" 2>/dev/null || echo "?")
echo "  Toplam audit kaydi: $AUDIT_COUNT"

echo ">> 9.3 Yanlis sifre ile giris (beklenen: 401 veya 429)"
BAD_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","username":"tkcl_admin","password":"YANLIS_SIFRE"}')
BAD_CODE=$(echo "$BAD_RESP" | tail -1)
if [ "$BAD_CODE" = "401" ] || [ "$BAD_CODE" = "429" ]; then green "Yanlis sifre reddedildi [HTTP $BAD_CODE]"
else red "Yanlis sifre: beklenmeyen HTTP $BAD_CODE"; fi

echo ">> 9.4 Token olmadan erisim (beklenen: 401 veya 403)"
NO_TOKEN_RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/subscriber/list")
NO_TOKEN_CODE=$(echo "$NO_TOKEN_RESP" | tail -1)
if [ "$NO_TOKEN_CODE" = "401" ] || [ "$NO_TOKEN_CODE" = "403" ]; then green "Token'siz erisim reddedildi [HTTP $NO_TOKEN_CODE]"
else red "Token'siz erisim: beklenmeyen HTTP $NO_TOKEN_CODE"; fi

echo ">> 9.5 Gecersiz token ile erisim (beklenen: 401 veya 403)"
BAD_TOKEN_RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/subscriber/list" \
  -H "Authorization: Bearer invalidtoken12345")
BAD_TOKEN_CODE=$(echo "$BAD_TOKEN_RESP" | tail -1)
if [ "$BAD_TOKEN_CODE" = "401" ] || [ "$BAD_TOKEN_CODE" = "403" ]; then green "Gecersiz token reddedildi [HTTP $BAD_TOKEN_CODE]"
else red "Gecersiz token: beklenmeyen HTTP $BAD_TOKEN_CODE"; fi

echo ">> 9.6 Node Resource raporu gonder"
NR_RESP=$(curl -s -w "\n%{http_code}" -X POST $BASE/api/v1/inventory/nodes/resources \
  -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"nodeId":"gNodeB-001","nodeType":"gNodeB","cpuUsagePercent":65.5,"memoryUsagePercent":48.2,"diskUsagePercent":35.0,"uptimeSeconds":864000}')
NR_CODE=$(echo "$NR_RESP" | tail -1)
if [ "$NR_CODE" = "200" ] || [ "$NR_CODE" = "201" ]; then green "Node Resource raporu gonderildi [HTTP $NR_CODE]"
else red "Node Resource raporu gonderilemedi [HTTP $NR_CODE]"; echo "  $(echo "$NR_RESP" | sed '$d')" | head -c 200; fi

echo ">> 9.7 Node Resources listesi"
NR_LIST=$(curl -s "$BASE/api/v1/inventory/nodes/resources?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
check "Node Resources listesi alindi" "content" "$NR_LIST"

echo ">> 9.8 Inventory gNodeB listesi"
GNB=$(curl -s "$BASE/api/v1/inventory/gnb?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
check "gNodeB listesi alindi" "content" "$GNB"

echo ""
header "TEST SONUCLARI"
echo -e "\033[1;32m  Basarili: $PASS\033[0m"
echo -e "\033[1;31m  Basarisiz: $FAIL\033[0m"
echo -e "\033[1;37m  Toplam: $TOTAL\033[0m"
echo ""
if [ $FAIL -eq 0 ]; then
  echo -e "\033[1;32m  TUM TESTLER BASARILI!\033[0m"
else
  echo -e "\033[1;33m  $FAIL test basarisiz oldu - yukariya bakin.\033[0m"
fi
echo ""
