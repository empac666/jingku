package com.broll.poc.metrics

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DeviceSnapshot(
    val batteryPct: Int?,
    val batteryChargeUah: Long?,
    val batteryTempC: Float,
    val thermalStatus: Int,
    val thermalName: String,
    val pssKb: Int?,
)

class DeviceProbe(private val context: Context) {
    fun snapshot(): DeviceSnapshot {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pctRaw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val pct = if (pctRaw == Int.MIN_VALUE) null else pctRaw
        val chargeRaw = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val chargeUah = if (chargeRaw == Long.MIN_VALUE || chargeRaw < 0) null else chargeRaw

        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tempTenth = sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val tempC = if (tempTenth == Int.MIN_VALUE) Float.NaN else tempTenth / 10f

        val (thermal, name) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val status = pm.currentThermalStatus
            status to thermalName(status)
        } else {
            -1 to "unsupported_api"
        }
        val pss = try {
            Debug.getPss().toInt()
        } catch (_: Throwable) {
            null
        }
        return DeviceSnapshot(
            batteryPct = pct,
            batteryChargeUah = chargeUah,
            batteryTempC = tempC,
            thermalStatus = thermal,
            thermalName = name,
            pssKb = pss,
        )
    }

    private fun thermalName(status: Int): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "unsupported_api"
        return when (status) {
            PowerManager.THERMAL_STATUS_NONE -> "none"
            PowerManager.THERMAL_STATUS_LIGHT -> "light"
            PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
            PowerManager.THERMAL_STATUS_SEVERE -> "severe"
            PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
            else -> "unknown_$status"
        }
    }
}

class MinuteMetricsAccumulator(private val probe: DeviceProbe) {
    var indexMs: Long = 0
    private var startCharge: Long? = null
    private var startPct: Int? = null
    private var maxThermal: Int = Int.MIN_VALUE
    private var maxPss: Int = Int.MIN_VALUE

    fun onWorkAtVideoSec(startSec: Double, elapsedMs: Long) {
        indexMs += elapsedMs
        val snap = probe.snapshot()
        if (startCharge == null) startCharge = snap.batteryChargeUah
        if (startPct == null) startPct = snap.batteryPct
        if (snap.thermalStatus > maxThermal) maxThermal = snap.thermalStatus
        snap.pssKb?.let { if (it > maxPss) maxPss = it }
    }

    fun deltaChargeUah(): Long? {
        val end = probe.snapshot().batteryChargeUah
        val start = startCharge
        return if (start != null && end != null) end - start else null
    }

    fun deltaPct(): Int? {
        val end = probe.snapshot().batteryPct
        val start = startPct
        return if (start != null && end != null) end - start else null
    }

    fun thermalPeak(): Int = if (maxThermal == Int.MIN_VALUE) -1 else maxThermal

    fun pssPeak(): Int? = if (maxPss == Int.MIN_VALUE) null else maxPss
}

class MetricsLogger(private val appContext: Context) {
    private val probe = DeviceProbe(appContext)
    val csvFile: File
    val csvMirror: File
    private val uiLines = ArrayDeque<String>()
    private var batteryBaselinePct: Int? = null
    private var batteryBaselineCharge: Long? = null
    var onUi: ((String) -> Unit)? = null

    init {
        val externalBase = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        val extDir = File(externalBase, "logs").apply { mkdirs() }
        csvFile = File(extDir, "broll_metrics.csv")
        val intDir = File(appContext.filesDir, "logs").apply { mkdirs() }
        csvMirror = File(intDir, "broll_metrics.csv")
        ensureHeader(csvFile)
        ensureHeader(csvMirror)
    }

    private fun ensureHeader(file: File) {
        if (!file.exists() || file.length() == 0L) {
            file.writeText(HEADER)
        }
    }

    fun markBatteryBaseline() {
        synchronized(this) {
            val snap = probe.snapshot()
            batteryBaselinePct = snap.batteryPct
            batteryBaselineCharge = snap.batteryChargeUah
        }
    }

    fun logSnapshot(event: String, stage: String, detail: String = "", embedderMode: String = "") {
        log(
            event = event,
            stage = stage,
            detail = detail,
            embedderMode = embedderMode,
            useProbeForRow = true,
        )
    }

    fun logMinuteSummary(
        videoDurationSec: Double,
        minuteIndex: Int,
        indexMsForMinute: Long,
        deltaChargeUah: Long?,
        deltaPct: Int?,
        thermalPeak: Int,
        pssPeak: Int?,
        embedderMode: String,
    ) {
        log(
            event = "index",
            stage = "minute_summary",
            elapsedMs = indexMsForMinute,
            videoDurationSec = videoDurationSec,
            videoMinuteIndex = minuteIndex,
            indexMsForMinute = indexMsForMinute,
            detail = buildString {
                append("video_minute=$minuteIndex index_ms=$indexMsForMinute ")
                append("battery_delta_uah=${deltaChargeUah ?: ""} battery_delta_pct=${deltaPct ?: ""} ")
                append("thermal_peak=$thermalPeak pss_peak_kb=${pssPeak ?: ""}")
            },
            embedderMode = embedderMode,
            overrideDeltaCharge = deltaChargeUah,
            overrideDeltaPct = deltaPct,
            overrideThermal = thermalPeak,
            overridePss = pssPeak,
        )
    }

