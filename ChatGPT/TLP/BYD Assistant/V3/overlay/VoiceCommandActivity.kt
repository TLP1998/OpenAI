package dev.vehiclebridge.byd

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dev.vehiclebridge.byd.protocol.DoorAction

/**
 * V3: ONE Android APK, four Launcher activities (main screen plus three voice actions).
 *
 * HyperOS XiaoAI recognition of several launcher activities sharing an app package
 * must be proven on the physical Xiaomi 15. This does not provide speaker identity.
 */
enum class VoiceMode { LOCK, UNLOCK, LOCK_CLOSEAIR }

abstract class VoiceCommandActivity : Activity() {
    protected abstract val voiceMode: VoiceMode

    private lateinit var controller: AuthController
    private lateinit var status: TextView
    private var attempted = false
    private var executing = false
    private var disposed = false
    private val observer: (AuthState) -> Unit = { state -> updateState(state) }

    @Suppress("DEPRECATION")
    private fun showOnLockscreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOnLockscreen()

        val title = when (voiceMode) {
            VoiceMode.LOCK -> "小破车上锁"
            VoiceMode.UNLOCK -> "小破车解锁"
            VoiceMode.LOCK_CLOSEAIR -> "小破车上锁并熄火"
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 48)
            setBackgroundColor(Color.rgb(246, 248, 252))
        }
        root.addView(TextView(this).apply {
            text = "小爱语音 · " + title
            textSize = 26f
            setTextColor(Color.rgb(22, 42, 82))
        })
        status = TextView(this).apply {
            text = "正在恢复车辆会话…"
            textSize = 18f
            setTextColor(Color.DKGRAY)
            setPadding(0, 32, 0, 28)
        }
        root.addView(status)

        // Never launch an account-settings activity over the lock screen.
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguard.isKeyguardLocked) {
            root.addView(Button(this).apply {
                text = "进入小破车主界面（账号 / 车辆 / PIN）"
                setOnClickListener {
                    startActivity(Intent(this@VoiceCommandActivity, MainActivity::class.java)
                        .putExtra("tab", 2))
                }
            })
        }
        root.addView(Button(this).apply {
            text = "关闭"
            setOnClickListener { finish() }
        })
        root.addView(TextView(this).apply {
            text = "\nV3 单 APK：不使用巴法云，也不设 45 秒冷却；同一时间不并发发送，且不自动重试。\n" +
                "「上锁并熄火」实际是上锁后向比亚迪云发送 CLOSEAIR（关闭空调）请求，" +
                "不等于已经验证的发动机熄火。\n" +
                "锁屏下无需二次身份认证的解锁存在安全风险，且能否唤起入口取决于 HyperOS。"
            textSize = 13f
            setTextColor(Color.GRAY)
        })
        setContentView(ScrollView(this).apply { addView(root) })

        // A configuration recreation must not repeat a remote command.
        if (savedInstanceState != null) {
            attempted = true
            status.text = "页面重新创建，本次不会重复发送车控命令。请核对车辆实际状态。"
            return
        }
        controller = AuthController.get(this)
        controller.initialize()
        controller.observe(observer)
    }

    private fun updateState(state: AuthState) {
        if (disposed || isFinishing) return
        if (executing) {
            status.text = state.message + "\n\n请以车辆实际执行结果为准。"
            return
        }
        if (attempted) return
        if (state.busy) {
            status.text = "正在恢复车辆会话，请稍候…"
            return
        }

        val vehicle = state.info?.selected
        if (vehicle == null) {
            attempted = true
            status.text = "尚未登录或未选择车辆，请先进入主界面完成设置。\n" + state.message
            return
        }
        if (state.info?.hasPin != true) {
            attempted = true
            status.text = "未保存操作 PIN，请先在车控主界面验证并保存。"
            return
        }

        attempted = true
        executing = true
        val lockAndCloseAir = voiceMode == VoiceMode.LOCK_CLOSEAIR
        val action = if (voiceMode == VoiceMode.UNLOCK) DoorAction.UNLOCK else DoorAction.LOCK
        status.text = when (voiceMode) {
            VoiceMode.LOCK -> "正在发送车辆上锁请求…"
            VoiceMode.UNLOCK -> "正在发送车辆解锁请求…"
            VoiceMode.LOCK_CLOSEAIR -> "正在发送锁车请求，确认成功后再发送 CLOSEAIR…"
        }
        controller.sendVoiceDoor(vehicle.vin, action, lockAndCloseAir)
    }

    override fun onDestroy() {
        disposed = true
        if (::controller.isInitialized) controller.remove(observer)
        super.onDestroy()
    }
}

class VoiceLockActivity : VoiceCommandActivity() {
    override val voiceMode: VoiceMode = VoiceMode.LOCK
}

class VoiceUnlockActivity : VoiceCommandActivity() {
    override val voiceMode: VoiceMode = VoiceMode.UNLOCK
}

class VoiceLockCloseAirActivity : VoiceCommandActivity() {
    override val voiceMode: VoiceMode = VoiceMode.LOCK_CLOSEAIR
}
