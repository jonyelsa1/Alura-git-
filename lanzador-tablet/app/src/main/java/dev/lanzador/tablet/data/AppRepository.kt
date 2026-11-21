package dev.lanzador.tablet.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Lee las apps instaladas (también las del perfil de trabajo) con LauncherApps
 * y se mantiene al día cuando se instala, actualiza o desinstala algo.
 */
class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val iconCache = HashMap<String, ImageBitmap>()
    private var reloadJob: Job? = null

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            reload(*packageNames)

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            reload(*packageNames)
    }

    fun start() {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        reload()
    }

    fun stop() {
        runCatching { launcherApps.unregisterCallback(callback) }
        scope.cancel()
    }

    fun reload(vararg changedPackages: String) {
        reloadJob?.cancel()
        reloadJob = scope.launch {
            if (changedPackages.isNotEmpty()) {
                synchronized(iconCache) {
                    iconCache.keys.removeAll { key -> changedPackages.any { key.startsWith("$it/") } }
                }
            }
            _apps.value = loadApps()
            _loaded.value = true
        }
    }

    private suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val metrics = context.resources.displayMetrics
        val iconPx = (ICON_SOURCE_DP * metrics.density).roundToInt().coerceAtLeast(96)
        val me = Process.myUserHandle()
        val result = ArrayList<AppEntry>()
        for (user in userManager.userProfiles) {
            val serial = userManager.getSerialNumberForUser(user)
            val activities = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
            for (info in activities) {
                ensureActive()
                val component = info.componentName
                if (component.packageName == context.packageName) continue
                val key = "${component.flattenToShortString()}#$serial"
                val cached = synchronized(iconCache) { iconCache[key] }
                val icon = cached ?: run {
                    val drawable = runCatching { info.getBadgedIcon(metrics.densityDpi) }.getOrNull()
                        ?: context.packageManager.defaultActivityIcon
                    drawable.toBitmap(iconPx, iconPx).asImageBitmap().also {
                        synchronized(iconCache) { iconCache[key] = it }
                    }
                }
                result += AppEntry(
                    key = key,
                    label = info.label?.toString()?.trim().orEmpty().ifEmpty { component.packageName },
                    packageName = component.packageName,
                    component = component,
                    user = user,
                    icon = icon,
                    isSystem = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isWorkProfile = user != me,
                )
            }
        }
        val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
        result.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun launch(app: AppEntry): Boolean = runCatching {
        launcherApps.startMainActivity(app.component, app.user, null, null)
    }.isSuccess

    fun openAppInfo(app: AppEntry): Boolean = runCatching {
        launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
    }.isSuccess

    /** Accesos directos de la app (p. ej. «Nueva pestaña de incógnito»). Solo si somos el launcher activo. */
    fun shortcutsFor(app: AppEntry): List<ShortcutInfo> = runCatching {
        if (!launcherApps.hasShortcutHostPermission()) return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setActivity(app.component)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST,
            )
        launcherApps.getShortcuts(query, app.user).orEmpty()
            .filter { it.isEnabled }
            .sortedWith(compareBy<ShortcutInfo>({ !it.isDeclaredInManifest }, { it.rank }))
            .take(4)
    }.getOrDefault(emptyList())

    fun startShortcut(shortcut: ShortcutInfo): Boolean = runCatching {
        launcherApps.startShortcut(shortcut, null, null)
    }.isSuccess

    private companion object {
        const val ICON_SOURCE_DP = 96
    }
}
