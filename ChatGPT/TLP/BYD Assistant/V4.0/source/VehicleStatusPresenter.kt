package dev.vehiclebridge.byd.bridge

import dev.vehiclebridge.byd.protocol.VehicleTelemetry
import java.text.DateFormat
import java.util.Date

/** Display reported values only. Never invent an anti-theft or door-lock state. */
internal object VehicleStatusPresenter {
    private val sectionKeys = listOf(
        "总里程", "EV里程", "HEV里程", "电量", "纯电续航",
        "最近能耗", "近 50 km 能耗", "累计能耗",
        "低压供电系统", "动力电池", "动力系统",
        "整车状态", "动力模式", "OK指示灯", "行驶状态",
        "车速", "功率", "发动机冷却液温度"
    )
    fun show(report: VehicleTelemetry?): String {
        val supplied = report?.rows?.toMap().orEmpty()
        return buildString {
            append("车辆健康 / 车辆数据\n")
            for (key in sectionKeys) append(key).append("    ")
                .append(supplied[key] ?: "未提供").append('\n')
            append("\n更新时间：")
            append(report?.reportedAt?.let {DateFormat.getDateTimeInstance().format(Date(it))} ?: "未提供")
            append("\n提示：云端状态可能延迟；没有独立锁状态时，不判断车辆已上锁。")
        }
    }
}
