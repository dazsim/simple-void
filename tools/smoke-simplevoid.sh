#!/usr/bin/env bash
set -eo pipefail
REPO=/workspace/mods/simple-void
DIST=/workspace/mods/simplevoid-dist/0.1.0-alpha.1
SMOKE=/workspace/mods/smoke
BASE=$SMOKE/26.1.2-neoforge
RUN=$SMOKE/simplevoid-$(date +%Y%m%d-%H%M%S)
mkdir -p "$RUN"
printf '%s\n' "$RUN" > "$SMOKE/simplevoid-last-run.txt"
export MCV=26.1.2 L=neoforge SM=$REPO
mkdir -p "$REPO/neoforge/runs/clientb/mods"
source "$SMOKE/sm-b-lib.sh"
SV=$RUN/server
CR=$RUN/client
SS=$CR/screenshots
CLOG=$RUN/client-gradle.log
CINIT=$SMOKE/simplevoid-client.gradle
RUNTASK='-Pversion=0.1.0-alpha.1 :neoforge:26.1.2:runClient'
export SIMPLEVOID_CLIENT_DIR=$CR
SERVER_PID= CLIENT_PID= XVFB_PID=
FAIL=0
mkdir -p "$SV/mods" "$CR" "$DIST/screenshots"
ln -s "$BASE/libraries" "$SV/libraries"
cp "$BASE/run.sh" "$BASE/eula.txt" "$SV/"
printf '%s\n' '-Xms512M' '-Xmx2G' > "$SV/user_jvm_args.txt"
cp "$DIST/simplevoid-neoforge-26.1.2-0.1.0-alpha.1.jar" "$SV/mods/"
cp "$REPO/tools/smoke-client.gradle" "$CINIT"

