package com.sukisu.ultra.ui.screen.home

import android.system.OsConstants
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.sukisu.ultra.ui.theme.FluxAccent
import com.sukisu.ultra.ui.theme.FluxAccentStore
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt
import com.sukisu.ultra.R
import com.sukisu.ultra.data.repository.SettingsRepository
import com.sukisu.ultra.data.repository.SettingsRepositoryImpl
import com.sukisu.ultra.ksuApp
import com.sukisu.ultra.ui.component.material.SegmentedColumn
import com.sukisu.ultra.ui.component.material.SegmentedSwitchItem
import com.sukisu.ultra.ui.component.miuix.AppCard
import com.sukisu.ultra.ui.theme.backgroundAwareCardColors
import com.sukisu.ultra.ui.theme.backgroundAwareColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.Text as MdText
import top.yukonga.miuix.kmp.basic.Text as MxText

/**
 * FluxSU home dashboard additions: a status info row (mode / SELinux / kernel)
 * and a block of quick toggles. Both UI modes share [QuickToggleState].
 *
 * Quick toggles reuse the same repository calls as the Settings screen, and a toggle is only
 * offered when the kernel reports the feature as "supported" (not forced, managed or missing).
 * The su toggle is deliberately temporary (never persisted), so it cannot lock you out of
 * root across reboots; use Settings for a permanent change.
 */
@Stable
class QuickToggleState(
    private val scope: CoroutineScope,
    private val repo: SettingsRepository,
) {
    var loaded by mutableStateOf(false)
        private set

    var suSupported by mutableStateOf(false)
        private set
    var suEnabled by mutableStateOf(false)
        private set

    var umountSupported by mutableStateOf(false)
        private set
    var umountEnabled by mutableStateOf(false)
        private set

    var mountHideSupported by mutableStateOf(false)
        private set
    var mountHideEnabled by mutableStateOf(false)
        private set

    val hasAny: Boolean
        get() = suSupported || umountSupported || mountHideSupported

    suspend fun load() = withContext(Dispatchers.IO) {
        runCatching {
            suSupported = repo.getSuCompatStatus() == "supported"
            suEnabled = repo.isSuEnabled()
            umountSupported = repo.getKernelUmountStatus() == "supported"
            umountEnabled = repo.isKernelUmountEnabled()
            mountHideSupported = repo.getMountHideStatus() == "supported"
            mountHideEnabled = repo.isMountHideEnabled()
        }
        loaded = true
    }

    fun setSu(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (repo.setSuEnabled(enabled)) suEnabled = enabled else toastFailed()
        }
    }

    fun setUmount(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (repo.setKernelUmountEnabled(enabled)) {
                repo.execKsudFeatureSave()
                umountEnabled = enabled
            } else {
                toastFailed()
            }
        }
    }

    fun setMountHide(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            val status = repo.setMountHideEnabled(enabled)
            repo.execKsudFeatureSave()
            mountHideEnabled = enabled
            when (status) {
                0 -> {}
                -OsConstants.EAGAIN -> toast(ksuApp.getString(R.string.settings_mount_hide_reboot_required))
                else -> toast(ksuApp.getString(R.string.settings_mount_hide_failed, status))
            }
        }
    }

    private suspend fun toastFailed() = toast(ksuApp.getString(R.string.flux_quick_failed))

    private suspend fun toast(message: String) = withContext(Dispatchers.Main) {
        Toast.makeText(ksuApp, message, Toast.LENGTH_LONG).show()
    }
}

@Composable
fun rememberQuickToggleState(): QuickToggleState {
    val scope = rememberCoroutineScope()
    val state = remember { QuickToggleState(scope, SettingsRepositoryImpl()) }
    LaunchedEffect(state) { state.load() }
    return state
}

/** Quick toggles and the info row only make sense with an active kernel and a real manager. */
val HomeUiState.showFluxDashboard: Boolean
    get() = isManager && ksuVersion != null && !isPreviewMode

// ---------------------------------------------------------------------------------------------
// Material
// ---------------------------------------------------------------------------------------------

