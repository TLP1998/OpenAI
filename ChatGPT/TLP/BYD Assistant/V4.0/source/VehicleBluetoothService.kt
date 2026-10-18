package dev.vehiclebridge.byd.bridge

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dev.vehiclebridge.byd.AuthController
import dev.vehiclebridge.byd.MainActivity
import dev.vehiclebridge.byd.R
import dev.vehiclebridge.byd.protocol.DoorAction

/** User-opt-in foreground Bluetooth monitor. No name-only device matching.
 * ACL events are not cryptographic proximity authentication and do not prove
 * a door's locked status. User must select a bonded hardware address.
 */
class VehicleBluetoothService: Service() {
    companion object {
        private const val CHANNEL = "xiaodi_bt_monitor"
        private const val REMINDER_CHANNEL = "xiaodi_possible_unlocked"
        private const val ID = 43
        private const val REMINDER_ID = 44
        private const val PREFERENCES = "xiaodi_bluetooth"
        const val ACTION_STOP = "dev.vehiclebridge.byd.STOP_BLUETOOTH"
        fun prefs(context: Context) = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        fun start(context: Context) {
            val p=prefs(context)
            if (!p.getBoolean("monitor_enabled",false)) return
            if (Build.VERSION.SDK_INT>=31 &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) return
            try { context.startForegroundService(Intent(context,VehicleBluetoothService::class.java)) }
            catch (_: Exception) {}
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val decider = BluetoothActionDecider()
    private var registered=false
    private var reminder: Runnable?=null
    private var disconnectPending: Runnable?=null
    private var lastAddress: String?=null
    private var connected=false
    private lateinit var controller: AuthController

    private val receiver=object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val address = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.address ?: return
            val p=prefs(this@VehicleBluetoothService)
            if (!address.equals(p.getString("device_address", ""),ignoreCase=true)) return
            if (intent.action==BluetoothDevice.ACTION_ACL_CONNECTED) {
                disconnectPending?.let { handler.removeCallbacks(it) }
                disconnectPending=null
                connected=true
                process(address,true)
            } else if (intent.action==BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                connected=false
                // Small grace period for momentary reconnects.
                disconnectPending?.let { handler.removeCallbacks(it) }
                disconnectPending=Runnable {
                    if (!connected) process(address,false)
                }.also { handler.postDelayed(it,15000) }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder?=null

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        val nm=getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL,"车辆蓝牙自动化",NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL,"车辆可能未锁提醒",NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern=longArrayOf(0,300,200,300)
        })
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val note=Notification.Builder(this,CHANNEL).setContentTitle("小迪车控助手 · 蓝牙监听")
            .setContentText("已启用，等待已绑定的车辆蓝牙事件")
            .setSmallIcon(R.drawable.ic_app).setOngoing(true).setContentIntent(open).build()
        if (Build.VERSION.SDK_INT>=29) startForeground(ID,note,ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        else startForeground(ID,note)
        controller=AuthController.get(this)
        if (Build.VERSION.SDK_INT>=31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) {
            stopSelf();return
        }
        registerReceiver(receiver,IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        })
        registered=true
    }

    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        if (intent?.action==ACTION_STOP || !prefs(this).getBoolean("monitor_enabled",false)) {
            stopSelf();return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun options(): BluetoothOptions {
        val p=prefs(this)
        return BluetoothOptions(
            enabledUnlockOnConnect=p.getBoolean("auto_unlock",false),
            enabledLockOnDisconnect=p.getBoolean("auto_lock",false),
            enabledPossibleUnlockedReminder=p.getBoolean("possible_unlocked_reminder",false),
            selectedDeviceAddress=p.getString("device_address","") ?: "",
            configuredDeviceName=p.getString("vehicle_name","秦Plus") ?: "秦Plus",
            reminderMinutes=p.getInt("reminder_minutes",5).coerceIn(1,60)
        )
    }

    private fun process(address: String, isConnected: Boolean) {
        val action=decider.evaluate(address,isConnected,options()) ?: return
        // The user must have selected a verified vehicle with stored PIN.
        val info=controller.state.info ?: return
        if (controller.state.busy || info.selected==null || !info.hasPin) return
        lastAddress=address
        when(action) {
            BluetoothAction.UNLOCK -> {
                controller.sendDoor(info.selected!!.vin,DoorAction.UNLOCK)
                // This is a possible-unlocked reminder, NOT an assertion of door state.
                scheduleReminder(address)
            }
            BluetoothAction.LOCK -> {
                controller.sendDoor(info.selected!!.vin,DoorAction.LOCK)
                cancelReminder()
            }
        }
    }

    private fun scheduleReminder(address: String) {
        cancelReminder()
        val config=options()
        if (!config.enabledPossibleUnlockedReminder) return
        val delay=config.reminderMinutes*60000L
        reminder=Runnable {
            if (options().enabledPossibleUnlockedReminder &&
                address.equals(options().selectedDeviceAddress,ignoreCase=true)) notifyPossibleUnlocked()
        }.also {handler.postDelayed(it,delay)}
    }

    private fun notifyPossibleUnlocked() {
        val nm=getSystemService(NotificationManager::class.java)
        val note=Notification.Builder(this,REMINDER_CHANNEL)
            .setContentTitle("车辆可能未上锁，请核实")
            .setContentText("蓝牙事件和远控请求无法确认车锁状态，请查看车辆实际状态。")
            .setSmallIcon(R.drawable.ic_app).setAutoCancel(true).build()
        try { nm.notify(REMINDER_ID,note) } catch (_: SecurityException) {}
        try {
            if (Build.VERSION.SDK_INT>=31) {
                getSystemService(VibratorManager::class.java).defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0,300,200,300),-1))
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0,300,200,300),-1))
            }
        } catch (_: Exception) {}
    }

    private fun cancelReminder() {
        reminder?.let {handler.removeCallbacks(it)}
        reminder=null
        getSystemService(NotificationManager::class.java).cancel(REMINDER_ID)
    }

    override fun onDestroy() {
        cancelReminder()
        disconnectPending?.let {handler.removeCallbacks(it)}
        if(registered) try {unregisterReceiver(receiver)}catch (_: Exception) {}
        super.onDestroy()
    }
}
