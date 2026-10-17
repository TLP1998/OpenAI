#!/usr/bin/env python3
"""Apply V4 dynamic climate controls to a matching fm1223/byd-android source checkout.
Development-only: verify China region OPENAIR, real Bemfa frames, signature before shipping.
"""
from pathlib import Path
import shutil
import sys
import xml.etree.ElementTree as ET

repo = Path(sys.argv[1]).resolve()
overlay = Path(sys.argv[2]).resolve()
app = repo / "app"
base = "src/main/java/dev/vehiclebridge/byd/"

def edit(relative, old, new, n=1):
    p = app / relative
    value = p.read_text(encoding="utf-8")
    count = value.count(old)
    if count != n:
        raise RuntimeError(f"Source mismatch at {relative}: expected {n}, found {count} for {old[:90]!r}")
    p.write_text(value.replace(old, new, n), encoding="utf-8")

edit("build.gradle.kts",
     'versionCode = 19; versionName = "0.4.12-dev"',
     'versionCode = 40; versionName = "4.0.0-dev"')
# Keep applicationId unchanged: dev.vehiclebridge.byd.
# This DOES NOT guarantee upgrade: the signing certificate must also match.

edit(base+"protocol/Vehicle.kt",
     'SHUTDOWN("CLOSEAIR") }',
     'SHUTDOWN("CLOSEAIR"), CLIMATE("OPENAIR") }')
edit(base+"protocol/ControlReceipt.kt",
     'DoorAction.SHUTDOWN -> "熄火"',
     'DoorAction.SHUTDOWN -> "关闭空调";DoorAction.CLIMATE -> "设置车内空调"')
edit(base+"data/BydApi.kt",
     'DoorAction.SHUTDOWN -> "熄火"',
     'DoorAction.SHUTDOWN -> "关闭空调";DoorAction.CLIMATE -> "设置车内空调"')

# Extend BYD wire request with temperature; only OPENAIR uses nonempty parameters.
edit(base+"protocol/CnProtocol.kt",
     'action: DoorAction, requestSerial: String? = null): String {',
     'action: DoorAction, requestSerial: String? = null, temperatureCelsius: Int? = null, durationMinutes: Int = 30): String {')
edit(base+"protocol/CnProtocol.kt",
     '"commandPwd" to CnCrypto.md5Upper(pin),"commandType" to action.command,"controlParamsMap" to "{}",',
     '"commandPwd" to CnCrypto.md5Upper(pin),"commandType" to action.command,"controlParamsMap" to (if(action==DoorAction.CLIMATE) dev.vehiclebridge.byd.bridge.ClimateControlParams.forTemperature(requireNotNull(temperatureCelsius),durationMinutes) else "{}"),')
# Forward temperature through the existing BYD transport / receipt polling.
edit(base+"data/BydApi.kt",
     'action: DoorAction,progress: (String)->Unit = {}): ControlReceipt {',
     'action: DoorAction,progress: (String)->Unit = {},temperatureCelsius: Int? = null,durationMinutes: Int = 30): ControlReceipt {')
edit(base+"data/BydApi.kt",
     'action: DoorAction,progress: (String)->Unit): ControlReceipt {',
     'action: DoorAction,progress: (String)->Unit,temperatureCelsius: Int?,durationMinutes: Int): ControlReceipt {')
edit(base+"data/BydApi.kt",
     'action: DoorAction,serial: String?): ControlReceipt {',
     'action: DoorAction,serial: String?,temperatureCelsius: Int? = null,durationMinutes: Int = 30): ControlReceipt {')
edit(base+"data/BydApi.kt",
     'protocol.doorRequest(session,vehicle,pin,action,serial)',
     'protocol.doorRequest(session,vehicle,pin,action,serial,temperatureCelsius,durationMinutes)')
edit(base+"data/BydApi.kt",
     'val first=dispatch(config,session,vehicle,pin,action,sentSerial)',
     'val first=dispatch(config,session,vehicle,pin,action,sentSerial,temperatureCelsius,durationMinutes)')

# Repository: no app re-login, use the stored PIN and old binding. The command
# is parameterised only for OPENAIR, retaining all existing non-climate calls.
edit(base+"data/SessionRepository.kt",
     'private fun sendCommands(saved: SavedAccount,vehicle: Vehicle,pin: String,action: DoorAction,progress: (String)->Unit): SessionInfo {',
     'private fun sendCommands(saved: SavedAccount,vehicle: Vehicle,pin: String,action: DoorAction,progress: (String)->Unit,forceCloseAir: Boolean = false,temperatureCelsius: Int? = null,durationMinutes: Int = 30): SessionInfo {')
edit(base+"data/SessionRepository.kt",
     'val first=api.executeControl(saved.config,saved.session,vehicle,pin,action,progress)',
     'val first=api.executeControl(saved.config,saved.session,vehicle,pin,action,progress,temperatureCelsius,durationMinutes)')
