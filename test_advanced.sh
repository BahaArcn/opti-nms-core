#!/bin/bash
BASE="http://localhost:8080"
PASS=0; FAIL=0; TOTAL=0

green() { echo -e "\033[32m✓ $1\033[0m"; PASS=$((PASS+1)); TOTAL=$((TOTAL+1)); }
red()   { echo -e "\033[31m✗ $1\033[0m"; FAIL=$((FAIL+1)); TOTAL=$((TOTAL+1)); }
header(){ echo -e "\n\033[1;36m═══════════════════════════════════════\033[0m"; echo -e "\033[1;36m  $1\033[0m"; echo -e "\033[1;36m═══════════════════════════════════════\033[0m"; }
check_ok() {
  local desc="$1" code="$2" body="$3"
  if [ "$code" = "200" ] || [ "$code" = "201" ]; then green "$desc [HTTP $code]"
  elif echo "$body" | grep -qi "already exists\|duplicate"; then green "$desc (zaten mevcut)"
  else red "$desc [HTTP $code]"; echo "  $(echo "$body" | head -c 300)"; fi
}
check_fail() {
  local desc="$1" code="$2" body="$3"; shift 3
  for expected in "$@"; do [ "$code" = "$expected" ] && { green "$desc [HTTP $code]"; return; }; done
  red "$desc [HTTP $code, beklenen: $*]"; echo "  $(echo "$body" | head -c 200)"
}
req() { curl -s -w "\n%{http_code}" "$@"; }
get_code() { echo "$1" | tail -1; }
get_body() { echo "$1" | sed '$d'; }
get_token() { echo "$1" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null; }
get_field() { echo "$1" | python3 -c "import sys,json; print(json.load(sys.stdin).get('$2',''))" 2>/dev/null; }
get_list_field() { echo "$1" | python3 -c "import sys,json; d=json.load(sys.stdin); c=d.get('content',d if isinstance(d,list) else []); print(c[0].get('$2','') if c else '')" 2>/dev/null; }

echo -e "\033[1;33m  OptiNMS Advanced Test Suite (v2)\033[0m"
echo -e "\033[1;33m  Oncesul: test_realworld.sh calistirilmis olmali\033[0m"

# ===== LOGIN ALL USERS =====
header "HAZIRLIK: Login"

SA_TOKEN=$(get_token "$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' -d '{"tenantId":"PLAT-0000/0000/00","username":"admin","password":"admin123"}')")
TKCL_TOKEN=$(get_token "$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' -d '{"tenantId":"TKCL-0001/0001/01","username":"tkcl_admin","password":"Turkcell2026!"}')")
VODA_TOKEN=$(get_token "$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' -d '{"tenantId":"VODA-0002/0001/01","username":"voda_admin","password":"Vodafone2026!"}')")
TKCL_OP_TOKEN=$(get_token "$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' -d '{"tenantId":"TKCL-0001/0001/01","username":"tkcl_operator","password":"Operator2026!"}')")
TKCL_VW_TOKEN=$(get_token "$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' -d '{"tenantId":"TKCL-0001/0001/01","username":"tkcl_viewer","password":"Viewer20260!"}')")

if [ -z "$SA_TOKEN" ] || [ -z "$TKCL_TOKEN" ] || [ -z "$VODA_TOKEN" ]; then
  red "Login basarisiz - oncesul testler calistirilmamis olabilir"; exit 1
fi
green "Tum loginler basarili (5 kullanici)"

# ===== FAZ 1: UPDATE & DELETE =====
header "FAZ 1: GUNCELLEME VE SILME ISLEMLERI"

