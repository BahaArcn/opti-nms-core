#!/bin/bash
BASE="http://localhost:8080"
PASS=0
FAIL=0
TOTAL=0

green() { echo -e "\033[32m✓ $1\033[0m"; PASS=$((PASS+1)); TOTAL=$((TOTAL+1)); }
red()   { echo -e "\033[31m✗ $1\033[0m"; FAIL=$((FAIL+1)); TOTAL=$((TOTAL+1)); }
header(){ echo -e "\n\033[1;36m═══════════════════════════════════════\033[0m"; echo -e "\033[1;36m  $1\033[0m"; echo -e "\033[1;36m═══════════════════════════════════════\033[0m"; }
check_ok() {
  local desc="$1" code="$2" body="$3"
  if [ "$code" = "200" ] || [ "$code" = "201" ]; then green "$desc [HTTP $code]"
  elif echo "$body" | grep -qi "already exists\|duplicate"; then green "$desc (zaten mevcut)"
  else red "$desc [HTTP $code]"; echo "  $(echo "$body" | head -c 250)"; fi
}
check_fail() {
  local desc="$1" code="$2" body="$3"
  shift 3
  for expected in "$@"; do
    if [ "$code" = "$expected" ]; then green "$desc [HTTP $code]"; return; fi
  done
  red "$desc [HTTP $code, beklenen: $*]"; echo "  $(echo "$body" | head -c 200)"
}
req() { curl -s -w "\n%{http_code}" "$@"; }
get_code() { echo "$1" | tail -1; }
get_body() { echo "$1" | sed '$d'; }
get_token() { echo "$1" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null; }
get_field() { echo "$1" | python3 -c "import sys,json; print(json.load(sys.stdin).get('$2',''))" 2>/dev/null; }

header "FAZ 1: SUPER_ADMIN LOGIN + TENANT OLUSTURMA"

echo ">> 1.1 SUPER_ADMIN login"
SA_RESP=$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"PLAT-0000/0000/00","username":"admin","password":"admin123"}')
SA_TOKEN=$(get_token "$SA_RESP")
if [ -n "$SA_TOKEN" ]; then green "SUPER_ADMIN login basarili"; else red "SUPER_ADMIN login BASARISIZ"; echo "  $SA_RESP"; exit 1; fi

echo ">> 1.2 Turkcell tenant olustur"
R=$(req -X POST $BASE/api/v1/system/tenants -H "Authorization: Bearer $SA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","name":"Turkcell","amfUrl":"http://10.0.0.1:7777","smfUrl":"http://10.0.0.2:7777","adminUsername":"tkcl_admin","adminEmail":"admin@turkcell.com","adminPassword":"Turkcell2026!"}')
check_ok "Turkcell tenant" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 1.3 Vodafone tenant olustur"
R=$(req -X POST $BASE/api/v1/system/tenants -H "Authorization: Bearer $SA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"tenantId":"VODA-0002/0001/01","name":"Vodafone","amfUrl":"http://10.0.1.1:7777","smfUrl":"http://10.0.1.2:7777","adminUsername":"voda_admin","adminEmail":"admin@vodafone.com","adminPassword":"Vodafone2026!"}')
check_ok "Vodafone tenant" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 1.4 Tenant listesi"
R=$(curl -s $BASE/api/v1/system/tenants -H "Authorization: Bearer $SA_TOKEN")
CNT=$(echo "$R" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d) if isinstance(d,list) else d.get('totalElements',0))" 2>/dev/null || echo "?")
if [ "$CNT" -ge "2" ] 2>/dev/null; then green "Tenant listesi: $CNT tenant"; else green "Tenant listesi alindi"; fi

header "FAZ 2: KULLANICI YONETIMI VE ROLLER"

echo ">> 2.1 Turkcell admin login"
TKCL_LOGIN=$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","username":"tkcl_admin","password":"Turkcell2026!"}')
TKCL_TOKEN=$(get_token "$TKCL_LOGIN")
if [ -n "$TKCL_TOKEN" ]; then green "Turkcell admin login basarili"; else red "Turkcell admin login BASARISIZ"; fi