edit(base+"data/SessionRepository.kt",
     'val combined=action==DoorAction.LOCK && vehicle.vin in saved.shutdownOnLock',
     'val combined=action==DoorAction.LOCK && (forceCloseAir || vehicle.vin in saved.shutdownOnLock)')
edit(base+"data/SessionRepository.kt",
     'if(combined && first.outcome!=RemoteOutcome.FAILURE)',
     'if(combined && first.outcome==RemoteOutcome.SUCCESS)')
# Kotlin trailing-lambda syntax would otherwise bind to the new trailing
# durationMinutes integer argument instead of the progress callback.
edit(base+"data/SessionRepository.kt",
     'api.executeControl(saved.config,saved.session,vehicle,pin,DoorAction.SHUTDOWN) {progress(first.describe(action)+"\\n"+it)}',
     'api.executeControl(saved.config,saved.session,vehicle,pin,DoorAction.SHUTDOWN,{progress(first.describe(action)+"\\n"+it)})')
edit(base+"data/SessionRepository.kt",
     'fun sendBoundDoor(bindingId: String, action: DoorAction, progress: (String)->Unit = {}): SessionInfo {',
     'fun sendBoundDoor(bindingId: String, action: DoorAction, progress: (String)->Unit = {},temperatureCelsius: Int? = null,durationMinutes: Int = 30): SessionInfo {')
p = app/(base+"data/SessionRepository.kt")
s = p.read_text(encoding="utf-8")
i = s.index("    @Synchronized fun sendBoundDoor(")
prefix, suffix = s[:i],s[i:]
original = 'return sendCommands(saved,vehicle,pin,action,progress)'
assert suffix.count(original) == 1
suffix=suffix.replace(original,
    'return sendCommands(saved,vehicle,pin,action,progress,forceCloseAir=(action==DoorAction.LOCK),temperatureCelsius=temperatureCelsius,durationMinutes=durationMinutes)', 1)
p.write_text(prefix+suffix,encoding="utf-8")

# Explicit local AC buttons: editable setpoint and manual CLOSEAIR.
edit(base+"AuthController.kt",
     'fun dispatchBridge(id: String,action: DoorAction,complete: (String)->Unit)',
     'fun dispatchBridge(id: String,action: DoorAction,temperatureCelsius: Int? = null,durationMinutes: Int = 30,complete: (String)->Unit)')
edit(base+"AuthController.kt",
     'repository.sendBoundDoor(id,action,::controlProgress)',
     'repository.sendBoundDoor(id,action,::controlProgress,temperatureCelsius,durationMinutes)')
edit(base+"AuthController.kt",
     '    fun saveBinding(binding: BemfaBinding)',
     '''    fun sendClimate(vin: String,temperatureCelsius: Int,durationMinutes: Int) = run("正在设置空调",
        "已发送空调请求，请实车核实",complete={dev.vehiclebridge.byd.bridge.BridgeNotifications.result(appContext,it)}) {
        repository.sendClimate(vin,temperatureCelsius,durationMinutes,::controlProgress)
    }
    fun saveBinding(binding: BemfaBinding)''')
edit(base+"data/SessionRepository.kt",
     '    @Synchronized fun setShutdownOnLock(vin: String, enabled: Boolean): SessionInfo {',
     '''    @Synchronized fun sendClimate(vin: String,temperatureCelsius: Int,durationMinutes: Int,progress: (String)->Unit = {}): SessionInfo {
        require(temperatureCelsius in 15..31)\n        require(durationMinutes in listOf(10,15,20,25,30))
        val (saved,vehicle)=refreshed(vin)
        val pin=saved.pins[vin] ?: throw ApiFailure(null,"请先在设置中验证并保存操作 PIN")
        api.verifyPin(saved.config,saved.session,vehicle,pin)
        return sendCommands(saved,vehicle,pin,DoorAction.CLIMATE,progress,temperatureCelsius=temperatureCelsius,durationMinutes=durationMinutes)
    }
    @Synchronized fun setShutdownOnLock(vin: String, enabled: Boolean): SessionInfo {''')

# Main UI: temperature +- controls, apply, stop AC button (does NOT stop engine).
layout = "src/main/res/layout/activity_main.xml"
anchor = '<Button android:id="@+id/shutdownButton"'
extra = '''<LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                            android:orientation="vertical" android:layout_marginTop="16dp">
                        <TextView style="@style/Section" android:text="远程空调（可设置时长）" />
                        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                                      android:orientation="horizontal" android:gravity="center_vertical">
                            <Button android:id="@+id/climateMinus" android:layout_width="56dp" android:layout_height="48dp" android:text="−" />
                            <TextView android:id="@+id/climateTemperature" android:layout_width="0dp"
                                android:layout_weight="1" android:layout_height="wrap_content"
                                android:text="26 ℃" android:gravity="center" android:textSize="20sp" />
                            <Button android:id="@+id/climatePlus" android:layout_width="56dp" android:layout_height="48dp" android:text="+" />
                        </LinearLayout>
                        <TextView style="@style/Caption" android:text="空调运行时间（语音和手动操作共用）" />
                        <Spinner android:id="@+id/climateDuration" android:layout_width="match_parent"
                                 android:layout_height="48dp" />
                        <Button android:id="@+id/climateApply" style="@style/ActionButton"
                                android:text="设定温度并启动空调" />
                        <Button android:id="@+id/climateStop" style="@style/ActionButton"
                                android:text="关闭空调（CLOSEAIR）" android:layout_marginTop="8dp" />
                    </LinearLayout>
                        '''