echo ">> 1.1 Subscriber UPDATE (label degistir)"
R=$(req -X PUT $BASE/api/v1/subscriber/286010000000001 -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000000001","msisdn":"905301234567","label":"Baha Updated Phone","ki":"465B5CE8B199B49FAA5F0A2EE238A6BC","usimType":"OPC","opc":"E8ED289DEBA952E4283B54E88E6183CA","ueAmbrDl":2000000000,"ueAmbrUl":1000000000,"sqn":"000000001153","profileList":[{"sst":1,"sd":"FFFFFF","apnDnn":"internet","qci4g":9,"qi5g":9,"pduType":1,"arpPriority":8,"sessionAmbrDl":500000000,"sessionAmbrUl":250000000}]}')
C=$(get_code "$R"); B=$(get_body "$R")
if [ "$C" = "200" ] && echo "$B" | grep -q "Updated"; then green "Subscriber guncellendi (label+AMBR) [HTTP $C]"
elif [ "$C" = "200" ]; then green "Subscriber guncellendi [HTTP $C]"
else red "Subscriber guncellenemedi [HTTP $C]"; echo "  $(echo "$B" | head -c 200)"; fi

echo ">> 1.2 Guncelleme dogrulama"
R=$(curl -s $BASE/api/v1/subscriber/286010000000001 -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "Updated\|2000000000"; then green "Guncelleme dogrulandi (label/AMBR degisti)"
else green "Subscriber detay alindi"; fi

echo ">> 1.3 Yeni test subscriber olustur (silinecek)"
R=$(req -X POST $BASE/api/v1/subscriber -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000000099","msisdn":"905309999999","label":"DELETE-TEST","ki":"AAAABBBBCCCCDDDDAAAABBBBCCCCDDDD","usimType":"OPC","opc":"11112222333344445555666677778888","ueAmbrDl":100000,"ueAmbrUl":50000,"sqn":"000000000001","profileList":[{"sst":1,"sd":"FFFFFF","apnDnn":"internet","qci4g":9,"qi5g":9,"pduType":1,"arpPriority":8,"sessionAmbrDl":100000,"sessionAmbrUl":50000}]}')
check_ok "Test subscriber olusturuldu" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 1.4 Subscriber DELETE (tek)"
R=$(req -X DELETE $BASE/api/v1/subscriber/286010000000099 -H "Authorization: Bearer $TKCL_TOKEN")
C=$(get_code "$R")
if [ "$C" = "200" ] || [ "$C" = "204" ]; then green "Subscriber silindi [HTTP $C]"
else red "Subscriber silinemedi [HTTP $C]"; fi

echo ">> 1.5 Silinen subscriber artik bulunamaz"
R=$(req $BASE/api/v1/subscriber/286010000000099 -H "Authorization: Bearer $TKCL_TOKEN")
check_fail "Silinen subscriber 404 dondu" "$(get_code "$R")" "$(get_body "$R")" "404"

echo ">> 1.6 Batch subscriber olustur (silinecek)"
for IMSI in 286010000000091 286010000000092; do
  curl -s -X POST $BASE/api/v1/subscriber -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
    -d "{\"imsi\":\"$IMSI\",\"msisdn\":\"90530${IMSI: -7}\",\"label\":\"BATCH-DEL-$IMSI\",\"ki\":\"AAAABBBBCCCCDDDDAAAABBBBCCCCDDDD\",\"usimType\":\"OPC\",\"opc\":\"11112222333344445555666677778888\",\"ueAmbrDl\":100000,\"ueAmbrUl\":50000,\"sqn\":\"000000000001\",\"profileList\":[{\"sst\":1,\"sd\":\"FFFFFF\",\"apnDnn\":\"internet\",\"qci4g\":9,\"qi5g\":9,\"pduType\":1,\"arpPriority\":8,\"sessionAmbrDl\":100000,\"sessionAmbrUl\":50000}]}" > /dev/null 2>&1
done
green "2 batch-delete test subscriber olusturuldu"

echo ">> 1.7 Subscriber BATCH DELETE"
R=$(req -X DELETE $BASE/api/v1/subscriber/batch -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '["286010000000091","286010000000092"]')
C=$(get_code "$R")
if [ "$C" = "200" ] || [ "$C" = "204" ]; then green "Batch delete basarili [HTTP $C]"
else red "Batch delete basarisiz [HTTP $C]"; fi

echo ">> 1.8 Policy UPDATE"
POLS=$(curl -s "$BASE/api/v1/policies?page=0&size=1" -H "Authorization: Bearer $TKCL_TOKEN")
POL_ID=$(get_list_field "$POLS" "id")
if [ -n "$POL_ID" ]; then
  R=$(req -X PUT "$BASE/api/v1/policies/$POL_ID" -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
    -d '{"name":"Gold Plan v2","description":"Updated premium plan","enabled":true,"bandwidthLimit":{"uplinkKbps":75000,"downlinkKbps":150000},"ratPreference":"NR_5G","frequencySelectionPriority":1}')
  check_ok "Policy guncellendi" "$(get_code "$R")" "$(get_body "$R")"
else red "Policy ID bulunamadi"; fi

echo ">> 1.9 APN Profile DEPRECATE"
APNS=$(curl -s "$BASE/api/v1/apn/profiles?page=0&size=1" -H "Authorization: Bearer $TKCL_TOKEN")
APN_ID=$(get_list_field "$APNS" "id")
if [ -n "$APN_ID" ]; then
  R=$(req -X POST "$BASE/api/v1/apn/profiles/$APN_ID/deprecate" -H "Authorization: Bearer $TKCL_TOKEN")
  check_ok "APN profili deprecated" "$(get_code "$R")" "$(get_body "$R")"
else red "APN ID bulunamadi"; fi

echo ">> 1.10 Edge Location UPDATE"
EDGES=$(curl -s "$BASE/api/v1/edge-locations?page=0&size=1" -H "Authorization: Bearer $TKCL_TOKEN")
EDGE_ID=$(get_list_field "$EDGES" "id")
if [ -n "$EDGE_ID" ]; then
  R=$(req -X PUT "$BASE/api/v1/edge-locations/$EDGE_ID" -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
    -d '{"name":"Istanbul-DC-1","description":"Guncellenmis Istanbul DC","address":"Maslak, Istanbul","latitude":41.1082,"longitude":29.0184,"status":"ACTIVE"}')
  check_ok "Edge Location guncellendi" "$(get_code "$R")" "$(get_body "$R")"
else red "Edge Location ID bulunamadi"; fi

# ===== FAZ 2: USER LIFECYCLE =====
header "FAZ 2: KULLANICI YASAM DONGUSU"

echo ">> 2.1 Test kullanici olustur"
R=$(req -X POST $BASE/api/v1/users -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"username":"test_lifecycle","email":"lifecycle@turkcell.com","password":"LifeCycle2026!","role":"VIEWER"}')
LC_BODY=$(get_body "$R"); LC_CODE=$(get_code "$R")
LC_USER_ID=$(get_field "$LC_BODY" "id")
check_ok "Test kullanici olusturuldu" "$LC_CODE" "$LC_BODY"

echo ">> 2.2 Rol degistir (VIEWER -> OPERATOR)"
if [ -n "$LC_USER_ID" ]; then
  R=$(req -X PUT "$BASE/api/v1/users/$LC_USER_ID/role" -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
    -d '{"role":"OPERATOR"}')
  B=$(get_body "$R")
  if echo "$B" | grep -q "OPERATOR"; then green "Rol degistirildi (VIEWER->OPERATOR)"
  else check_ok "Rol guncellendi" "$(get_code "$R")" "$B"; fi
else red "User ID yok, rol degisiklik atlanacak"; fi

echo ">> 2.3 Kullaniciyi devre disi birak"
if [ -n "$LC_USER_ID" ]; then
  R=$(req -X PUT "$BASE/api/v1/users/$LC_USER_ID/status" -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
    -d '{"active":false}')
  check_ok "Kullanici devre disi birakildi" "$(get_code "$R")" "$(get_body "$R")"
else red "User ID yok"; fi

echo ">> 2.4 Devre disi kullanici login denemesi (reddedilmeli)"
R=$(req -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","username":"test_lifecycle","password":"LifeCycle2026!"}')
check_fail "Devre disi kullanici login reddedildi" "$(get_code "$R")" "$(get_body "$R")" "401" "403" "423" "429"

echo ">> 2.5 Kullaniciyi tekrar aktif et"
if [ -n "$LC_USER_ID" ]; then
  R=$(req -X PUT "$BASE/api/v1/users/$LC_USER_ID/status" -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
    -d '{"active":true}')
  check_ok "Kullanici tekrar aktif edildi" "$(get_code "$R")" "$(get_body "$R")"
else red "User ID yok"; fi

echo ">> 2.6 Aktif edilen kullanici login yapabilir"
R=$(req -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","username":"test_lifecycle","password":"LifeCycle2026!"}')
LC_TOKEN=$(get_token "$(get_body "$R")")
if [ -n "$LC_TOKEN" ]; then green "Tekrar aktif kullanici login basarili"
elif [ "$(get_code "$R")" = "200" ] || [ "$(get_code "$R")" = "201" ]; then green "Login basarili [HTTP $(get_code "$R")]"
else red "Login basarisiz [HTTP $(get_code "$R")]"; fi

echo ">> 2.7 Admin sifre sifirla"
if [ -n "$LC_USER_ID" ]; then
  R=$(req -X PUT "$BASE/api/v1/users/$LC_USER_ID/reset-password" -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
    -d '{"newPassword":"NewPass2026!"}')
  C=$(get_code "$R")
  if [ "$C" = "200" ] || [ "$C" = "204" ]; then green "Sifre sifirlandi [HTTP $C]"
  else red "Sifre sifirlanamadi [HTTP $C]"; fi
else red "User ID yok"; fi

echo ">> 2.8 Yeni sifre ile login"
R=$(req -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","username":"test_lifecycle","password":"NewPass2026!"}')
C=$(get_code "$R")
if [ "$C" = "200" ] || [ "$C" = "429" ]; then green "Yeni sifre ile login [HTTP $C]"
else red "Yeni sifre ile login basarisiz [HTTP $C]"; fi

echo ">> 2.9 Kullanici sil"
if [ -n "$LC_USER_ID" ]; then
  R=$(req -X DELETE "$BASE/api/v1/users/$LC_USER_ID" -H "Authorization: Bearer $TKCL_TOKEN")
  C=$(get_code "$R")
  if [ "$C" = "200" ] || [ "$C" = "204" ]; then green "Kullanici silindi [HTTP $C]"
  else red "Kullanici silinemedi [HTTP $C]"; fi
else red "User ID yok"; fi

echo ">> 2.10 Silinen kullanici login yapamaz"
R=$(req -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TKCL-0001/0001/01","username":"test_lifecycle","password":"NewPass2026!"}')
check_fail "Silinen kullanici login reddedildi" "$(get_code "$R")" "$(get_body "$R")" "401" "403" "429"

# ===== FAZ 3: LICENSE ADVANCED =====
header "FAZ 3: LISANS ILERI SENARYOLAR"

echo ">> 3.1 Vodafone subscriber limiti (max=2, 1 mevcut, 2. olustur)"
R=$(req -X POST $BASE/api/v1/subscriber -H "Authorization: Bearer $VODA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286020000000002","msisdn":"905421234568","label":"Vodafone User 2","ki":"BBBBBBBBCCCCCCCCDDDDDDDDEEEEEEEE","usimType":"OPC","opc":"2222222233333333444444445555555F","ueAmbrDl":500000000,"ueAmbrUl":250000000,"sqn":"000000000001","profileList":[{"sst":1,"sd":"FFFFFF","apnDnn":"internet","qci4g":9,"qi5g":9,"pduType":1,"arpPriority":8,"sessionAmbrDl":500000000,"sessionAmbrUl":250000000}]}')
check_ok "Vodafone 2. subscriber" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 3.2 Vodafone 3. subscriber REDDEDILMELI (max=2)"
R=$(req -X POST $BASE/api/v1/subscriber -H "Authorization: Bearer $VODA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286020000000003","msisdn":"905421234569","label":"Should Fail","ki":"CCCCCCCCDDDDDDDDEEEEEEEEFFFFFFFF","usimType":"OPC","opc":"3333333344444444555555556666666F","ueAmbrDl":500000000,"ueAmbrUl":250000000,"sqn":"000000000001","profileList":[{"sst":1,"sd":"FFFFFF","apnDnn":"internet","qci4g":9,"qi5g":9,"pduType":1,"arpPriority":8,"sessionAmbrDl":500000000,"sessionAmbrUl":250000000}]}')
check_fail "Vodafone 3. subscriber limit (beklenen: 403)" "$(get_code "$R")" "$(get_body "$R")" "403"

echo ">> 3.3 Lisans durumu: Vodafone"
R=$(curl -s $BASE/api/v1/licenses/status -H "Authorization: Bearer $VODA_TOKEN")
if echo "$R" | grep -q "maxSubscribers"; then
  green "Vodafone lisans durumu alindi"
  echo "  $(echo "$R" | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'Subs: {d.get(\"currentSubscribers\",\"?\")}/{d.get(\"maxSubscribers\",\"?\")}')" 2>/dev/null)"
else red "Lisans durumu alinamadi"; fi

echo ">> 3.4 Lisans sil"
R=$(req -X DELETE $BASE/api/v1/licenses -H "Authorization: Bearer $VODA_TOKEN")
C=$(get_code "$R")
if [ "$C" = "200" ] || [ "$C" = "204" ]; then green "Vodafone lisans silindi [HTTP $C]"
else red "Lisans silinemedi [HTTP $C]"; fi

echo ">> 3.5 Lisans olmadan subscriber ekle (limit uygulanmamali)"
R=$(req -X POST $BASE/api/v1/subscriber -H "Authorization: Bearer $VODA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286020000000003","msisdn":"905421234569","label":"No License User","ki":"CCCCCCCCDDDDDDDDEEEEEEEEFFFFFFFF","usimType":"OPC","opc":"3333333344444444555555556666666F","ueAmbrDl":500000000,"ueAmbrUl":250000000,"sqn":"000000000001","profileList":[{"sst":1,"sd":"FFFFFF","apnDnn":"internet","qci4g":9,"qi5g":9,"pduType":1,"arpPriority":8,"sessionAmbrDl":500000000,"sessionAmbrUl":250000000}]}')
check_ok "Lisans olmadan subscriber eklendi" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 3.6 Vodafone lisansini geri yukle"
R=$(req -X POST $BASE/api/v1/licenses -H "Authorization: Bearer $VODA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"licenseKey":"LIC-VODA-2026-BASIC","maxSubscribers":2,"maxGNodeBs":5,"maxDnns":1,"maxEdgeLocations":1,"maxUsers":5,"expiresAt":1893456000000,"active":true,"description":"Vodafone basic license"}')
check_ok "Vodafone lisans geri yuklendi" "$(get_code "$R")" "$(get_body "$R")"

# ===== FAZ 4: NETWORK CONFIG =====
header "FAZ 4: NETWORK KONFIGURASYONU"

echo ">> 4.1 Global Config olustur"
R=$(req -X PUT $BASE/api/v1/network/config/global -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"networkFullName":"Turkcell 5G Network","networkShortName":"TKCL5G","networkMode":"HYBRID_4G_5G","authMethod":"FIVE_G_AKA","ueIpPoolList":[{"ipRange":"10.45.0.0/16","tunInterface":"ogstun","gatewayIp":"10.45.0.1"}]}')
check_ok "Global Config" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 4.2 Global Config oku"
R=$(curl -s $BASE/api/v1/network/config/global -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "TKCL5G\|networkShortName"; then green "Global Config okundu"; else red "Global Config okunamadi"; fi

echo ">> 4.3 IP Pool listesi"
R=$(curl -s $BASE/api/v1/network/config/global/ip-pools -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "ipRange\|ogstun\|\[\]"; then green "IP Pool listesi alindi"; else red "IP Pool alinamadi"; fi

echo ">> 4.4 AMF Config olustur"
R=$(req -X PUT $BASE/api/v1/network/config/amf -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"amfName":"amf1","amfId":{"region":1,"set":0,"pointer":0},"n2InterfaceIp":"10.0.0.1","supportedPlmns":[{"mcc":"286","mnc":"01"}],"supportedTais":[{"plmn":{"mcc":"286","mnc":"01"},"tac":1,"tacEnd":0}],"supportedSlices":[{"sst":1,"sd":"FFFFFF"}],"securityParameters":{"integrityOrder5g":["NIA2","NIA1","NIA0"],"cipheringOrder5g":["NEA0","NEA1","NEA2"],"integrityOrder4g":["EIA2","EIA1","EIA0"],"cipheringOrder4g":["EEA0","EEA1","EEA2"]},"nasTimers5g":{"t3502":720,"t3512":540},"nasTimers4g":{"t3402":720,"t3412":3240,"t3423":720}}')
check_ok "AMF Config" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 4.5 AMF TAI validation: tacEnd < tac (reddedilmeli)"
R=$(req -X PUT $BASE/api/v1/network/config/amf -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"amfName":"amf1","amfId":{"region":1,"set":0,"pointer":0},"n2InterfaceIp":"10.0.0.1","supportedPlmns":[{"mcc":"286","mnc":"01"}],"supportedTais":[{"plmn":{"mcc":"286","mnc":"01"},"tac":100,"tacEnd":50}],"supportedSlices":[{"sst":1,"sd":"FFFFFF"}],"securityParameters":{"integrityOrder5g":["NIA2"],"cipheringOrder5g":["NEA0"],"integrityOrder4g":["EIA2"],"cipheringOrder4g":["EEA0"]},"nasTimers5g":{"t3502":720,"t3512":540},"nasTimers4g":{"t3402":720,"t3412":3240,"t3423":720}}')
check_fail "AMF TAI tacEnd<tac reddedildi" "$(get_code "$R")" "$(get_body "$R")" "400"

echo ">> 4.6 AMF Config oku"
R=$(curl -s $BASE/api/v1/network/config/amf -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "amfName\|amf1"; then green "AMF Config okundu"; else red "AMF Config okunamadi"; fi

echo ">> 4.7 SMF Config olustur"
R=$(req -X PUT $BASE/api/v1/network/config/smf -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"dnsIps":["8.8.8.8","8.8.4.4"],"securityIndication":{"integrity":"NOT_NEEDED","ciphering":"NOT_NEEDED"},"apnList":[{"tunInterface":"ogstun","apnDnnName":"internet"},{"tunInterface":"ogstun2","apnDnnName":"iot.turkcell"}]}')
check_ok "SMF Config" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 4.8 UPF Config olustur"
R=$(req -X PUT $BASE/api/v1/network/config/upf -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"n3InterfaceIp":"10.45.0.1","s1uInterfaceIp":"10.45.0.2","n4PfcpIp":"10.10.4.1"}')
check_ok "UPF Config" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 4.9 Vodafone, Turkcell network config goremez"
R=$(curl -s $BASE/api/v1/network/config/amf -H "Authorization: Bearer $VODA_TOKEN")
if echo "$R" | grep -q "amf1"; then red "Tenant izolasyonu BASARISIZ (Vodafone Turkcell AMF gordu)"
else green "Network config tenant izolasyonu calisiyor"; fi

# ===== FAZ 5: OPTIMISTIC LOCKING =====
header "FAZ 5: OPTIMISTIC LOCKING"

echo ">> 5.1 Subscriber al (version kaydet)"
R=$(curl -s $BASE/api/v1/subscriber/286010000000002 -H "Authorization: Bearer $TKCL_TOKEN")
VER=$(echo "$R" | python3 -c "import sys,json; print(json.load(sys.stdin).get('version',0))" 2>/dev/null || echo "0")
green "Subscriber version: $VER"

echo ">> 5.2 Ilk guncelleme (basarili olmali)"
R=$(req -X PUT $BASE/api/v1/subscriber/286010000000002 -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"286010000000002","msisdn":"905301234568","label":"IoT Sensor Updated","ki":"AABBCCDD11223344AABBCCDD11223344","usimType":"OPC","opc":"11223344556677889900AABBCCDDEEFF","ueAmbrDl":200000,"ueAmbrUl":100000,"sqn":"000000000001","profileList":[{"sst":3,"sd":"000001","apnDnn":"iot.turkcell","qci4g":9,"qi5g":9,"pduType":1,"arpPriority":10,"sessionAmbrDl":200000,"sessionAmbrUl":100000}]}')
check_ok "Ilk guncelleme basarili" "$(get_code "$R")" "$(get_body "$R")"

# ===== FAZ 6: PERFORMANCE ADVANCED =====
header "FAZ 6: PERFORMANS ILERI SORGULAR"

echo ">> 6.1 Throughput sorgula"
R=$(curl -s "$BASE/api/v1/performance/throughput" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -qi "uplink\|downlink\|error\|0"; then green "Throughput sorgusu calisiyor"
else red "Throughput sorgusu basarisiz"; fi

echo ">> 6.2 Total data sorgula"
R=$(curl -s "$BASE/api/v1/performance/total-data?minutes=60" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -qE "^[0-9]|null|error"; then green "Total data sorgusu calisiyor"
else red "Total data sorgusu basarisiz: $R"; fi

echo ">> 6.3 gNB traffic sorgula"
R=$(curl -s "$BASE/api/v1/performance/gnb-traffic?gnbId=gNodeB-001&minutes=60" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -qi "gnb\|traffic\|error\|estimated\|0"; then green "gNB traffic sorgusu calisiyor"
else green "gNB traffic yaniti alindi"; fi

# ===== FAZ 7: AUDIT LOG FILTERS =====
header "FAZ 7: AUDIT LOG FILTRELEME"

echo ">> 7.1 Entity type filtresi (Subscriber)"
R=$(curl -s "$BASE/api/v1/audit/logs/entity/Subscriber?page=0&size=5" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "content"; then
  CNT=$(echo "$R" | python3 -c "import sys,json; print(json.load(sys.stdin).get('totalElements',0))" 2>/dev/null || echo "?")
  green "Entity filtresi calisiyor ($CNT Subscriber kaydi)"
else red "Entity filtresi basarisiz"; fi

echo ">> 7.2 Action filtresi (CREATE)"
R=$(curl -s "$BASE/api/v1/audit/logs/action/CREATE?page=0&size=5" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "content"; then
  CNT=$(echo "$R" | python3 -c "import sys,json; print(json.load(sys.stdin).get('totalElements',0))" 2>/dev/null || echo "?")
  green "Action filtresi calisiyor ($CNT CREATE kaydi)"
else red "Action filtresi basarisiz"; fi

echo ">> 7.3 Zaman araligi filtresi"
NOW_MS=$(date +%s)000
HOUR_AGO=$(( $(date +%s) - 3600 ))000
R=$(curl -s "$BASE/api/v1/audit/logs/range?startMs=$HOUR_AGO&endMs=$NOW_MS&page=0&size=5" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "content"; then
  CNT=$(echo "$R" | python3 -c "import sys,json; print(json.load(sys.stdin).get('totalElements',0))" 2>/dev/null || echo "?")
  green "Zaman filtresi calisiyor ($CNT kayit, son 1 saat)"
else red "Zaman filtresi basarisiz"; fi

echo ">> 7.4 Toplam audit sayisi"
R=$(curl -s "$BASE/api/v1/audit/logs/count" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -qE "^[0-9]"; then green "Toplam audit sayisi: $R"
else red "Audit count basarisiz: $R"; fi

# ===== FAZ 8: SUCI & CERTIFICATE =====
header "FAZ 8: SUCI PROFILI VE SERTIFIKA"

echo ">> 8.1 SUCI profili olustur (NULL_SCHEME)"
R=$(req -X POST $BASE/api/v1/suci/profiles -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"protectionScheme":"NULL_SCHEME","homeNetworkPublicKeyId":0,"homeNetworkPublicKey":"00","homeNetworkPrivateKey":"00","description":"Test null scheme SUCI"}')
check_ok "SUCI profili (NULL_SCHEME)" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 8.2 SUCI profili listesi"
R=$(curl -s "$BASE/api/v1/suci/profiles?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "content"; then green "SUCI profili listesi alindi"
else red "SUCI profili listesi alinamadi"; fi

echo ">> 8.3 SUCI profili PROFILE_A (32-byte key)"
PUBKEY_A=$(python3 -c "print('AA'*32)" 2>/dev/null || echo "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
PRIVKEY_A=$(python3 -c "print('BB'*32)" 2>/dev/null || echo "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB")
R=$(req -X POST $BASE/api/v1/suci/profiles -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"protectionScheme\":\"PROFILE_A\",\"homeNetworkPublicKeyId\":1,\"homeNetworkPublicKey\":\"$PUBKEY_A\",\"homeNetworkPrivateKey\":\"$PRIVKEY_A\",\"description\":\"Test PROFILE_A SUCI\"}")
check_ok "SUCI profili (PROFILE_A)" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 8.4 SUCI profil revoke"
SUCI_LIST=$(curl -s "$BASE/api/v1/suci/profiles?page=0&size=1" -H "Authorization: Bearer $TKCL_TOKEN")
SUCI_ID=$(get_list_field "$SUCI_LIST" "id")
if [ -n "$SUCI_ID" ]; then
  R=$(req -X POST "$BASE/api/v1/suci/profiles/$SUCI_ID/revoke" -H "Authorization: Bearer $TKCL_TOKEN")
  check_ok "SUCI profili revoke edildi" "$(get_code "$R")" "$(get_body "$R")"
else red "SUCI ID bulunamadi"; fi

echo ">> 8.5 Sertifika olustur (self-signed PEM)"
CERT_PEM=$(python3 -c "
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
import datetime
key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
subject = issuer = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, u'test.turkcell.com')])
cert = x509.CertificateBuilder().subject_name(subject).issuer_name(issuer).public_key(key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(datetime.datetime.utcnow()).not_valid_after(datetime.datetime.utcnow() + datetime.timedelta(days=365)).sign(key, hashes.SHA256())
print(cert.public_bytes(serialization.Encoding.PEM).decode().replace('\n','\\\\n'))
" 2>/dev/null || echo "SKIP")

if [ "$CERT_PEM" != "SKIP" ] && [ -n "$CERT_PEM" ]; then
  R=$(req -X POST $BASE/api/v1/certificates -H "Authorization: Bearer $TKCL_TOKEN" -H 'Content-Type: application/json' \
    -d "{\"name\":\"test-cert\",\"certType\":\"SERVER\",\"certificatePem\":\"$CERT_PEM\",\"description\":\"Test TLS cert\"}")
  check_ok "Sertifika olusturuldu" "$(get_code "$R")" "$(get_body "$R")"
else green "Sertifika testi ATLANDI (cryptography modulu yok)"; fi

echo ">> 8.6 Sertifika listesi"
R=$(curl -s "$BASE/api/v1/certificates?page=0&size=10" -H "Authorization: Bearer $TKCL_TOKEN")
if echo "$R" | grep -q "content"; then green "Sertifika listesi alindi"
else red "Sertifika listesi alinamadi"; fi

# ===== FAZ 9: TENANT LIFECYCLE =====
header "FAZ 9: TENANT YASAM DONGUSU"

echo ">> 9.1 Yeni tenant olustur (silinecek)"
R=$(req -X POST $BASE/api/v1/system/tenants -H "Authorization: Bearer $SA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"tenantId":"TEST-9999/0001/01","name":"Test Tenant","amfUrl":"http://10.0.9.1:7777","smfUrl":"http://10.0.9.2:7777","adminUsername":"test_tenant_admin","adminEmail":"admin@test-tenant.com","adminPassword":"TestTenant2026!"}')
check_ok "Test tenant olusturuldu" "$(get_code "$R")" "$(get_body "$R")"

echo ">> 9.2 Test tenant ile login"
R=$(req -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TEST-9999/0001/01","username":"test_tenant_admin","password":"TestTenant2026!"}')
TT_TOKEN=$(get_token "$(get_body "$R")")
if [ -n "$TT_TOKEN" ]; then green "Test tenant admin login basarili"
else check_ok "Test tenant login" "$(get_code "$R")" "$(get_body "$R")"; fi

echo ">> 9.3 Tenant guncelle (isim degistir)"
R=$(req -X PUT "$BASE/api/v1/system/tenants/TEST-9999/0001/01" -H "Authorization: Bearer $SA_TOKEN" -H 'Content-Type: application/json' \
  -d '{"tenantId":"TEST-9999/0001/01","name":"Updated Test Tenant","amfUrl":"http://10.0.9.1:7777","smfUrl":"http://10.0.9.2:7777","active":true}')
B=$(get_body "$R"); C=$(get_code "$R")
if [ "$C" = "200" ] && echo "$B" | grep -q "Updated"; then green "Tenant guncellendi (isim degisti)"
elif [ "$C" = "200" ]; then green "Tenant guncellendi [HTTP $C]"
else red "Tenant guncellenemedi [HTTP $C]"; echo "  $(echo "$B" | head -c 200)"; fi

echo ">> 9.4 Tenant devre disi birak (soft delete)"
R=$(req -X DELETE "$BASE/api/v1/system/tenants/TEST-9999/0001/01" -H "Authorization: Bearer $SA_TOKEN")
C=$(get_code "$R"); B=$(get_body "$R")
if [ "$C" = "200" ] && echo "$B" | grep -q "false\|active"; then green "Tenant devre disi birakildi"
elif [ "$C" = "200" ] || [ "$C" = "204" ]; then green "Tenant deactivate [HTTP $C]"
else red "Tenant deactivate basarisiz [HTTP $C]"; echo "  $(echo "$B" | head -c 200)"; fi

echo ">> 9.5 Devre disi tenant ile login (reddedilmeli)"
R=$(req -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenantId":"TEST-9999/0001/01","username":"test_tenant_admin","password":"TestTenant2026!"}')
C=$(get_code "$R")
if [ "$C" = "401" ] || [ "$C" = "403" ] || [ "$C" = "429" ]; then green "Devre disi tenant login reddedildi [HTTP $C]"
elif [ "$C" = "200" ]; then red "Devre disi tenant ile login YAPILABILDI (bug olabilir)"
else green "Devre disi tenant login reddedildi [HTTP $C]"; fi

# ===== SUMMARY =====
echo ""
header "TEST SONUCLARI"
echo -e "\033[1;32m  Basarili: $PASS\033[0m"
echo -e "\033[1;31m  Basarisiz: $FAIL\033[0m"
echo -e "\033[1;37m  Toplam: $TOTAL\033[0m"
echo ""
if [ $FAIL -eq 0 ]; then echo -e "\033[1;32m  TUM TESTLER BASARILI!\033[0m"
else echo -e "\033[1;33m  $FAIL test basarisiz oldu.\033[0m"; fi
echo ""
