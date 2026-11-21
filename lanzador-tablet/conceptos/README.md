# Conceptos de diseño para Lanzador

Maquetas estáticas a 1920×1200 (la pantalla de la T65Max) para elegir dirección antes de programar. Los datos son inventados (tiempo, agenda, música, fotos) y los iconos son pictogramas genéricos de [Lucide](https://lucide.dev), no los logotipos reales de cada app.

| Concepto | Imagen | En qué se inspira |
| --- | --- | --- |
| Cristal | `capturas/cristal.png`, `capturas/cristal-cajon.png` | Liquid Glass de Apple (iOS/iPadOS 26) y el tema Liquid Glass de Smart Launcher 6.6 |
| Expresivo | `capturas/expresivo.png` | Material 3 Expressive de Google (Android 16) |
| Bento | `capturas/bento.png` | Cuadrículas «bento» y glassmorphism 2.0 |
| Enfoque | `capturas/enfoque.png` | Nothing OS (reloj de puntos), Niagara (lista de texto), Kvaesitso (buscar escribiendo) |

## Qué haría falta para llevarlos a la app

- **Cristal y Bento.** Android no deja a un launcher desenfocar el fondo de pantalla del sistema de forma fiable. La vía segura es que el launcher dibuje el fondo él mismo (lo elige el usuario dentro de la app), lo desenfoque una vez al cambiarlo y recorte la zona de cada tarjeta. Como el fondo no se mueve, el coste en batería es bajo. Los widgets propios del launcher (tiempo, agenda, música, batería, temporizador) llevarían el cristal; los widgets de otras apps conservan su propio aspecto.
- **Expresivo.** Compose ya trae las formas de Material 3 Expressive (`androidx.graphics.shapes`). Es la más sencilla de construir con buen acabado.
- **Enfoque.** La más rápida de usar y de programar. Las notificaciones dentro de la lista necesitan que el usuario dé acceso a las notificaciones.
- La música necesita acceso a notificaciones (para leer lo que suena) y las fotos de «Recuerdo», permiso de fotos.

## Regenerar las imágenes

```bash
npm install
node render.mjs            # todas
node render.mjs bento      # solo una
```

Usa Chromium mediante Playwright.

## Fuentes consultadas

- Apple Newsroom, «Apple introduces a delightful and elegant new software design», 9 de junio de 2025: https://www.apple.com/newsroom/2025/06/apple-introduces-a-delightful-and-elegant-new-software-design/
- MacStories, reseña de iOS y iPadOS 26, capítulo de la pantalla de inicio, septiembre de 2025: https://www.macstories.net/stories/ios-and-ipados-26-the-macstories-review/4
- heise online, sobre el modo de iconos transparentes «Clear» de iOS 26, junio de 2025: https://heise.de/-10442076
- 9to5Google, «Android 16 Material 3 Expressive redesign», 13 de mayo de 2025: https://9to5google.com/2025/05/13/android-16-material-3-expressive-redesign/
- Smart Launcher, resumen de 2025 (tema Liquid Glass para widgets), 23 de diciembre de 2025: https://www.smartlauncher.net/blog/2025wrapupsl66
- Wavespace, «8 UI Design Trends in 2026»: https://www.wavespace.agency/blog/ui-design-trends
- Android Authority, mejores launchers de Android (Kvaesitso, Niagara): https://www.androidauthority.com/best-android-launchers-3580969/
- GSMArena, anuncio de Nothing OS 4.0, 2025: https://www.gsmarena.com/nothing_os_40_announced_with_refined_design_updated_dark_mode_and_quick_settings-news-69546.php