edit(layout,anchor,extra+anchor)
xml = (app/layout).read_text(encoding="utf-8")
ET.fromstring(xml)

edit(base+"MainActivity.kt",
     '    private var manualRequest=false',
     '    private var manualRequest=false\n    private var climateSetpoint=26\n    private var climateDurationMinutes=30')
edit(base+"MainActivity.kt",
     '        binding.shutdownButton.setOnClickListener {sendDoor(DoorAction.SHUTDOWN)}',
     '''        binding.shutdownButton.setOnClickListener {sendDoor(DoorAction.SHUTDOWN)}
        val durationOptions=listOf(10,15,20,25,30)
        val climatePrefs=getSharedPreferences("xiaodi_climate",MODE_PRIVATE)
        climateDurationMinutes=climatePrefs.getInt("duration_minutes",30)
            .takeIf {it in durationOptions} ?: 30
        binding.climateDuration.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,
            durationOptions.map {"$it 分钟"})
        binding.climateDuration.setSelection(durationOptions.indexOf(climateDurationMinutes))
        binding.climateDuration.onItemSelectedListener=object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?,view: View?,position: Int,id: Long) {
                climateDurationMinutes=durationOptions[position]
                climatePrefs.edit().putInt("duration_minutes",climateDurationMinutes).apply()
            }
        }
        binding.climateMinus.setOnClickListener {setClimateSetpoint(climateSetpoint-1)}
        binding.climatePlus.setOnClickListener {setClimateSetpoint(climateSetpoint+1)}
        binding.climateApply.setOnClickListener {
            val info=controller.state.info ?: return@setOnClickListener
            val vin=info.selected?.vin ?: return@setOnClickListener
            if(!controller.state.busy && info.hasPin) {
                manualRequest=true
                controller.sendClimate(vin,climateSetpoint,climateDurationMinutes)
            } else if(!info.hasPin) selectTab(2)
        }
        binding.climateStop.setOnClickListener {sendDoor(DoorAction.SHUTDOWN)}''')
edit(base+"MainActivity.kt",
     '    private fun sendDoor(action: DoorAction) {',
     '''    private fun setClimateSetpoint(value: Int) {
        climateSetpoint=value.coerceIn(15,31)
        binding.climateTemperature.text="$climateSetpoint ℃"
    }
    private fun sendDoor(action: DoorAction) {''')
edit(base+"MainActivity.kt",
     '        binding.shutdownOnLock.setOnCheckedChangeListener(null)',
     '''        listOf(binding.climateMinus,binding.climatePlus,binding.climateDuration,binding.climateApply,binding.climateStop)
            .forEach {it.isEnabled=!state.busy && vehicle!=null}
        binding.shutdownOnLock.setOnCheckedChangeListener(null)''')

# MQTT gate handles 6-field commands and dispatches actual numeric setpoints.
shutil.copyfile(overlay/"ClimateControlParams.kt", app/(base+"bridge/ClimateControlParams.kt"))
shutil.copyfile(overlay/"XiaodiCommandParser.kt", app/(base+"bridge/XiaodiCommandParser.kt"))
shutil.copyfile(overlay/"ClimateCommandGate.kt", app/(base+"bridge/ClimateCommandGate.kt"))
(app/"src/test/java/dev/vehiclebridge/byd/bridge").mkdir(parents=True, exist_ok=True)
shutil.copyfile(overlay/"XiaodiCommandParserTest.kt", app/"src/test/java/dev/vehiclebridge/byd/bridge/XiaodiCommandParserTest.kt")
shutil.copyfile(overlay/"ClimateControlParamsTest.kt", app/"src/test/java/dev/vehiclebridge/byd/bridge/ClimateControlParamsTest.kt")
edit(base+"bridge/BemfaService.kt",
     'private var gate: CommandGate?=null',
     'private var gate: ClimateCommandGate?=null')
edit(base+"bridge/BemfaService.kt",
     'gate=CommandGate(next.topic)',
     'gate=ClimateCommandGate(next.topic)')
edit(base+"bridge/BemfaService.kt",
     'controller.dispatchBridge(target.id,action) {result ->',
     'controller.dispatchBridge(target.id,action.action,action.temperatureCelsius,
                                    getSharedPreferences("xiaodi_climate",MODE_PRIVATE)
                                        .getInt("duration_minutes",30).takeIf {it in listOf(10,15,20,25,30)} ?: 30) {result ->')

print("V4 dynamic MQTT temperature, fixed 30min OPENAIR, UI +/- apply and stop AC patched.")
print("DO NOT SHIP without compilation, APK signature compatibility, and real-vehicle validation.")