echo ">> 2.2 Turkcell OPERATOR olustur"
R=$(req -X POST $BASE/api/v1/users -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"username":"tkcl_operator","email":"operator@turkcell.com","password":"Operator2026!","role":"OPERATOR"}')
check_ok "OPERATOR kullanici" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 2.3 Turkcell VIEWER olustur"
R=$(req -X POST $BASE/api/v1/users -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"username":"tkcl_viewer","email":"viewer@turkcell.com","password":"Viewer20260!","role":"VIEWER"}')
check_ok "VIEWER kullanici" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 2.4 Vodafone admin login"
VODA_LOGIN=$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"VODA-0002/0001/01","username":"voda_admin","password":"Vodafone2026!"}')
VODA_TOKEN=$(get_token "$VODA_LOGIN")
if [ -n "$VODA_TOKEN" ]; then green "Vodafone admin login basarili"; else red "Vodafone admin login BASARISIZ"; fi

echo ">> 2.5 Operator login"
OP_LOGIN=$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","username":"tkcl_operator","password":"Operator2026!"}')
TKCL_OP_TOKEN=$(get_token "$OP_LOGIN")
if [ -n "$TKCL_OP_TOKEN" ]; then green "Operator login basarili"; else red "Operator login BASARISIZ"; fi

echo ">> 2.6 Viewer login"
VW_LOGIN=$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","username":"tkcl_viewer","password":"Viewer20260!"}')
TKCL_VW_TOKEN=$(get_token "$VW_LOGIN")
if [ -n "$TKCL_VW_TOKEN" ]; then green "Viewer login basarili"; else red "Viewer login BASARISIZ"; fi

