package dev.vehiclebridge.byd

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import dev.vehiclebridge.byd.protocol.DoorAction

/**
 * Voice entry controlled by signature permission.
 * Only built-in voice launcher apps (same signing certificate) can start it.
 * LOCK: automatic, once per 45 seconds; UNLOCK: device authentication required.
 * Never executes shutdown on lock; no automatic retries.
 */
class VoiceCommandActivity : Activity() {
    companion object {
        const val ACTION_LOCK = "dev.vehiclebridge.byd.xiaoai.LOCK"
        const val ACTION_UNLOCK = "dev.vehiclebridge.byd.xiaoai.UNLOCK"
        private const val COOLDOWN_MS = 45_000L
    }

    private lateinit var controller: AuthController
    private lateinit var status: TextView
    private lateinit var heading: TextView
    private var action: DoorAction? = null
    private var attempted = false
    private var executing = false
    private var consentRequested = false
    private var cancelled = false
    private var authenticatorCancellation: CancellationSignal? = null
    private val observer: (AuthState) -> Unit = { updateState(it) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        action = when (intent?.action) {
            ACTION_LOCK -> DoorAction.LOCK
            ACTION_UNLOCK -> DoorAction.UNLOCK
            else -> null
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 48)
            setBackgroundColor(Color.rgb(246, 248, 252))
        }
        heading = TextView(this).apply {
            text = if (action == DoorAction.LOCK) "小爱语音 · 锁车" else if (action == DoorAction.UNLOCK) "小爱语音 · 解锁" else "无效入口"
            textSize = 26f
            setTextColor(Color.rgb(22, 42, 82))
        }
        root.addView(heading)
        status = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.DKGRAY)
            setPadding(0, 36, 0, 32)
            text = "正在检查车辆会话…"
        }
        root.addView(status)
        val setup = Button(this).apply {
            text = "打开车控主 App（登录 / 车辆 / PIN）"
            setOnClickListener {
                startActivity(Intent(this@VoiceCommandActivity, MainActivity::class.java).putExtra("tab", 2))
            }
        }
        root.addView(setup)
        val close = Button(this).apply { text = "关闭"; setOnClickListener { finish() } }
        root.addView(close)
        val note = TextView(this).apply {
            text = "\n安全规则：语音锁车不会附带熄火；解锁必须经过手机系统身份认证；结果未知时不自动重试。\n控制反馈不代表已读取实时车锁状态。"
            textSize = 13f
            setTextColor(Color.GRAY)
        }
        root.addView(note)
        val scroll = android.widget.ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        if (action == null) {
            show("不支持的启动操作。")
            return
        }
        // Prevent rotating the screen from automatically re-dispatching an in-flight command.
        if (state != null) {
            attempted = true
            show("页面已重新创建。为了防止重复车控，本次不再发送，请现场核对车辆。")
            return
        }
        controller = AuthController.get(this)
        controller.initialize()
        controller.observe(observer)
    }

    private fun show(message: String) { status.text = message }

    private fun updateState(state: AuthState) {
        if (cancelled || isFinishing) return
        if (executing) {
            show(state.message + "\n\n请以车辆实际执行结果为准。")
            return
        }
        if (attempted) return
        if (state.busy) { show("正在恢复比亚迪会话，请稍候…"); return }
        val info = state.info
        val vehicle = info?.selected
        if (vehicle == null) {
            attempted = true
            show("尚未登录或未选择车辆。请先打开车控主 App 登录并选择车辆。\n" + state.message)
            return
        }
        if (!info.hasPin) {
            attempted = true
            show("未保存操作 PIN。请先在车控主 App 的设置中验证并保存 PIN。")
            return
        }
        // Exactly one action is performed per activity creation.
        attempted = true
        val vin = vehicle.vin
        if (action == DoorAction.UNLOCK) {
            requestUnlockConsent(vin)
        } else {
            dispatch(vin, DoorAction.LOCK)
        }
    }

    private fun requestUnlockConsent(vin: String) {
        if (consentRequested) return
        consentRequested = true
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguard.isDeviceSecure) {
            show("拒绝解锁：手机未设置安全锁屏。请先设置系统锁屏密码或指纹。")
            return
        }
        show("等待手机系统身份认证，认证通过后才会发送解锁请求。")
        if (Build.VERSION.SDK_INT >= 30) {
            val signal = CancellationSignal()
            authenticatorCancellation = signal
            try {
                val prompt = BiometricPrompt.Builder(this)
                    .setTitle("确认远程解锁车辆")
                    .setSubtitle("请验证指纹或使用手机锁屏凭证")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()
                prompt.authenticate(signal, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (!cancelled && !isFinishing) dispatch(vin, DoorAction.UNLOCK)
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (!executing) show("未通过本人认证，未发送解锁指令。")
                    }
                })
            } catch (_: Exception) {
                show("无法启动系统身份认证，本次不执行解锁。")
            }
        } else {
            val challenge = keyguard.createConfirmDeviceCredentialIntent("确认远程解锁车辆", "通过系统锁屏认证后执行")
            if (challenge == null) {
                show("当前系统不支持安全验证，本次不执行解锁。")
            } else {
                @Suppress("DEPRECATION")
                startActivityForResult(challenge, 200)
            }
        }
    }

    @Deprecated("Legacy credential confirmation for Android 8–10 only")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 200 || cancelled || isFinishing) return
        val vin = controller.state.info?.selected?.vin
        if (resultCode == RESULT_OK && vin != null) dispatch(vin, DoorAction.UNLOCK)
        else show("解锁认证取消或未通过，没有发送车辆指令。")
    }

    private fun dispatch(vin: String, command: DoorAction) {
        if (executing || cancelled) return
        val preferences = getSharedPreferences("voice_gate", MODE_PRIVATE)
        val key = if (command == DoorAction.LOCK) "lock" else "unlock"
        val last = preferences.getLong(key, 0L)
        val now = SystemClock.elapsedRealtime()
        if (last > 0 && now >= last && now - last < COOLDOWN_MS) {
            show("45 秒内重复语音指令已拦截，不再发送车辆请求。")
            return
        }
        preferences.edit().putLong(key, now).apply()
        executing = true
        show("已通过本地语音入口，正在发送" + if (command == DoorAction.LOCK) "锁车" else "解锁" + "请求…")
        // Repository verifies the saved PIN again before remote control.
        // The voice-specific path forbids combined LOCK + SHUTDOWN.
        controller.sendVoiceDoor(vin, command)
    }

    override fun onDestroy() {
        cancelled = true
        authenticatorCancellation?.cancel()
        if (::controller.isInitialized) controller.remove(observer)
        super.onDestroy()
    }
}
