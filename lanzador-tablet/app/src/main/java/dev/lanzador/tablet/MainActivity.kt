package dev.lanzador.tablet

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import dev.lanzador.tablet.data.PhotoSource
import dev.lanzador.tablet.data.WallpaperMode
import dev.lanzador.tablet.ui.LauncherRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private val calendarPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshCalendar()
    }

    private val photosPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshPhotos(force = true)
    }

    private val notificationsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) saveWallpaper(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Android pone un velo detrás de los botones para que se vean; el launcher no lo necesita.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        setContent { LauncherRoot(viewModel) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pulsar «Inicio» estando ya en el launcher cierra el cajón y los paneles.
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) viewModel.onHomePressed()
    }

    fun requestCalendar() = calendarPermission.launch(Manifest.permission.READ_CALENDAR)

    fun requestPhotos() = photosPermission.launch(PhotoSource.permissions)

    /** El aviso del temporizador necesita permiso de notificaciones (Android 13+). Se pide una vez. */
    fun requestNotificationsOnce() {
        if (Build.VERSION.SDK_INT < 33) return
        val sp = getSharedPreferences("lanzador_permisos", MODE_PRIVATE)
        if (sp.getBoolean("post_notifications_asked", false)) return
        sp.edit { putBoolean("post_notifications_asked", true) }
        notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun pickWallpaper() = pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    /** Copia la foto elegida dentro de la app (el permiso del selector es temporal). */
    private fun saveWallpaper(uri: Uri) {
        lifecycleScope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    filesDir.listFiles { f -> f.name.startsWith("fondo_") }?.forEach { it.delete() }
                    val file = File(filesDir, "fondo_${System.currentTimeMillis()}.jpg")
                    contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                    file.absolutePath
                }.getOrNull()
            }
            if (path == null) {
                Toast.makeText(this@MainActivity, "No se pudo usar esa foto", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.prefs.updateSettings { it.copy(wallpaperMode = WallpaperMode.PHOTO, wallpaperPhoto = path) }
            }
        }
    }
}
