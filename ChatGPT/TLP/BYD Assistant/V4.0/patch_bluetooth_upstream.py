#!/usr/bin/env python3
"""Second stage AFTER patch_dynamic_upstream.py: Bluetooth feature plumbing and UI."""
from pathlib import Path
import shutil,sys,xml.etree.ElementTree as ET
root=Path(sys.argv[1]); overlay=Path(sys.argv[2]); app=root/"app"
base="src/main/java/dev/vehiclebridge/byd/"
def edit(path,old,new,count=1):
    p=app/path;s=p.read_text(encoding="utf-8")
    if s.count(old)!=count:raise RuntimeError(f"{path}: expected {count} anchors, got {s.count(old)}: {old[:80]}")
    p.write_text(s.replace(old,new,count),encoding="utf-8")

shutil.copyfile(overlay/"VehicleBluetoothService.kt",app/(base+"bridge/VehicleBluetoothService.kt"))
edit("src/main/AndroidManifest.xml",
     ' <uses-permission android:name="android.permission.WAKE_LOCK" />',
     ''' <uses-permission android:name="android.permission.WAKE_LOCK" />
 <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
 <uses-permission android:name="android.permission.VIBRATE" />''')
edit("src/main/AndroidManifest.xml",
     '  <service android:name=".bridge.BemfaService" android:exported="false" android:foregroundServiceType="connectedDevice" />',
     '''  <service android:name=".bridge.BemfaService" android:exported="false" android:foregroundServiceType="connectedDevice" />
  <service android:name=".bridge.VehicleBluetoothService" android:exported="false" android:foregroundServiceType="connectedDevice" />''')
manifest=(app/"src/main/AndroidManifest.xml").read_text(encoding="utf-8")
ET.fromstring(manifest)
edit(base+"bridge/BridgeBootReceiver.kt",
     'if(saved?.binding?.enabled==true && saved.pins.containsKey(saved.binding.vin)) BemfaService.start(context)',
     '''if(saved?.binding?.enabled==true && saved.pins.containsKey(saved.binding.vin)) BemfaService.start(context)
                if(saved!=null) VehicleBluetoothService.start(context)''')

# Three independent switches + bonded device picker + editable delay.
layout="src/main/res/layout/activity_main.xml"
edit(layout,'<Button android:id="@+id/notificationSettings"',
'''<TextView style="@style/Section" android:text="车辆蓝牙自动化（需先选择已配对车辆）" android:layout_marginTop="12dp" />
                        <Button android:id="@+id/selectVehicleBluetooth" style="@style/ActionButton"
                                android:text="选择已配对车辆蓝牙" android:layout_marginTop="8dp" />
                        <TextView android:id="@+id/vehicleBluetoothSelected" style="@style/Caption"
                                  android:text="尚未绑定已配对设备" android:layout_marginTop="8dp" />
                        <Switch android:id="@+id/autoUnlockBluetooth" android:layout_width="match_parent"
                                android:layout_height="wrap_content" android:minHeight="48dp"
                                android:text="连接车辆蓝牙自动解锁" />
                        <Switch android:id="@+id/autoLockBluetooth" android:layout_width="match_parent"
                                android:layout_height="wrap_content" android:minHeight="48dp"
                                android:text="断开车辆蓝牙自动锁车" />
                        <Switch android:id="@+id/remindPossibleUnlocked" android:layout_width="match_parent"
                                android:layout_height="wrap_content" android:minHeight="48dp"
                                android:text="可能未锁车提醒（通知与振动）" />
                        <TextView style="@style/Caption" android:text="提醒延迟：仅表示可能未锁，非真实车锁状态；应用后台被系统终止可能导致提醒失效。" />
                        <Spinner android:id="@+id/unlockedReminderMinutes"
                                 android:layout_width="match_parent" android:layout_height="48dp" />
                        <Button android:id="@+id/notificationSettings"''')