owned_alive() {
    local pid=$1 expected=$2
    [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null && [ "$(readlink "/proc/$pid/cwd")" = "$expected" ]
}
server_down() {
    if owned_alive "$SERVER_PID" "$SV"; then
        rc stop 2>/dev/null || true
        for i in $(seq 1 45); do kill -0 "$SERVER_PID" 2>/dev/null || break; sleep 1; done
        owned_alive "$SERVER_PID" "$SV" && kill "$SERVER_PID" || true
        wait "$SERVER_PID" 2>/dev/null || true
    fi
    SERVER_PID=
}
client_down() {
    for pid in $(pgrep java || true); do
        if [ "$(readlink "/proc/$pid/cwd")" = "$CR" ]; then kill "$pid" 2>/dev/null || true; fi
    done
    if [ -n "$CLIENT_PID" ]; then
        for i in $(seq 1 30); do kill -0 "$CLIENT_PID" 2>/dev/null || break; sleep 1; done
        if kill -0 "$CLIENT_PID" 2>/dev/null; then
            local group=$(ps -o pgid= -p "$CLIENT_PID" | tr -d ' ')
            [ "$group" = "$CLIENT_PID" ] && kill -- "-$CLIENT_PID" 2>/dev/null || true
        fi
        wait "$CLIENT_PID" 2>/dev/null || true
    fi
    CLIENT_PID=
}
cleanup() {
    local result=$?
    trap - EXIT INT TERM
    set +e
    client_down
    server_down
    [ -n "$XVFB_PID" ] && kill "$XVFB_PID" 2>/dev/null
    python3 - "$RUN" <<'AUDIT'
import os, pathlib, sys
root=sys.argv[1]
alive=[]
for process in pathlib.Path('/proc').iterdir():
    if not process.name.isdigit():continue
    try:
        cwd=os.readlink(process/'cwd')
        command=(process/'cmdline').read_bytes().replace(b'\0',b' ').decode(errors='replace')
        if cwd.startswith(root) and 'java' in command:alive.append((process.name,cwd,command[:180]))
    except (OSError,ProcessLookupError):pass
print('CLEANUP remaining Simple Void game JVMs:',alive)
sys.exit(bool(alive))
AUDIT
    [ $? = 0 ] || result=1
    echo "RESULT simplevoid exit=$result run=$RUN"
    exit "$result"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

if ! rg -q '^Holder: Simple Void' /workspace/mods/test-server-lock.txt; then echo 'Lock is not ours'; exit 1; fi
if ss -ltn '( sport = :25671 or sport = :25672 )' | rg -q ':2567[12]'; then echo 'Shared ports are busy'; exit 1; fi

server_up() {
    cat > "$SV/server.properties" <<'PROPERTIES'
enable-rcon=true
rcon.password=smoke
rcon.port=25672
server-port=25671
online-mode=false
white-list=false
level-type=simplevoid\:void
level-name=world
spawn-protection=0
enforce-secure-profile=false
gamemode=creative
generate-structures=false
spawn-monsters=false
pause-when-empty-seconds=-1
broadcast-rcon-to-ops=false
view-distance=12
simulation-distance=4
PROPERTIES
    (cd "$SV"; exec setsid ./run.sh nogui < /dev/null > out.log 2>&1) &
    SERVER_PID=$!
    echo "SERVER pid=$SERVER_PID directory=$SV"
    for i in $(seq 1 180); do
        rg -q 'RCON running' "$SV/out.log" && { echo 'SERVER ready'; return; }
        kill -0 "$SERVER_PID" 2>/dev/null || { echo 'SERVER exited'; return 1; }
        rg -q 'Crash report|Failed to start|Exception initializing level' "$SV/out.log" && { echo 'SERVER failed'; return 1; }
        sleep 1
    done
    echo 'SERVER timeout'; return 1
}
client_up() {
    if ! pgrep -f '^Xvfb :88 ' >/dev/null; then
        setsid Xvfb :88 -screen 0 1920x1200x24 -ac +extension GLX +render -noreset > "$RUN/xvfb.log" 2>&1 < /dev/null &
        XVFB_PID=$!
    fi
    sleep 1
    cat > "$CR/options.txt" <<'OPTIONS'
guiScale:2
onboardAccessibility:false
pauseOnLostFocus:false
tutorialStep:none
renderDistance:12
simulationDistance:5
maxFps:30
enableVsync:false
clouds:off
soundCategory_master:0.0
showSubtitles:false
OPTIONS
    (cd "$REPO"; exec setsid env DISPLAY=:88 SDL_VIDEO_FORCE_EGL=1 ./gradlew -I "$CINIT"         -Porg.gradle.java.installations.paths="$JAVA_HOME" $RUNTASK > "$CLOG" 2>&1 < /dev/null) &
    CLIENT_PID=$!
    echo "CLIENT launcher=$CLIENT_PID directory=$CR"
    for i in $(seq 1 300); do
        rg -q 'Dev joined the game' "$SV/out.log" && { echo 'CLIENT joined'; return; }
        kill -0 "$CLIENT_PID" 2>/dev/null || { echo 'CLIENT launcher exited'; return 1; }
        rg -q 'BUILD FAILED|Crash report saved' "$CLOG" && { echo 'CLIENT failed'; return 1; }
        sleep 1
    done
    echo 'CLIENT timeout'; return 1
}
shot() {
    sleep 5
    python3 - "$1" <<'CAPTURE'
from PIL import ImageGrab
import sys
image=ImageGrab.grab(bbox=(320,240,1600,960), xdisplay=':88')
image.save(sys.argv[1])
print('SCREENSHOT',sys.argv[1],image.size)
CAPTURE
}
look() {
    local angles
    angles=$(python3 - "$@" <<'ANGLES'
import math,sys
x,y,z,a,b,c=map(float,sys.argv[1:]);dx,dy,dz=a-x,b-(y+1.62),c-z
print(f'{math.degrees(math.atan2(-dx,dz)):.2f} {-math.degrees(math.atan2(dy,math.hypot(dx,dz))):.2f}')
ANGLES
)
    rc "tp Dev $1 $2 $3 $angles"
}
expect() {
    local label=$1 command=$2 text=$3 response reply
    response=$(rc "$command")
    printf '%s\n' "$response" | tee -a "$RUN/checks.log"
    reply="${response#* | }"
    if [[ "$reply" == *"$text"* ]]; then
        echo "PASS $label" | tee -a "$RUN/checks.log"
    else
        echo "FAIL $label expected=$text" | tee -a "$RUN/checks.log"
        FAIL=$((FAIL+1))
    fi
}

server_up
rc 'time set 6000'
rc 'weather clear'
rc 'gamerule advance_time false'
rc 'gamerule spawn_mobs false'
rc 'gamerule max_block_modifications 262144'
rc 'forceload add -270 -10 275 20'
rc 'forceload add 80 80 95 95'
rc 'forceload add -112 -112 -97 -97'
client_up
sleep 5
rc 'data get entity Dev Pos' | tee "$RUN/spawn.txt"
expect initial_spawn 'execute if entity @a[name=Dev,x=4.5,y=67,z=2.5,distance=..0.15]' 'Test passed'
expect island_floor 'execute if block 4 66 2 minecraft:grass_block' 'Test passed'
shot "$DIST/screenshots/01-start-spawn.png"
expect void_chunk_east 'fill 80 -64 80 95 319 95 minecraft:air' 'No blocks were filled'
expect void_chunk_west 'fill -112 -64 -112 -97 319 -97 minecraft:air' 'No blocks were filled'
expect distant_origin 'execute if block 260 64 2 minecraft:stone_bricks' 'Test passed'
expect distant_rotated_corner 'execute if block 256 64 6 minecraft:stone_bricks' 'Test passed'
expect deferred_absent 'execute if block -252 64 2 minecraft:air' 'Test passed'
expect unmet_condition 'void place later_example' 'placed 0, already placed 0, conditions not met 1'
expect deferred_still_absent 'execute if block -252 64 2 minecraft:air' 'Test passed'
expect setup_condition_and_duplicates 'void place simplevoid:skyblock' 'placed 0, already placed 2, conditions not met 1'
rc 'gamemode spectator Dev'
look 16 80 -12 4.5 67 4.5
shot "$DIST/screenshots/02-island-elevated.png"
look 270 73 -8 258 64 4
shot "$DIST/screenshots/03-distant-example.png"
rc 'scoreboard objectives add void_example dummy'
rc 'scoreboard players set #world void_example 1'
expect deferred_placement 'void place later_example' 'placed 1, already placed 0, conditions not met 0'
expect deferred_exists 'execute if block -252 64 2 minecraft:stone_bricks' 'Test passed'
look -240 73 -8 -250 64 4
shot "$DIST/screenshots/04-after-void-place.png"
rc 'setblock -252 64 2 minecraft:oak_planks'
expect named_duplicate_skipped 'void place later_example' 'placed 0, already placed 1, conditions not met 0'
expect explicit_named_duplicate 'void place simplevoid:skyblock later_example' 'placed 0, already placed 1, conditions not met 0'
expect named_edit_preserved 'execute if block -252 64 2 minecraft:oak_planks' 'Test passed'
rc 'setblock 4 66 2 minecraft:oak_planks'
expect setup_duplicates_skipped 'void place simplevoid:skyblock' 'placed 0, already placed 3, conditions not met 0'
expect spawn_edit_preserved 'execute if block 4 66 2 minecraft:oak_planks' 'Test passed'
rc 'save-all flush'
printf 'CHECKS failures=%s\n' "$FAIL" | tee -a "$RUN/checks.log"
[ "$FAIL" = 0 ]
