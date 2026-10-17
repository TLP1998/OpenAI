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
import android.widget.TextView
import dev.vehiclebridge.byd.protocol.DoorAction

/**
 * Voice-only component with signature-protected launch.
 * Launch while locked is subject to HyperOS and XiaoAI limitations.
 * No artificial cooldown; duplicates in flight are still blocked by AuthController.
 */
class VoiceCommandActivity : Activity() {
    companion object {
        const val ACTION_LOCK = "dev.vehiclebridge.byd.xiaoai.LOCK"
        const val ACTION_UNLOCK = "dev.vehiclebridge.byd.xiaoai.UNLOCK"
        const val ACTION_LOCK_AND_SHUTDOWN = "dev.vehiclebridge.byd.xiaoai.LOCK_SHUTDOWN"
    }
    private lateinit var controller: AuthController
    private lateinit var status: TextView
    private var action: DoorAction? = null
    private var shutdownAfterLock = false
    private var attempted = false
    private var executing = false
    private var disposed = false
    private val observer: (AuthState)->Unit = { updateState(it) }

    @Suppress("DEPRECATION")
    private fun allowLockscreenDisplay() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        allowLockscreenDisplay()
        when (intent?.action) {
            ACTION_LOCK -> action=DoorAction.LOCK
            ACTION_UNLOCK -> action=DoorAction.UNLOCK
            ACTION_LOCK_AND_SHUTDOWN -> {
                action=DoorAction.LOCK
                shutdownAfterLock=true
            }
        }
        val title = when(intent?.action) {
            ACTION_LOCK -> "小破车上锁"
            ACTION_UNLOCK -> "小破车解锁"
            ACTION_LOCK_AND_SHUTDOWN -> "小破车上锁并熄火"
            else -> "未知操作"
        }
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(40,80,40,48)
            setBackgroundColor(Color.rgb(246,248,252))
        }
        root.addView(TextView(this).apply {
            text="小爱语音 · "+title
            textSize=26f
            setTextColor(Color.rgb(22,42,82))
        })
        status=TextView(this).apply {
            text="正在恢复车辆会话…"
            textSize=18f
            setPadding(0,32,0,28)
            setTextColor(Color.DKGRAY)
        }
        root.addView(status)
        // A lockscreen entry must not expose the logged-in main UI as a bypass.
        val keyguard=getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if(!keyguard.isKeyguardLocked) {
            root.addView(Button(this).apply {
                text="打开车控主 App（账号 / 车辆 / PIN）"
                setOnClickListener {
                    startActivity(Intent(this@VoiceCommandActivity,MainActivity::class.java).putExtra("tab",2))
                }
            })
        }
        root.addView(Button(this).apply {text="关闭";setOnClickListener{finish()}})
        root.addView(TextView(this).apply {
            text="\n未设置45秒冷却时间，但同一时刻不会并发发送车控请求；不自动重试。\n「上锁并熄火」沿用上游 CLOSEAIR 关闭空调指令，并非已验证的发动机熄火。\n操作反馈不代表读取了实时车锁状态。\n锁屏语音操作可能被系统限制。"
            textSize=13f
            setTextColor(Color.GRAY)
        })
        setContentView(android.widget.ScrollView(this).apply {addView(root)})
        if (action==null) {status.text="无效指令，未发送车控请求。";return}
        // Don't repeat requests after rotation or other Activity recreation.
        if(state!=null){
            attempted=true
            status.text="页面重新创建，为避免重复执行，本次没有再次发送指令。"
            return
        }
        controller=AuthController.get(this)
        controller.initialize()
        controller.observe(observer)
    }

    private fun updateState(state: AuthState) {
        if(disposed || isFinishing)return
        if(executing) {
            status.text=state.message+"\n\n请按车辆实际反馈确认结果。"
            return
        }
        if(attempted)return
        if(state.busy){status.text="正在恢复车辆会话，请稍候…";return}
        val vehicle=state.info?.selected
        if(vehicle==null){
            attempted=true
            status.text="尚未登录或未选择车辆，请先在主程序里完成设置。\n"+state.message
            return
        }
        if(state.info?.hasPin!=true){
            attempted=true
            status.text="未保存操作 PIN，请先在主程序里验证并保存。"
            return
        }
        attempted=true
        executing=true
        status.text=when(intent?.action){
            ACTION_UNLOCK -> "正在发送解锁请求…"
            ACTION_LOCK_AND_SHUTDOWN -> "正在发送锁车请求，确认后再发送关闭空调请求…"
            else -> "正在发送锁车请求…"
        }
        controller.sendVoiceDoor(vehicle.vin,action!!,shutdownAfterLock)
    }

    override fun onDestroy() {
        disposed=true
        if(::controller.isInitialized) controller.remove(observer)
        super.onDestroy()
    }
}
