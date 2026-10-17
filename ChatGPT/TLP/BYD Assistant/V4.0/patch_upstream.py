#!/usr/bin/env python3
"""Patch a PINNED fm1223/byd-android checkout into an experimental Xiaomi/Bemfa AC build.

DO NOT RELEASE before checking the upstream commit, Bemfa real-world payloads,
original APK signature and target vehicle OPENAIR support.
"""
from pathlib import Path
import shutil
import sys

root = Path(sys.argv[1]).resolve()
overlay = Path(sys.argv[2]).resolve()
app = root / "app"

def edit(path, old, new, expected=1):
    p = app / path
    s = p.read_text(encoding="utf-8")
    if s.count(old) != expected:
        raise RuntimeError(f"Unexpected upstream change at {path}: expected {expected}, got {s.count(old)}")
    p.write_text(s.replace(old, new, expected), encoding="utf-8")

base = "src/main/java/dev/vehiclebridge/byd/"
edit("build.gradle.kts", 'versionCode = 19; versionName = "0.4.12-dev"',
     'versionCode = 40; versionName = "4.0.0-dev"')
# Keep the original applicationId: dev.vehiclebridge.byd.
# IMPORTANT: this does NOT make the APK upgradable without the original signer.

edit(base + "protocol/Vehicle.kt",
     'SHUTDOWN("CLOSEAIR") }',
     'SHUTDOWN("CLOSEAIR"), CLIMATE_26_30("OPENAIR") }')
edit(base + "protocol/ControlReceipt.kt",
     'DoorAction.SHUTDOWN -> "熄火"',
     'DoorAction.SHUTDOWN -> "关闭空调";DoorAction.CLIMATE_26_30 -> "开启空调26℃运行30分钟"')
edit(base + "data/BydApi.kt",
     'DoorAction.SHUTDOWN -> "熄火"',
     'DoorAction.SHUTDOWN -> "关闭空调";DoorAction.CLIMATE_26_30 -> "开启空调26℃运行30分钟"')

# Verified pyBYD OPENAIR fields. Duration=5 means 30 minutes, 26C -> raw 12.
edit(base + "protocol/CnProtocol.kt",
     '"commandPwd" to CnCrypto.md5Upper(pin),"commandType" to action.command,"controlParamsMap" to "{}",',
     '"commandPwd" to CnCrypto.md5Upper(pin),"commandType" to action.command,"controlParamsMap" to '
     '(if(action==DoorAction.CLIMATE_26_30) dev.vehiclebridge.byd.bridge.ClimateControlParams.fixed26C30Minutes() else "{}"),')

edit(base + "data/SessionRepository.kt",
     'val combined=action==DoorAction.LOCK && vehicle.vin in saved.shutdownOnLock',
     'val combined=action==DoorAction.LOCK && vehicle.vin in saved.shutdownOnLock')
# For Bemfa's "off", force the original LOCKDOOR then CLOSEAIR sequence,
# independently from the manual-control user preference.
edit(base + "data/SessionRepository.kt",
     'fun sendBoundDoor(bindingId: String, action: DoorAction, progress: (String)->Unit = {}): SessionInfo {',
     'fun sendBoundDoor(bindingId: String, action: DoorAction, progress: (String)->Unit = {}): SessionInfo {')
# Add an explicit linked-only dispatch argument at sendCommands site (first call is sendDoor).
edit(base + "data/SessionRepository.kt",
     'private fun sendCommands(saved: SavedAccount,vehicle: Vehicle,pin: String,action: DoorAction,progress: (String)->Unit): SessionInfo {',
     'private fun sendCommands(saved: SavedAccount,vehicle: Vehicle,pin: String,action: DoorAction,progress: (String)->Unit,forcedShutdown: Boolean = false): SessionInfo {')
edit(base + "data/SessionRepository.kt",
     'val combined=action==DoorAction.LOCK && vehicle.vin in saved.shutdownOnLock',
     'val combined=action==DoorAction.LOCK && (forcedShutdown || vehicle.vin in saved.shutdownOnLock)')
edit(base + "data/SessionRepository.kt",
     'if(combined && first.outcome!=RemoteOutcome.FAILURE)',
     'if(combined && first.outcome==RemoteOutcome.SUCCESS)')
# Target only the sendBoundDoor block's final call.
repo = app / base / "data/SessionRepository.kt"
s = repo.read_text(encoding="utf-8")
start = s.index("    @Synchronized fun sendBoundDoor(")
before, after = s[:start], s[start:]
assert after.count("return sendCommands(saved,vehicle,pin,action,progress)") == 1
after = after.replace("return sendCommands(saved,vehicle,pin,action,progress)",
                      "return sendCommands(saved,vehicle,pin,action,progress,forcedShutdown=(action==DoorAction.LOCK))", 1)
repo.write_text(before + after, encoding="utf-8")

# For the V4 prototype, replace the old 2/3-byte-only gate with the AC parser.
# These classes all remain in the original single-topic Bemfa background service.
for filename in ["XiaodiCommandParser.kt", "ClimateControlParams.kt"]:
    shutil.copyfile(overlay / filename, app / base / "bridge" / filename)
gate = app / base / "bridge/CommandGate.kt"
s = gate.read_text(encoding="utf-8")
s = s.replace("payload.size !in 2..3", "payload.size !in 2..64")
s = s.replace('val action=when(text) {"on" -> DoorAction.UNLOCK;"off" -> DoorAction.LOCK;else -> return null}',
'''val action=when(XiaodiCommandParser.parse(text)) {
            XiaodiCommand.PowerOnRequested -> DoorAction.UNLOCK
            XiaodiCommand.PowerOffRequested -> DoorAction.LOCK
            is XiaodiCommand.AirConditionerSettings -> DoorAction.CLIMATE_26_30
            null -> return null
        }''')
assert "XiaodiCommandParser.parse(text)" in s
gate.write_text(s, encoding="utf-8")
# This is intentionally NOT a verified system-level remote start/stop.
print("V4 development patch applied. NEVER treat CLOSEAIR as engine shutdown.")