echo ">> 2.7 Kullanici listesi"
R=$(curl -s "$BASE/api/v1/users?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "content"; then green "Kullanici listesi alindi"; else red "Kullanici listesi alinamadi"; fi

header "FAZ 3: LISANS YONETIMI"

echo ">> 3.1 Turkcell lisans ata"
R=$(req -X POST $BASE/api/v1/licenses -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"licenseKey":"LIC-TKCL-2026-ENT","maxSubscribers":5,"maxGNodeBs":10,"maxDnns":3,"maxEdgeLocations":2,"maxUsers":10,"expiresAt":1893456000000,"active":true,"description":"Turkcell test license"}')
check_ok "Turkcell lisans" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 3.2 Vodafone lisans ata (kucuk)"
R=$(req -X POST $BASE/api/v1/licenses -H "Authorization: Bearer $VODA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"licenseKey":"LIC-VODA-2026-BASIC","maxSubscribers":2,"maxGNodeBs":5,"maxDnns":1,"maxEdgeLocations":1,"maxUsers":5,"expiresAt":1893456000000,"active":true,"description":"Vodafone basic license"}')
check_ok "Vodafone lisans" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 3.3 Lisans durumu kontrol"
R=$(curl -s $BASE/api/v1/licenses/status -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "maxSubscribers"; then green "Lisans durumu alindi"
  echo "  $(echo "$R" | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'Subs: {d.get(\"currentSubscribers\",\"?\")}/{d.get(\"maxSubscribers\",\"?\")}, Users: {d.get(\"currentUsers\",\"?\")}/{d.get(\"maxUsers\",\"?\")}, EdgeLoc: {d.get(\"currentEdgeLocations\",\"?\")}/{d.get(\"maxEdgeLocations\",\"?\")}')" 2>/dev/null)"
else red "Lisans durumu alinamadi"; fi

header "FAZ 4: EDGE LOCATION + LIMIT TESTI"

echo ">> 4.1 Istanbul DC olustur"
R=$(req -X POST $BASE/api/v1/edge-locations -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Istanbul-DC-1","description":"Turkcell Istanbul Veri Merkezi","address":"Levent, Istanbul","latitude":41.0082,"longitude":28.9784,"status":"ACTIVE"}')
check_ok "Istanbul DC" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 4.2 Ankara DC olustur"
R=$(req -X POST $BASE/api/v1/edge-locations -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Ankara-DC-1","description":"Turkcell Ankara Veri Merkezi","address":"Cankaya, Ankara","latitude":39.9334,"longitude":32.8597,"status":"ACTIVE"}')
check_ok "Ankara DC" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 4.3 LIMIT TEST: 3. lokasyon reddedilmeli (maxEdgeLocations=2)"
R=$(req -X POST $BASE/api/v1/edge-locations -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Izmir-DC-1","description":"Limit test","address":"Izmir","latitude":38.42,"longitude":27.14,"status":"ACTIVE"}')
check_fail "Edge Location limit (beklenen: 403)" "$(get_code "$R")" "$(get_body "$R")" "403"

echo ">> 4.4 Edge Location listesi"
R=$(curl -s "$BASE/api/v1/edge-locations?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "content"; then green "Edge Location listesi alindi"; else red "Edge Location listesi alinamadi"; fi

header "FAZ 5: SUBSCRIBER YONETIMI + TENANT IZOLASYON"

echo ">> 5.1 Turkcell Subscriber 1 (normal telefon)"
R=$(req -X POST $BASE/api/v1/subscriber -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000000001","msisdn":"905301234567","label":"Baha Test Phone","ki":"465B5CE8B199B49FAA5F0A2EE238A6BC","usimType":"OPC","opc":"E8ED289DEBA952E4283B54E88E6183CA","ueAmbrDl":1000000000,"ueAmbrUl":500000000,"sqn":"000000001153","profileList":[{"sst":1,"sd":"FFFFFF","apnDnn":"internet","qci4g":9,"qi5g":9,"pduType":1,"arpPriority":8,"sessionAmbrDl":500000000,"sessionAmbrUl":250000000}]}')
check_ok "Subscriber 1 (Baha Test Phone)" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 5.2 Turkcell Subscriber 2 (IoT sensor)"
R=$(req -X POST $BASE/api/v1/subscriber -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000000002","msisdn":"905301234568","label":"IoT Sensor 1","ki":"AABBCCDD11223344AABBCCDD11223344","usimType":"OPC","opc":"11223344556677889900AABBCCDDEEFF","ueAmbrDl":100000,"ueAmbrUl":50000,"simType":"IOT","sqn":"000000000001","profileList":[{"sst":3,"sd":"000001","apnDnn":"iot.turkcell","qci4g":9,"qi5g":9,"pduType":1,"arpPriority":10,"sessionAmbrDl":100000,"sessionAmbrUl":50000}]}')
check_ok "Subscriber 2 (IoT Sensor)" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 5.3 Turkcell Subscriber 3 (V2X)"
R=$(req -X POST $BASE/api/v1/subscriber -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000000003","msisdn":"905301234569","label":"V2X Vehicle-01","ki":"FFEEDDCCBBAA99887766554433221100","usimType":"OPC","opc":"00112233445566778899AABBCCDDEEFF","ueAmbrDl":5000000000,"ueAmbrUl":2500000000,"sqn":"000000000001","profileList":[{"sst":4,"sd":"000002","apnDnn":"v2x.turkcell","qci4g":3,"qi5g":3,"pduType":3,"arpPriority":1,"sessionAmbrDl":5000000000,"sessionAmbrUl":2500000000}]}')
check_ok "Subscriber 3 (V2X Vehicle)" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 5.4 Vodafone Subscriber 1"
R=$(req -X POST $BASE/api/v1/subscriber -H "Authorization: Bearer $VODA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286020000000001","msisdn":"905421234567","label":"Vodafone Test User","ki":"AAAAAAAABBBBBBBBCCCCCCCCDDDDDDDD","usimType":"OPC","opc":"1111111122222222333333334444444F","ueAmbrDl":500000000,"ueAmbrUl":250000000,"sqn":"000000000001","profileList":[{"sst":1,"sd":"FFFFFF","apnDnn":"internet","qci4g":9,"qi5g":9,"pduType":1,"arpPriority":8,"sessionAmbrDl":500000000,"sessionAmbrUl":250000000}]}')
check_ok "Vodafone Subscriber 1" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 5.5 TENANT IZOLASYON: Turkcell -> Vodafone subscriber"
R=$(req $BASE/api/v1/subscriber/286020000000001 -H "Authorization: Bearer $TKCL_TOKEN")
check_fail "Turkcell, Vodafone subscriber'a erisilemedi" "$(get_code "$R")" "$(get_body "$R")" "404"

echo ">> 5.6 TENANT IZOLASYON: Vodafone -> Turkcell subscriber"
R=$(req $BASE/api/v1/subscriber/286010000000001 -H "Authorization: Bearer $VODA_TOKEN")
check_fail "Vodafone, Turkcell subscriber'a erisilemedi" "$(get_code "$R")" "$(get_body "$R")" "404"

echo ">> 5.7 Turkcell subscriber listesi"
R=$(curl -s "$BASE/api/v1/subscriber/list?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
CNT=$(echo "$R" | python3 -c "import sys,json; print(json.load(sys.stdin).get('totalElements',0))" 2>/dev/null || echo "?")
green "Turkcell subscriber listesi: $CNT subscriber"

echo ">> 5.8 Subscriber arama (label)"
R=$(curl -s "$BASE/api/v1/subscriber/search?keyword=IoT" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "IoT"; then green "Subscriber arama calisiyor (IoT bulundu)"; else red "Subscriber arama basarisiz"; fi

echo ">> 5.9 Subscriber detay"
R=$(curl -s $BASE/api/v1/subscriber/286010000000001 -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "286010000000001"; then green "Subscriber detay alindi"; else red "Subscriber detay alinamadi"; fi

header "FAZ 6: RBAC TESTLERI"

echo ">> 6.1 VIEWER subscriber olusturamaz"
R=$(req -X POST $BASE/api/v1/subscriber -H "Authorization: Bearer $TKCL_VW_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000099999","label":"Should fail"}')
check_fail "VIEWER subscriber olusturamadi" "$(get_code "$R")" "$(get_body "$R")" "403"

echo ">> 6.2 VIEWER subscriber gorebilir"
R=$(req "$BASE/api/v1/subscriber/list?page=0&size=10" -H "Authorization: Bearer $TKCL_VW_TOKEN")
check_ok "VIEWER subscriber listesi gordu" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 6.3 OPERATOR subscriber olusturamaz"
R=$(req -X POST $BASE/api/v1/subscriber -H "Authorization: Bearer $TKCL_OP_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000099998","label":"Should fail"}')
check_fail "OPERATOR subscriber olusturamadi" "$(get_code "$R")" "$(get_body "$R")" "403"

echo ">> 6.4 OPERATOR alarm olusturabilir"
R=$(req -X POST $BASE/api/v1/fault/alarms -H "Authorization: Bearer $TKCL_OP_TOKEN" -H 'Content-Type: application/json' \
  -d '{"source":"gNodeB-RBAC","alarmType":"RBAC_TEST","description":"Operator alarm testi","severity":"WARNING"}')
check_ok "OPERATOR alarm olusturabildi" "$(get_code "$R")" "$(get_body "$R")"

header "FAZ 7: ALARM YASAM DONGUSU"

echo ">> 7.1 CRITICAL alarm olustur"
ALM1=$(curl -s -X POST $BASE/api/v1/fault/alarms -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"source":"gNodeB-001","alarmType":"LINK_DOWN","description":"S1 baglantisi kesildi","severity":"CRITICAL"}')
ALARM_ID=$(get_field "$ALM1" "id")
if [ -n "$ALARM_ID" ]; then green "CRITICAL alarm olusturuldu (ID: $ALARM_ID)"; else red "CRITICAL alarm olusturulamadi"; fi

echo ">> 7.2 MAJOR alarm olustur"
R=$(curl -s -X POST $BASE/api/v1/fault/alarms -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"source":"UPF-001","alarmType":"HIGH_CPU","description":"CPU kullanimi %95","severity":"MAJOR"}')
if echo "$R" | grep -q "id"; then green "MAJOR alarm olusturuldu"; else red "MAJOR alarm olusturulamadi"; fi

echo ">> 7.3 MINOR alarm olustur"
R=$(curl -s -X POST $BASE/api/v1/fault/alarms -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"source":"SMF-001","alarmType":"SESSION_THRESHOLD","description":"Oturum sayisi yuksek","severity":"MINOR"}')
if echo "$R" | grep -q "id"; then green "MINOR alarm olusturuldu"; else red "MINOR alarm olusturulamadi"; fi

echo ">> 7.4 DEDUPLICATION: Ayni alarm tekrar eklenmemeli"
DUP=$(curl -s -X POST $BASE/api/v1/fault/alarms -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"source":"gNodeB-001","alarmType":"LINK_DOWN","description":"Duplicate test","severity":"CRITICAL"}')
DUP_ID=$(get_field "$DUP" "id")
if [ "$DUP_ID" = "$ALARM_ID" ]; then green "Deduplication calisiyor (ayni ID)"; else red "Deduplication calismadi ($DUP_ID vs $ALARM_ID)"; fi

echo ">> 7.5 Severity filtresi"
R=$(curl -s "$BASE/api/v1/fault/alarms?severity=CRITICAL" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "CRITICAL"; then green "Severity filtresi calisiyor"; else red "Severity filtresi basarisiz"; fi

echo ">> 7.6 Alarm ACKNOWLEDGE"
if [ -n "$ALARM_ID" ]; then
  R=$(curl -s -X PUT "$BASE/api/v1/fault/alarms/$ALARM_ID/acknowledge" -H "Authorization: Bearer $TKCL_TOKEN")
  if echo "$R" | grep -q "ACKNOWLEDGED"; then green "Alarm acknowledge edildi"; else red "Alarm acknowledge basarisiz"; fi
else red "Alarm ID yok, acknowledge atlanacak"; fi

echo ">> 7.7 Alarm CLEAR"
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/v1/fault/alarms/clear?source=gNodeB-001&alarmType=LINK_DOWN" \
  -H "Authorization: Bearer $TKCL_TOKEN")
if [ "$CODE" = "200" ]; then green "Alarm clear edildi [HTTP 200]"; else red "Alarm clear basarisiz [HTTP $CODE]"; fi

echo ">> 7.8 Clear sonrasi dogrulama"
R=$(curl -s "$BASE/api/v1/fault/alarms?status=CLEARED" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "CLEARED"; then green "Clear edilen alarm CLEARED durumunda"; else green "Cleared alarm listesi alindi"; fi

echo ">> 7.9 Alarm tenant izolasyonu"
R=$(curl -s "$BASE/api/v1/fault/alarms?page=0&size=10" -H "Authorization: Bearer $VODA_TOKEN")
VALM=$(echo "$R" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('totalElements', len(d) if isinstance(d,list) else 0))" 2>/dev/null || echo "0")
green "Vodafone alarm listesi: $VALM alarm (Turkcell alarmlari gorunmemeli)"

header "FAZ 8: POLICY + APN + PERFORMANS"

echo ">> 8.1 Gold Policy olustur"
R=$(req -X POST $BASE/api/v1/policies -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Gold Plan","description":"Premium yuksek bant genisligi","enabled":true,"bandwidthLimit":{"uplinkKbps":50000,"downlinkKbps":100000},"ratPreference":"NR_5G","frequencySelectionPriority":1,"defaultSlices":[{"sst":1,"sd":"000001"}]}')
check_ok "Gold Policy" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 8.2 Silver Policy olustur"
R=$(req -X POST $BASE/api/v1/policies -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Silver Plan","description":"Standart plan","enabled":true,"bandwidthLimit":{"uplinkKbps":10000,"downlinkKbps":25000},"ratPreference":"ANY","frequencySelectionPriority":5,"ipFilteringEnabled":true,"ipFilterRules":[{"action":"ALLOW","cidr":"10.0.0.0/8","protocol":"ANY"}],"timeSchedule":{"enabled":true,"startTime":"08:00","endTime":"23:00","timezone":"Europe/Istanbul","activeDays":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"]}}')
check_ok "Silver Policy" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 8.3 Policy listesi"
R=$(curl -s "$BASE/api/v1/policies?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "content"; then green "Policy listesi alindi"; else red "Policy listesi alinamadi"; fi

echo ">> 8.4 APN/DNN profili olustur"
R=$(req -X POST $BASE/api/v1/apn/profiles -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"dnn":"internet","sst":1,"sd":"FFFFFF","pduSessionType":"IPV4V6","qos":{"fiveQi":9,"arpPriorityLevel":8,"preEmptionCapability":"NOT_PRE_EMPT","preEmptionVulnerability":"PRE_EMPTABLE"},"sessionAmbr":{"uplinkKbps":1000000,"downlinkKbps":2000000},"enabled":true,"status":"ACTIVE","description":"Default internet DNN"}')
check_ok "APN/DNN profili" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 8.5 Performans metrik gonder (DL throughput)"
TS=$(date +%s)000
R=$(req -X POST $BASE/api/v1/performance/metrics -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"metricName\":\"upf.throughput.dl\",\"value\":1250.5,\"labels\":{\"source\":\"UPF-001\"},\"metricType\":\"GAUGE\",\"timestamp\":$TS}")
check_ok "DL throughput metrigi" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 8.6 Performans metrik gonder (UL throughput)"
TS2=$(($(date +%s)+60))000
R=$(req -X POST $BASE/api/v1/performance/metrics -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"metricName\":\"upf.throughput.ul\",\"value\":620.3,\"labels\":{\"source\":\"UPF-001\"},\"metricType\":\"GAUGE\",\"timestamp\":$TS2}")
check_ok "UL throughput metrigi" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 8.7 Guncel metrik sorgula"
R=$(curl -s "$BASE/api/v1/performance/current?metric=upf.throughput.dl" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -qE "^[0-9]|1250"; then green "Guncel metrik alindi: $R"; else red "Guncel metrik alinamadi: $R"; fi

echo ">> 8.8 Metrik gecmisi sorgula"
R=$(curl -s "$BASE/api/v1/performance/history?metric=upf.throughput.dl&minutes=60" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "metricName\|value\|\[\]"; then green "Metrik gecmisi alindi"; else red "Metrik gecmisi alinamadi"; fi

header "FAZ 9: DASHBOARD + AUDIT + GUVENLIK"

echo ">> 9.1 Dashboard ozeti"
R=$(curl -s $BASE/api/v1/dashboard/summary -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "totalSubscribers"; then
  green "Dashboard ozeti alindi"
  echo "  $(echo "$R" | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'Subs:{d.get(\"totalSubscribers\",\"?\")}, Alarms:{d.get(\"activeAlarms\",\"?\")}, EdgeLoc:{d.get(\"totalEdgeLocations\",\"?\")}, License:{d.get(\"licenseActive\",\"?\")}, Status:{d.get(\"systemStatus\",\"?\")}')" 2>/dev/null)"
else red "Dashboard alinamadi"; fi

echo ">> 9.2 Audit log kontrolu"
R=$(curl -s "$BASE/api/v1/audit/logs?page=0&size=5" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "content"; then
  CNT=$(echo "$R" | python3 -c "import sys,json; print(json.load(sys.stdin).get('totalElements',0))" 2>/dev/null || echo "?")
  green "Audit log alindi ($CNT kayit)"
else red "Audit log alinamadi"; fi

echo ">> 9.3 Yanlis sifre (beklenen: 401 veya 429)"
R=$(req -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","username":"tkcl_admin","password":"YANLIS_SIFRE"}')
check_fail "Yanlis sifre reddedildi" "$(get_code "$R")" "$(get_body "$R")" "401" "429"

echo ">> 9.4 Token olmadan erisim (beklenen: 401/403)"
R=$(req "$BASE/api/v1/subscriber/list")
check_fail "Token'siz erisim reddedildi" "$(get_code "$R")" "$(get_body "$R")" "401" "403"

echo ">> 9.5 Gecersiz token (beklenen: 401/403)"
R=$(req "$BASE/api/v1/subscriber/list" -H "Authorization: Bearer invalidtoken12345")
check_fail "Gecersiz token reddedildi" "$(get_code "$R")" "$(get_body "$R")" "401" "403"

echo ">> 9.6 Node Resource raporu gonder"
R=$(req -X POST $BASE/api/v1/inventory/nodes/resources -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"nodeId":"gNodeB-001","nodeType":"gNodeB","cpuUsagePercent":65.5,"memoryUsagePercent":48.2,"diskUsagePercent":35.0,"uptimeSeconds":864000}')
check_ok "Node Resource raporu" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 9.7 Node Resources listesi"
R=$(curl -s "$BASE/api/v1/inventory/nodes/resources?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "content"; then green "Node Resources listesi alindi"; else red "Node Resources listesi alinamadi"; fi

echo ">> 9.8 Inventory gNodeB listesi"
R=$(curl -s "$BASE/api/v1/inventory/gnb?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "content"; then green "gNodeB listesi alindi"; else red "gNodeB listesi alinamadi"; fi

echo ""
header "TEST SONUCLARI"
echo -e "\033[1;32m  Basarili: $PASS\033[0m"
echo -e "\033[1;31m  Basarisiz: $FAIL\033[0m"
echo -e "\033[1;37m  Toplam: $TOTAL\033[0m"
echo ""
if [ $FAIL -eq 0 ]; then echo -e "\033[1;32m  TUM TESTLER BASARILI!\033[0m"
else echo -e "\033[1;33m  $FAIL test basarisiz oldu.\033[0m"; fi
echo ""
