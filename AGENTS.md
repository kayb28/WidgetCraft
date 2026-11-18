# AGENTS.md — WidgetCraft

Guía para agentes (y humanos) que trabajen en este repo. Lecciones heredadas del proyecto MihonFix: aquí se aplican desde el día 1 para no repetir sus problemas.

## 1. Regla crítica: firma estable

- Todas las builds release se firman en CI con el keystore estable (`CN=WidgetCraft`, huella SHA-256 `7A:AF:B2:70:F7:51:AD:13:0A:3D:56:5C:47:63:E3:9B:79:F8:37:98:D9:2F:F4:D0:75:A1:B6:18:84:F4:86:50`), montado desde los secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Si falta `KEYSTORE_BASE64`, la CI **falla a propósito** (fail fast). Un release sin firmar produce APKs que «no se instalan encima» y es exactamente el bug que hundió la experiencia de MihonFix durante días.
- Nunca generar keystore efímero en runners. Nunca commitear el keystore. La copia de la identidad de firma la conserva el usuario en su carpeta de descargas.

## 2. Versiones

- Cada versión que se entregue al usuario sube `versionCode` en `app/build.gradle.kts` (1, 2, 3…) y usa `versionName` semántico.
- Publicar versión = push de tag `v<versión>` → la CI crea GitHub Release con los APK.
- El registro de qué cambió cada versión va en `docs/REGISTRO.md` (cronológico, una entrada por tarea, con commit, motivo, decisiones y estado).

## 3. Arquitectura de referencia (no romperla sin actualizar este documento)

- **Un solo AppWidgetProvider** (`widget/WidgetProvider.kt`): cualquier widget instalado apunta a una configuración guardada en `WidgetStore` (SharedPreferences) y se pinta con `WidgetRenderer`.
- **WidgetRenderer** es la única fuente de verdad del aspecto: el preview del editor y el widget real usan la misma función (`widgetId = -1` para preview). Cambiar el aspecto de un diseño = cambiar su renderer + su layout `wg_<diseño>.xml`.
- **Estilo global** (fondo, opacidad, radio, texto, acento, escala, negrita) y **propiedades por diseño** viven en el JSON de configuración; el esquema está en `Designs` (`core/WidgetData.kt`). Añadir un diseño = entrada en `Designs.all` + layout `wg_<id>.xml` + rama en `WidgetRenderer.render`.
- **Colores automáticos**: modo del sistema (claro/oscuro) + contraste garantizado por luminancia (`StyleEngine`). Los fondos redondeados se pintan como bitmap porque RemoteViews no acepta drawables arbitrarios; el bitmap se re-renderiza en `onAppWidgetOptionsChanged` (redimensionado).
- **Acciones de toque**: PendingIntents con `FLAG_IMMUTABLE` siempre; `requestCode` = `1000 + widgetId*8 + n` (ver `WidgetProvider.Companion`).

## 4. Límites de plataformado

- minSdk 29 (Android 10), target/compileSdk 35. `targetCellWidth/Height` y `android:description` solo aplican en API 31+ (degradan bien).
- `updatePeriodMillis` mínimo 30 min (Android); los relojes usan `TextClock` (se actualizan solos), batería/dispositivo se refrescan al tocar el widget y en cada update.
- RemoteViews solo acepta vistas concretas (TextView, TextClock, ImageView, ProgressBar, LinearLayout, FrameLayout, RelativeLayout…): nada de CardView, Compose o vistas custom en los widgets.

## 5. Tests y CI

- `./gradlew :app:assembleRelease :app:testReleaseUnitTest` es la barrera: los tests de `app/src/test` cubren la lógica pura (tiempos, esquema de diseños, formatos).
- La CI corre en push a `main`, PRs y tags `v*` (`.github/workflows/build.yml`). Antes de dar por terminada una tarea, la build correspondiente debe estar verde y verificada por su log, no por fe.

## 6. Al reportar problemas de instalación

Seguir el mismo protocolo que en MihonFix: descartar firma con evidencia (logs de CI + `verify_apk_signature.py` del repo MihonFix si aplica) antes de asumir nada; revisar archivo correcto (ABI), Play Protect, instalador del fabricante y copias en doble app/espacio secundario. El certificado de referencia es el que está en la sección 1.
