#!/usr/bin/env python3
"""Patch pinned upstream fm1223/byd-android into ONE APK containing 4 launcher activities."""
from pathlib import Path
import re
import shutil
import sys
import xml.etree.ElementTree as ET

repo = Path(sys.argv[1]).resolve()
overlay = Path(sys.argv[2]).resolve()
app = repo / "app"

def edit(path, old, new, count=1):
    p = app / path
    content = p.read_text(encoding="utf8")
    actual = content.count(old)
    if actual != count:
        raise RuntimeError("Upstream changed: expected %s copies of %r in %s, got %s" %
                           (count, old[:90], p, actual))
    p.write_text(content.replace(old, new, count), encoding="utf8")

# Fresh V3 package name allows safe SIDE-BY-SIDE installation with known-working V2.2.
edit("build.gradle.kts",
     'applicationId = "dev.vehiclebridge.byd"',
     'applicationId = "dev.vehiclebridge.byd.xiaoai.v3"')
edit("build.gradle.kts",
     'versionCode = 19; versionName = "0.4.12-dev"',
     'versionCode = 30; versionName = "3.0.0-preview"')
edit("src/main/res/values/strings.xml",
     '<string name="app_name">车辆助手</string>',
     '<string name="app_name">小破车智能车控</string>')
edit("src/main/res/values/strings.xml",
     '</resources>',
     ''' <string name="voice_lock_name">小破车上锁</string>
 <string name="voice_unlock_name">小破车解锁</string>
 <string name="voice_lock_closeair_name">小破车上锁并熄火</string>
</resources>''')

repo_src = app / "src/main/java/dev/vehiclebridge/byd"
shutil.copyfile(overlay / "VoiceCommandActivity.kt", repo_src / "VoiceCommandActivity.kt")

# Preserve tested V2.2 voice-specific controller and safe combination behaviour.
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
edit("src/main/java/dev/vehiclebridge/byd/data/SessionRepository.kt",
     'if(combined && first.outcome!=RemoteOutcome.FAILURE)',
     'if(combined && first.outcome==RemoteOutcome.SUCCESS)')
edit("src/main/java/dev/vehiclebridge/byd/data/SessionRepository.kt",
     '熄火：未发送（锁车执行失败）',
     '熄火：未发送（锁车未确认成功）')
edit("src/main/java/dev/vehiclebridge/byd/AuthController.kt",
     '    fun saveBinding(binding: BemfaBinding)',
     '''    fun sendVoiceDoor(vin: String,action: DoorAction,shutdownAfterLock: Boolean = false) = run("正在执行语音车控","请求已发送，请现场确认",complete={dev.vehiclebridge.byd.bridge.BridgeNotifications.result(appContext,it)}) {
        repository.sendDoor(vin,action,null,::controlProgress,false,shutdownAfterLock)
    }
    fun saveBinding(binding: BemfaBinding)''')

# Disable Bemfa UI/service/boot receiver, while keeping existing settings and real BYD APIs.
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
     "// Disabled: no Bemfa background process in V3.")

manifest_path = app / "src/main/AndroidManifest.xml"
xml = manifest_path.read_text(encoding="utf8")
xml = re.sub(r'\s*<uses-permission android:name="android.permission.(?!INTERNET|ACCESS_NETWORK_STATE)[^"]+" />', '', xml)
xml = xml.replace('android:networkSecurityConfig="@xml/network_security_config"', '')
xml = xml.replace('  <service android:name=".bridge.BemfaService" android:exported="false" android:foregroundServiceType="connectedDevice" />', '')
xml = re.sub(r'\s*<receiver android:name="\.bridge\.BridgeBootReceiver".*?</receiver>', '', xml, flags=re.S)

# Four launcher activities in one application: original MainActivity plus THREE voice entries.
activities = [
    (".VoiceLockActivity", "@string/voice_lock_name", "@drawable/ic_lock"),
    (".VoiceUnlockActivity", "@string/voice_unlock_name", "@drawable/ic_unlock"),
    (".VoiceLockCloseAirActivity", "@string/voice_lock_closeair_name", "@drawable/ic_lock"),
]
additions = []
for activity, label, icon in activities:
    additions.append('''  <activity android:name="%s"
       android:exported="true" android:label="%s" android:icon="%s"
       android:showWhenLocked="true" android:turnScreenOn="true">
    <intent-filter>
      <action android:name="android.intent.action.MAIN" />
      <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
  </activity>''' % (activity, label, icon))
assert xml.count(' </application>') == 1
xml = xml.replace(' </application>', '\n'.join(additions) + '\n </application>')
ET.fromstring(xml)
assert xml.count('android.intent.category.LAUNCHER') == 4, "Need main UI + three voice launcher entries"
assert "android.permission.VOICE_ENTRY" not in xml
manifest_path.write_text(xml, encoding="utf8")

print("V3 patched: one APK, 4 launchable activities, no Bemfa, voice actions preserve V2.2.")