# Runtime BLUETOOTH_CONNECT permission: request on settings enable or pairing selection.
edit(base+"MainActivity.kt",
     'import android.content.Intent',
     '''import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.bluetooth.BluetoothManager
import dev.vehiclebridge.byd.bridge.VehicleBluetoothService''')
anchor='        binding.vehicleBluetoothName.setText(bluetoothPrefs.getString("vehicle_name","秦Plus") ?: "秦Plus")'
setup='''        val reminderDelays=listOf(1,3,5,10,15,30,60)
        binding.unlockedReminderMinutes.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,
            reminderDelays.map {"$it 分钟"})
        val selectedReminder=bluetoothPrefs.getInt("reminder_minutes",5)
        binding.unlockedReminderMinutes.setSelection(reminderDelays.indexOf(selectedReminder).coerceAtLeast(0))
        binding.unlockedReminderMinutes.onItemSelectedListener=object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?,view: View?,position: Int,id: Long) {
                bluetoothPrefs.edit().putInt("reminder_minutes",reminderDelays[position]).apply()
                updateBluetoothMonitor()
            }
        }
        val storedAddress=bluetoothPrefs.getString("device_address","").orEmpty()
        binding.vehicleBluetoothSelected.text=if(storedAddress.isBlank()) "尚未绑定已配对设备"
            else "已绑定设备地址："+storedAddress.take(5)+"…"+storedAddress.takeLast(5)
        val toggleBindings=listOf(
            binding.autoUnlockBluetooth to "auto_unlock",
            binding.autoLockBluetooth to "auto_lock",
            binding.remindPossibleUnlocked to "possible_unlocked_reminder"
        )
        for((sw,key) in toggleBindings) {
            sw.isChecked=bluetoothPrefs.getBoolean(key,false)
            sw.setOnCheckedChangeListener {_,checked ->
                bluetoothPrefs.edit().putBoolean(key,checked).apply()
                updateBluetoothMonitor()
                if(key=="possible_unlocked_reminder" && checked && Build.VERSION.SDK_INT>=33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),4043)
            }
        }
        binding.selectVehicleBluetooth.setOnClickListener { selectBondedVehicleBluetooth() }
        '''
edit(base+"MainActivity.kt",anchor,setup+anchor)
anchor='    private fun setClimateSetpoint(value: Int) {'
methods='''    private fun bluetoothPermissionGranted(): Boolean =
        Build.VERSION.SDK_INT<31 ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED

    private fun requestBluetoothPermissionIfNeeded(): Boolean {
        if(bluetoothPermissionGranted()) return false
        if(Build.VERSION.SDK_INT>=31)
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT),4042)
        return true
    }

    private fun selectBondedVehicleBluetooth() {
        if(requestBluetoothPermissionIfNeeded()) return
        try {
            val manager=getSystemService(BluetoothManager::class.java)
            val paired=manager.adapter?.bondedDevices?.sortedBy {it.name.orEmpty()}?.toList().orEmpty()
            if(paired.isEmpty()) {
                AlertDialog.Builder(this).setMessage("没有检测到已配对的蓝牙设备，请先在系统蓝牙设置中配对车辆。")
                    .setPositiveButton("知道了",null).show()
                return
            }
            val names=paired.map { (it.name ?: "未知设备") + "  ("+it.address.take(5)+"…)"} .toTypedArray()
            AlertDialog.Builder(this).setTitle("选择车辆蓝牙设备")
                .setItems(names) {_,i ->
                    val item=paired[i]
                    getSharedPreferences("xiaodi_bluetooth",MODE_PRIVATE).edit()
                        .putString("device_address",item.address)
                        .putString("vehicle_name",item.name ?: "秦Plus").apply()
                    binding.vehicleBluetoothName.setText(item.name ?: "秦Plus")
                    binding.vehicleBluetoothSelected.text="已绑定："+names[i]
                    updateBluetoothMonitor()
                }.setNegativeButton("取消",null).show()
        } catch (_: SecurityException) {
            requestBluetoothPermissionIfNeeded()
        }
    }

    private fun updateBluetoothMonitor() {
        val prefs=getSharedPreferences("xiaodi_bluetooth",MODE_PRIVATE)
        val active=prefs.getBoolean("auto_unlock",false) ||
            prefs.getBoolean("auto_lock",false) ||
            prefs.getBoolean("possible_unlocked_reminder",false)
        val hasDevice=!prefs.getString("device_address","").isNullOrBlank()
        prefs.edit().putBoolean("monitor_enabled",active && hasDevice).apply()
        if(active && !hasDevice) {
            android.widget.Toast.makeText(this,"请先绑定已配对的车辆蓝牙设备",android.widget.Toast.LENGTH_LONG).show()
            stopService(Intent(this,VehicleBluetoothService::class.java))
            return
        }
        if(active && requestBluetoothPermissionIfNeeded()) return
        if(active && hasDevice) VehicleBluetoothService.start(this)
        else stopService(Intent(this,VehicleBluetoothService::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==4042 && grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)
            updateBluetoothMonitor()
    }

    '''
edit(base+"MainActivity.kt",anchor,methods+anchor)
print("Bluetooth foreground monitoring/permission and three independent UI switches added.")
