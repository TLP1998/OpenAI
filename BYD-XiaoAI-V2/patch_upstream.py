#!/usr/bin/env python3
"""Apply explicit, bounded changes to pinned fm1223/byd-android source."""
from pathlib import Path
import re
import shutil
import sys

repo = Path(sys.argv[1]).resolve()
overlay = Path(sys.argv[2]).resolve()
app = repo / "app"
def edit(path, old, new, count=1):
    p = app / path
    content = p.read_text(encoding="utf8")
    if content.count(old) != count:
        raise RuntimeError(f"Upstream changed: expected {count} matches of {old[:80]!r} in {p}, got {content.count(old)}")
    p.write_text(content.replace(old,new,count),encoding="utf8")

edit("build.gradle.kts", 'applicationId = "dev.vehiclebridge.byd"', 'applicationId = "dev.vehiclebridge.byd.xiaoai"')
edit("build.gradle.kts", 'versionCode = 19; versionName = "0.4.12-dev"', 'versionCode = 22; versionName = "1.2.0-xiaopoche"')
edit("src/main/res/values/strings.xml", '<string name="app_name">车辆助手</string>', '<string name="app_name">比亚迪车控主程序</string>')

repo_src = app / "src/main/java/dev/vehiclebridge/byd"
shutil.copyfile(overlay / "VoiceCommandActivity.kt", repo_src / "VoiceCommandActivity.kt")

# Voice actions bypass the upstream optional "lock + shutdown" preference.
edit("src/main/java/dev/vehiclebridge/byd/data/SessionRepository.kt",
     "fun sendDoor(vin: String, action: DoorAction, temporaryPin: String? = null, progress: (String)->Unit = {}): SessionInfo {",
     "fun sendDoor(vin: String, action: DoorAction, temporaryPin: String? = null, progress: (String)->Unit = {}, allowShutdownOnLock: Boolean = true, voiceShutdown: Boolean = false): SessionInfo {")
edit("src/main/java/dev/vehiclebridge/byd/data/SessionRepository.kt",
     "return sendCommands(saved,vehicle,pin,action,progress)\n    }\n    @Synchronized fun setShutdownOnLock",
     "return sendCommands(saved,vehicle,pin,action,progress,allowShutdownOnLock,voiceShutdown)\n    }\n    @Synchronized fun setShutdownOnLock")
edit("src/main/java/dev/vehiclebridge/byd/data/SessionRepository.kt",
     "private fun sendCommands(saved: SavedAccount,vehicle: Vehicle,pin: String,action: DoorAction,progress: (String)->Unit): SessionInfo {",
     "private fun sendCommands(saved: SavedAccount,vehicle: Vehicle,pin: String,action: DoorAction,progress: (String)->Unit,allowShutdownOnLock: Boolean = true,voiceShutdown: Boolean = false): SessionInfo {")
edit("src/main/java/dev/vehiclebridge/byd/data/SessionRepository.kt",
     "val combined=action==DoorAction.LOCK && vehicle.vin in saved.shutdownOnLock",
     "val combined=action==DoorAction.LOCK && (voiceShutdown || (allowShutdownOnLock && vehicle.vin in saved.shutdownOnLock))")
edit("src/main/java/dev/vehiclebridge/byd/AuthController.kt",
     '    fun saveBinding(binding: BemfaBinding)',
     '''    fun sendVoiceDoor(vin: String,action: DoorAction,shutdownAfterLock: Boolean = false) = run("正在执行语音车控","请求已发送，请现场确认",complete={dev.vehiclebridge.byd.bridge.BridgeNotifications.result(appContext,it)}) {
        repository.sendDoor(vin,action,null,::controlProgress,false,shutdownAfterLock)
    }
    fun saveBinding(binding: BemfaBinding)''')

# Disable Bemfa UI and service. The voice version does not connect to Bemfa.
edit("src/main/java/dev/vehiclebridge/byd/MainActivity.kt",
     "binding.navBridge.setOnClickListener {selectTab(1)}",
     "binding.navBridge.visibility=View.GONE")
edit("src/main/java/dev/vehiclebridge/byd/MainActivity.kt",
     "binding.bridgeStatusBadge.setOnClickListener {selectTab(1)}",
     "binding.bridgeStatusBadge.visibility=View.GONE")
edit("src/main/java/dev/vehiclebridge/byd/MainActivity.kt",
     "binding.bemfaButton.setOnClickListener {selectTab(1)}",
     "binding.bemfaButton.visibility=View.GONE")
edit("src/main/java/dev/vehiclebridge/byd/MainActivity.kt",
     "if(resumed && !state.busy && info?.binding?.enabled==true) BemfaService.start(this)",
     "// Bemfa background bridge deliberately disabled in XiaoAI edition.")
# Retain manual shutdown and manual lock + shutdown preferences.
# Combined lock/shutdown uses the upstream second CLOSEAIR command; never retry automatically.
edit("src/main/java/dev/vehiclebridge/byd/data/SessionRepository.kt",
     'if(combined && first.outcome!=RemoteOutcome.FAILURE)',
     'if(combined && first.outcome==RemoteOutcome.SUCCESS)')
edit("src/main/java/dev/vehiclebridge/byd/data/SessionRepository.kt",
     '熄火：未发送（锁车执行失败）',
     '熄火：未发送（锁车未确认成功）')

p = app / "src/main/AndroidManifest.xml"
xml = p.read_text(encoding="utf8")
start = xml.index(' <uses-permission')
# Keep only network permissions required for cloud vehicle API.
xml = re.sub(r'\s*<uses-permission android:name="android.permission.(?!INTERNET|ACCESS_NETWORK_STATE)[^"]+" />', '', xml)
xml = xml.replace('android:networkSecurityConfig="@xml/network_security_config"', '')
xml = xml.replace('  <service android:name=".bridge.BemfaService" android:exported="false" android:foregroundServiceType="connectedDevice" />', '')
xml = re.sub(r'\s*<receiver android:name="\.bridge\.BridgeBootReceiver".*?</receiver>', '', xml,flags=re.S)
xml = xml.replace('<application ','''<permission android:name="dev.vehiclebridge.byd.xiaoai.permission.VOICE_ENTRY"
             android:protectionLevel="signature" />
 <application ''',1)
xml = xml.replace(' </application>', '''  <activity android:name=".VoiceCommandActivity"
      android:exported="true"
      android:permission="dev.vehiclebridge.byd.xiaoai.permission.VOICE_ENTRY" />
 </application>''',1)
p.write_text(xml,encoding="utf8")
print("XiaoAI V2 patch applied: signature voice gateway, three voice actions, no 45s cooldown, lockscreen display attempt, Bemfa disabled.")
