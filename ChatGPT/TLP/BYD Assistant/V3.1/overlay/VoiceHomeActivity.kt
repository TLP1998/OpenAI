package dev.vehiclebridge.byd

import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import dev.vehiclebridge.byd.protocol.DoorAction

/**
 * V3.1: one APK, exactly one LAUNCHER icon.
 * In-app configurable phrases are NOT XiaoAI shortcut/skill registrations.
 * Speech-to-text uses installed Android speech provider (may vary on HyperOS).
 */
class VoiceHomeActivity : Activity() {
    companion object {
        private const val REQUEST_SPEECH = 311
        private const val STORE = "xiaodi_voice_phrases"
        private const val KEY_AUTO = "auto_recognition"
    }
    private lateinit var controller: AuthController
    private lateinit var status: TextView
    private lateinit var heard: TextView
    private lateinit var auto: CheckBox
    private lateinit var phrases: Map<VoiceTask, EditText>
    private val scheduler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences(STORE, MODE_PRIVATE) }
    private var pendingTask: VoiceTask? = null
    private var executing = false
    private var launchedRecognition = false
    private var autoStarted = false
    private var disposed = false
    private val observer: (AuthState) -> Unit = { onControllerState(it) }

    @Suppress("DEPRECATION")
    private fun allowLockscreenEntry() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }

    private fun label(text: String, size: Float = 16f, color: Int = Color.DKGRAY) =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allowLockscreenEntry()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 56, 36, 50)
            setBackgroundColor(Color.rgb(246, 248, 252))
        }
        container.addView(label("小笛车控助手", 28f, Color.rgb(17, 51, 88)))
        container.addView(label("V3.1 ｜ 一个 App · 一个图标 · 自定义语音口令", 13f))
        container.addView(label("\n小爱可先打开本 App，随后由手机的语音识别服务接收口令。修改下面的口令不会直接修改超级小爱的唤醒词或快捷指令。\n", 14f))

        status = label("正在恢复车辆会话…", 17f)
        container.addView(status)
        heard = label("\n最近识别：暂无\n", 14f)
        container.addView(heard)

        container.addView(Button(this).apply {
            text = "开始说指令"
            setOnClickListener { startVoiceRecognition() }
        })
        auto = CheckBox(this).apply {
            text = "打开 App 时自动进入语音识别"
            isChecked = prefs.getBoolean(KEY_AUTO, false)
            setOnCheckedChangeListener { _, enabled ->
                prefs.edit().putBoolean(KEY_AUTO, enabled).apply()
                if (enabled) Toast.makeText(this@VoiceHomeActivity,
                    "已启用；下次打开 App 后尝试自动开始语音识别", Toast.LENGTH_LONG).show()
            }
        }
        container.addView(auto)

        container.addView(label("\n语音口令设置", 21f, Color.rgb(17, 51, 88)))
        val fields = linkedMapOf<VoiceTask, EditText>()
        for ((task, name) in linkedMapOf(
            VoiceTask.LOCK to "仅上锁",
            VoiceTask.UNLOCK to "解锁",
            VoiceTask.LOCK_CLOSEAIR to "上锁并关闭空调"
        )) {
            container.addView(label(name, 14f))
            val field = EditText(this).apply {
                isSingleLine = true
                textSize = 17f
                setText(prefs.getString(task.name, VoicePhraseMatcher.defaults[task])!!)
                hint = "输入自定义口令"
                setSelectAllOnFocus(true)
            }
            container.addView(field)
            fields[task] = field
        }
        phrases = fields
        container.addView(Button(this).apply {
            text = "保存三条语音口令"
            setOnClickListener { savePhrases() }
        })
        container.addView(Button(this).apply {
            text = "打开车辆管理与手动车控"
            setOnClickListener {
                val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
                if (keyguard.isKeyguardLocked) {
                    status.text = "手机仍处于锁屏状态，请先解锁手机再进入账号与 PIN 设置。"
                } else {
                    startActivity(Intent(this@VoiceHomeActivity, MainActivity::class.java))
                }
            }
        })
        container.addView(label("\n安全与兼容说明：本版无每次操作的二次认证；手机锁屏时如果 HyperOS 允许语音入口执行，附近任何人也可能触发解锁。语音识别依赖手机已安装的系统语音服务，可能需要联网或系统弹窗。没有 45 秒冷却，但车控不并发、不自动重试。组合命令中的“熄火”实为上游 CLOSEAIR 关闭空调请求。", 13f))

        val scroll = ScrollView(this)
        scroll.addView(container)
        setContentView(scroll)

        controller = AuthController.get(this)
        controller.initialize()
        controller.observe(observer)

        if (savedInstanceState != null) {
            // Never re-dispatch a vehicle command or auto-open the microphone on rotation.
            autoStarted = true
            status.text = "页面已重新创建，不会自动重复车辆请求。"
        } else if (prefs.getBoolean(KEY_AUTO, false)) {
            autoStarted = true
            scheduler.postDelayed({
                if (!disposed && !isFinishing && !launchedRecognition) startVoiceRecognition()
            }, 1300L)
        }
    }

    private fun loadedPhrases(): Map<VoiceTask, String> = VoiceTask.entries.associateWith {
        prefs.getString(it.name, VoicePhraseMatcher.defaults[it])!!
    }

    private fun savePhrases() {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (keyguard.isKeyguardLocked) {
            status.text = "修改口令前请先解锁手机。"
            return
        }
        val entries = phrases.mapValues { (_, edit) -> edit.text.toString().trim() }
        if (!VoicePhraseMatcher.distinctValid(entries)) {
            status.text = "三条口令必须各不相同，且每条长度为 2～24 个文字或数字（忽略空格和标点）。"
            return
        }
        val editor = prefs.edit()
        for ((task, value) in entries) editor.putString(task.name, value)
        editor.apply()
        status.text = "口令已保存在本机。下次语音识别按新口令执行；超级小爱的入口名称不会因此改变。"
    }

    private fun startVoiceRecognition() {
        if (launchedRecognition || executing || disposed) {
            status.text = "当前已有语音识别或车控请求正在执行。"
            return
        }
        val recognition = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PROMPT,
                "请说：" + loadedPhrases().values.joinToString(" / "))
        }
        try {
            launchedRecognition = true
            @Suppress("DEPRECATION")
            startActivityForResult(recognition, REQUEST_SPEECH)
            status.text = "等待系统语音识别结果…"
        } catch (_: ActivityNotFoundException) {
            launchedRecognition = false
            status.text = "没有找到支持中文语音识别的系统应用。请检查小米语音服务或相关应用设置。"
        } catch (_: SecurityException) {
            launchedRecognition = false
            status.text = "系统禁止启动语音识别，请检查 HyperOS 的应用权限。"
        }
    }

    @Deprecated("Speech RecognizerIntent compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SPEECH) return
        launchedRecognition = false
        if (resultCode != RESULT_OK || data == null) {
            status.text = "语音识别已取消或没有返回文字；没有发送任何车控请求。"
            return
        }
        val alternatives = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        heard.text = "\n最近识别：" + (alternatives.firstOrNull() ?: "无文字") + "\n"
        val settings = loadedPhrases()
        // First N-best recognition result that exactly matches a single saved phrase.
        val matched = alternatives.firstNotNullOfOrNull { VoicePhraseMatcher.match(it, settings) }
        if (matched == null) {
            status.text = "没有匹配到已保存的口令，没有发送车控命令。可修改口令使其与识别文字一致。"
            return
        }
        requestVehicleOperation(matched)
    }

    private fun requestVehicleOperation(task: VoiceTask) {
        if (pendingTask != null || executing) {
            status.text = "上次指令尚未完成，不重复发送。"
            return
        }
        pendingTask = task
        tryDispatch()
    }

    private fun onControllerState(state: AuthState) {
        if (disposed || isFinishing) return
        if (executing) {
            if (!state.busy) executing = false
            status.text = state.message + "\n\n操作结果仅代表服务器对本次指令的反馈。"
            return
        }
        if (pendingTask != null) {
            tryDispatch()
        } else if (state.busy) {
            status.text = "正在恢复比亚迪登录状态…"
        } else if (state.info?.selected != null) {
            status.text = "车辆会话已就绪。点击“开始说指令”，或启用自动语音识别。"
        } else {
            status.text = "请先进入车辆管理，登录、选择车辆并保存 6 位操作 PIN。"
        }
    }

    private fun tryDispatch() {
        if (executing || disposed) return
        val task = pendingTask ?: return
        val state = controller.state
        if (state.busy) {
            status.text = "正在恢复车辆会话，识别到的指令已暂存，准备执行…"
            return
        }
        pendingTask = null
        val vehicle = state.info?.selected
        if (vehicle == null || state.info?.hasPin != true) {
            status.text = "尚未完成账号登录、车辆选择或操作 PIN 设置；此次没有发送车辆命令。"
            return
        }
        executing = true
        val action = if (task == VoiceTask.UNLOCK) DoorAction.UNLOCK else DoorAction.LOCK
        val closeAir = task == VoiceTask.LOCK_CLOSEAIR
        status.text = when (task) {
            VoiceTask.LOCK -> "识别到锁车口令，正在发送上锁请求…"
            VoiceTask.UNLOCK -> "识别到解锁口令，正在发送解锁请求…"
            VoiceTask.LOCK_CLOSEAIR -> "识别到组合口令，正在锁车，随后可能发送 CLOSEAIR…"
        }
        controller.sendVoiceDoor(vehicle.vin, action, closeAir)
    }

    override fun onDestroy() {
        disposed = true
        scheduler.removeCallbacksAndMessages(null)
        if (::controller.isInitialized) controller.remove(observer)
        super.onDestroy()
    }
}