@Composable
fun FluxInfoRowMaterial(state: HomeUiState, modifier: Modifier = Modifier) {
    val selinux = state.systemInfo.selinuxStatus.ifBlank { "—" }
    val selinuxAccent = when (selinux) {
        "Enforcing" -> MaterialTheme.colorScheme.primary
        "Permissive" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoTileMaterial(
            label = stringResource(R.string.flux_dashboard_mode),
            value = state.workingModeLabel.ifBlank { "—" },
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        InfoTileMaterial(
            label = stringResource(R.string.flux_dashboard_selinux),
            value = selinux,
            accent = selinuxAccent,
            modifier = Modifier.weight(1f),
        )
        InfoTileMaterial(
            label = stringResource(R.string.flux_dashboard_kernel),
            value = state.systemInfo.kernelVersion.ifBlank { "—" },
            accent = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InfoTileMaterial(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
        color = backgroundAwareColor(MaterialTheme.colorScheme.surfaceBright),
        border = BorderStroke(1.dp, fluxGlowBrush()),
    ) {
        Column(
            modifier = Modifier
                .height(72.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(accent, CircleShape))
                MdText(
                    text = label,
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            MdText(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun QuickTogglesMaterial(state: QuickToggleState, modifier: Modifier = Modifier) {
    if (!state.loaded || !state.hasAny) return

    val suTitle = stringResource(R.string.flux_quick_su_title)
    val suSummary = stringResource(R.string.flux_quick_su_summary)
    val umountTitle = stringResource(R.string.flux_quick_umount_title)
    val umountSummary = stringResource(R.string.flux_quick_umount_summary)
    val hideTitle = stringResource(R.string.flux_quick_mounthide_title)
    val hideSummary = stringResource(R.string.flux_quick_mounthide_summary)

    val items = buildList<@Composable () -> Unit> {
        if (state.suSupported) add {
            SegmentedSwitchItem(
                title = suTitle,
                summary = suSummary,
                checked = state.suEnabled,
                onCheckedChange = state::setSu,
            )
        }
        if (state.umountSupported) add {
            SegmentedSwitchItem(
                title = umountTitle,
                summary = umountSummary,
                checked = state.umountEnabled,
                onCheckedChange = state::setUmount,
            )
        }
        if (state.mountHideSupported) add {
            SegmentedSwitchItem(
                title = hideTitle,
                summary = hideSummary,
                checked = state.mountHideEnabled,
                onCheckedChange = state::setMountHide,
            )
        }
    }
    SegmentedColumn(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.flux_quick_toggles),
        content = items,
    )
}

// ---------------------------------------------------------------------------------------------
// Miuix
// ---------------------------------------------------------------------------------------------

@Composable
fun FluxInfoRowMiuix(state: HomeUiState, modifier: Modifier = Modifier) {
    val selinux = state.systemInfo.selinuxStatus.ifBlank { "—" }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoTileMiuix(
            label = stringResource(R.string.flux_dashboard_mode),
            value = state.workingModeLabel.ifBlank { "—" },
            modifier = Modifier.weight(1f),
        )
        InfoTileMiuix(
            label = stringResource(R.string.flux_dashboard_selinux),
            value = selinux,
            modifier = Modifier.weight(1f),
        )
        InfoTileMiuix(
            label = stringResource(R.string.flux_dashboard_kernel),
            value = state.systemInfo.kernelVersion.ifBlank { "—" },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InfoTileMiuix(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier, colors = backgroundAwareCardColors()) {
        Column(
            modifier = Modifier
                .height(72.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MxText(
                text = label,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
            )
            MxText(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun QuickTogglesMiuix(state: QuickToggleState, modifier: Modifier = Modifier) {
    if (!state.loaded || !state.hasAny) return

    AppCard(modifier = modifier.fillMaxWidth(), colors = backgroundAwareCardColors()) {
        if (state.suSupported) {
            SwitchPreference(
                title = stringResource(R.string.flux_quick_su_title),
                summary = stringResource(R.string.flux_quick_su_summary),
                checked = state.suEnabled,
                onCheckedChange = state::setSu,
            )
        }
        if (state.umountSupported) {
            SwitchPreference(
                title = stringResource(R.string.flux_quick_umount_title),
                summary = stringResource(R.string.flux_quick_umount_summary),
                checked = state.umountEnabled,
                onCheckedChange = state::setUmount,
            )
        }
        if (state.mountHideSupported) {
            SwitchPreference(
                title = stringResource(R.string.flux_quick_mounthide_title),
                summary = stringResource(R.string.flux_quick_mounthide_summary),
                checked = state.mountHideEnabled,
                onCheckedChange = state::setMountHide,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Terminal prompt header, accent picker, system monitor
// ---------------------------------------------------------------------------------------------

@Composable
private fun fluxGlowBrush(): Brush {
    val c = MaterialTheme.colorScheme
    return Brush.linearGradient(listOf(c.primary.copy(alpha = 0.85f), c.secondary.copy(alpha = 0.35f)))
}

@Composable
private fun rememberTypewriter(text: String, charDelayMs: Long = 32L, startDelayMs: Long = 0L): String {
    var shown by remember(text) { mutableStateOf("") }
    LaunchedEffect(text) {
        delay(startDelayMs)
        for (i in 1..text.length) {
            shown = text.substring(0, i)
            delay(charDelayMs)
        }
    }
    return shown
}

/** A row of small swatches that switch the app's accent colour. */
@Composable
fun FluxAccentPicker(selectedBorder: Color, modifier: Modifier = Modifier) {
    val current = FluxAccentStore.accent
    val shape = CutCornerShape(topEnd = 5.dp, bottomStart = 5.dp)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FluxAccent.values().forEach { option ->
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(option.dark, shape)
                    .then(if (option == current) Modifier.border(2.dp, selectedBorder, shape) else Modifier)
                    .clickable { FluxAccentStore.set(option) },
            )
        }
    }
}

/** A fake shell prompt: the command types itself out, then a pulsing kernel status line appears. */
@Composable
fun TerminalPromptMaterial(state: HomeUiState, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "prompt")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "prompt_cursor_alpha",
    )
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "prompt_pulse",
    )
    val active = state.ksuVersion != null
    val primary = MaterialTheme.colorScheme.primary
    val statusColor = if (active) primary else MaterialTheme.colorScheme.error

    val command = "./status"
    val typedCommand = rememberTypewriter(command, startDelayMs = 150L)
    val commandDone = typedCommand.length == command.length
    val result = if (active) "kernel module active" else "kernel module not loaded"
    val typedResult = rememberTypewriter(if (commandDone) result else "", charDelayMs = 14L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MdText(
                text = "flux@su:~\$ ",
                style = MaterialTheme.typography.bodyMedium,
                color = primary,
                fontWeight = FontWeight.Bold,
            )
            MdText(
                text = typedCommand,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            MdText(
                text = "\u2588",
                style = MaterialTheme.typography.bodyMedium,
                color = primary.copy(alpha = cursorAlpha),
            )
            Spacer(Modifier.weight(1f))
            FluxAccentPicker(selectedBorder = MaterialTheme.colorScheme.onSurface)
        }
        if (commandDone) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MdText(
                    text = "\u25CF ",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor.copy(alpha = if (active) pulse else 1f),
                )
                MdText(
                    text = (if (active) "[ OK ] " else "[FAIL] ") + typedResult,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class MonitorSnapshot(
    val ramUsed: Long,
    val ramTotal: Long,
    val diskUsed: Long,
    val diskTotal: Long,
    val uptimeMs: Long,
)

private fun readMonitor(context: Context): MonitorSnapshot {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    val stat = StatFs(Environment.getDataDirectory().path)
    val total = stat.blockCountLong * stat.blockSizeLong
    val free = stat.availableBlocksLong * stat.blockSizeLong
    return MonitorSnapshot(
        ramUsed = info.totalMem - info.availMem,
        ramTotal = info.totalMem,
        diskUsed = total - free,
        diskTotal = total,
        uptimeMs = SystemClock.elapsedRealtime(),
    )
}

@Composable
private fun rememberMonitor(): MonitorSnapshot? {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf<MonitorSnapshot?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            snapshot = withContext(Dispatchers.IO) { runCatching { readMonitor(context) }.getOrNull() }
            delay(2000L)
        }
    }
    return snapshot
}

private fun asciiBar(fraction: Float, width: Int = 14): String {
    val filled = (fraction.coerceIn(0f, 1f) * width).roundToInt()
    return "\u2588".repeat(filled) + "\u2591".repeat(width - filled)
}

private fun gb(bytes: Long): String = String.format(Locale.US, "%.1f", bytes / 1_073_741_824.0)

private fun formatUptime(ms: Long): String {
    val totalMinutes = ms / 60_000L
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60
    return if (days > 0) "${days}d ${hours}h ${minutes}m" else "${hours}h ${minutes}m"
}

@Composable
private fun MonitorRow(
    name: String,
    used: Long,
    total: Long,
    style: TextStyle,
    accent: Color,
    label: Color,
    value: Color,
) {
    val fraction = if (total > 0L) used.toFloat() / total.toFloat() else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(text = "$name ", style = style.copy(color = label))
        BasicText(text = asciiBar(fraction), style = style.copy(color = accent))
        BasicText(
            text = " ${(fraction * 100).roundToInt()}%  ${gb(used)}/${gb(total)}G",
            style = style.copy(color = value),
        )
    }
}

@Composable
private fun MonitorBody(
    snapshot: MonitorSnapshot,
    accent: Color,
    label: Color,
    value: Color,
    modifier: Modifier = Modifier,
) {
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(text = "// system monitor", style = mono.copy(color = accent, fontWeight = FontWeight.Bold))
        MonitorRow("ram ", snapshot.ramUsed, snapshot.ramTotal, mono, accent, label, value)
        MonitorRow("disk", snapshot.diskUsed, snapshot.diskTotal, mono, accent, label, value)
        BasicText(text = "up   ${formatUptime(snapshot.uptimeMs)}", style = mono.copy(color = value))
    }
}

@Composable
fun SystemMonitorMaterial(modifier: Modifier = Modifier) {
    val snapshot = rememberMonitor() ?: return
    val c = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
        color = backgroundAwareColor(c.surfaceBright),
        border = BorderStroke(1.dp, fluxGlowBrush()),
    ) {
        MonitorBody(
            snapshot = snapshot,
            accent = c.primary,
            label = c.onSurfaceVariant,
            value = c.onSurface,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
fun SystemMonitorMiuix(modifier: Modifier = Modifier) {
    val snapshot = rememberMonitor() ?: return
    AppCard(modifier = modifier.fillMaxWidth(), colors = backgroundAwareCardColors()) {
        MonitorBody(
            snapshot = snapshot,
            accent = MiuixTheme.colorScheme.primary,
            label = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            value = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(14.dp),
        )
    }
}