    fun log(
        event: String,
        stage: String,
        elapsedMs: Long = 0,
        videoDurationSec: Double = 0.0,
        videoMinuteIndex: Int = -1,
        indexMsForMinute: Long = -1,
        detail: String = "",
        mirrorToUi: Boolean = true,
        embedderMode: String = "",
        useProbeForRow: Boolean = true,
        overrideDeltaCharge: Long? = null,
        overrideDeltaPct: Int? = null,
        overrideThermal: Int? = null,
        overridePss: Int? = null,
    ) {
        val text = synchronized(this) {
            val line = appendRow(
                event,
                stage,
                elapsedMs,
                videoDurationSec,
                videoMinuteIndex,
                indexMsForMinute,
                detail,
                embedderMode,
                useProbeForRow,
                overrideDeltaCharge,
                overrideDeltaPct,
                overrideThermal,
                overridePss,
            )
            if (!mirrorToUi) return@synchronized null
            uiLines.addLast(line)
            while (uiLines.size > 80) uiLines.removeFirst()
            uiLines.joinToString("\n")
        }
        if (text != null) onUi?.invoke(text)
    }

    private fun appendRow(
        event: String,
        stage: String,
        elapsedMs: Long,
        videoDurationSec: Double,
        videoMinuteIndex: Int,
        indexMsForMinute: Long,
        detail: String,
        embedderMode: String,
        useProbe: Boolean,
        overrideDeltaCharge: Long?,
        overrideDeltaPct: Int?,
        overrideThermal: Int?,
        overridePss: Int?,
    ): String {
        val snap = if (useProbe) probe.snapshot() else null
        val pct = snap?.batteryPct
        val charge = snap?.batteryChargeUah
        val baselinePct = batteryBaselinePct
        val baselineCharge = batteryBaselineCharge
        val deltaPct = overrideDeltaPct ?: run {
            if (baselinePct != null && pct != null) pct - baselinePct else null
        }
        val deltaCharge = overrideDeltaCharge ?: run {
            if (baselineCharge != null && charge != null) charge - baselineCharge else null
        }
        val thermal = overrideThermal ?: snap?.thermalStatus ?: -1
        val thermalName = if (overrideThermal != null) {
            thermalNameFromStatus(thermal)
        } else {
            snap?.thermalName ?: thermalNameFromStatus(thermal)
        }
        val pss = overridePss ?: snap?.pssKb
        val temp = snap?.batteryTempC ?: Float.NaN

        val ts = ISO.format(Date())
        val row = listOf(
            ts,
            event,
            stage,
            elapsedMs.toString(),
            formatSec(videoDurationSec),
            videoMinuteIndex.toString(),
            indexMsForMinute.toString(),
            pct?.toString() ?: "",
            deltaPct?.toString() ?: "",
            charge?.toString() ?: "",
            deltaCharge?.toString() ?: "",
            if (temp.isNaN()) "" else String.format(Locale.US, "%.1f", temp),
            thermal.toString(),
            thermalName,
            pss?.toString() ?: "",
            embedderMode,
            detail,
        ).joinToString(",") { csvEscape(it) }
        csvFile.appendText(row + "\n")
        csvMirror.appendText(row + "\n")
        val tempStr = if (temp.isNaN()) "" else String.format(Locale.US, "%.1f", temp)
        return "$ts $event/$stage ${elapsedMs}ms mode=$embedderMode pss=${pss ?: ""}KB bat=${pct ?: ""}% dµAh=${deltaCharge ?: ""} temp=$tempStr thermal=$thermalName $detail"
    }

    private fun thermalNameFromStatus(status: Int): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "unsupported_api"
        return when (status) {
            PowerManager.THERMAL_STATUS_NONE -> "none"
            PowerManager.THERMAL_STATUS_LIGHT -> "light"
            PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
            PowerManager.THERMAL_STATUS_SEVERE -> "severe"
            PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
            else -> "unknown_$status"
        }
    }

    fun uiText(): String = uiLines.joinToString("\n")

    private fun csvEscape(value: String): String {
        val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuote) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    companion object {
        private val ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        const val HEADER =
            "timestamp_iso,event,stage,elapsed_ms,video_duration_sec,video_minute_index,index_ms_for_minute," +
                "battery_pct,battery_delta_pct,battery_charge_uah,battery_delta_uah,battery_temp_c," +
                "thermal_status,thermal_status_name,pss_kb,embedder_mode,detail\n"

        private fun formatSec(v: Double): String = String.format(Locale.US, "%.3f", v)
    }
}
