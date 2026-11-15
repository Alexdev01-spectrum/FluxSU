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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
// Terminal prompt header (Material)
// ---------------------------------------------------------------------------------------------

/** A fake shell prompt with a blinking cursor and a one-line kernel status. */
@Composable
fun TerminalPromptMaterial(state: HomeUiState, modifier: Modifier = Modifier) {
    val cursorAlpha by rememberInfiniteTransition(label = "prompt_cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "prompt_cursor_alpha",
    )
    val active = state.ksuVersion != null
    val primary = MaterialTheme.colorScheme.primary
    val statusColor = if (active) primary else MaterialTheme.colorScheme.error

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
                text = "./status",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            MdText(
                text = "\u2588",
                style = MaterialTheme.typography.bodyMedium,
                color = primary.copy(alpha = cursorAlpha),
            )
        }
        MdText(
            text = if (active) "[ OK ] kernel module active" else "[FAIL] kernel module not loaded",
            style = MaterialTheme.typography.bodySmall,
            color = statusColor,
        )
    }
}
