# REGISTRO — WidgetCraft

Registro cronológico de decisiones y trabajos. Convención: una entrada por tarea, con hora UTC, commit, motivo, qué se hizo y estado.

## 2026-11-18

- **02:15 UTC — creación del proyecto y primera versión (`v1.0.0`).** Motivo: encargo del usuario — una app que no solo permita aplicar widgets sino crearlos, con lista base propia, widgets totalmente ajustables a la pantalla y adaptación a tema claro/oscuro. Decisiones confirmadas con el usuario antes de empezar: plataforma Android nativo (API 29+), nombre WidgetCraft, editor de profundidad «personalización» (propiedades por diseño + estilo global, no diseñador libre), 12 diseños base, repo público con APK generado por GitHub Actions. Hecho:
  - Proyecto Gradle completo (AGP 8.5.2, Kotlin 2.0.20, Gradle 8.9, JDK 21, compileSdk 35) con wrapper reutilizado del repo MihonFix.
  - Catálogo de 12 diseños: reloj digital (TextClock), fecha, batería, contador de toques, nota rápida, cita del día (24 frases propias + propias del usuario), cuenta atrás, progreso del tiempo (día/mes/año), control de tiempo, lista de tareas (4 filas con tachado), estado del dispositivo (RAM/almacenamiento/uptime) y accesos directos (hasta 4 apps). Ninguno requiere permisos especiales.
  - Modelo de configuración JSON (props por diseño + estilo global) y persistencia en SharedPreferences con mapeo widget instalado ↔ configuración.
  - Motor de estilo: colores automáticos claro/oscuro con contraste por luminancia, fondo redondeado pintado como bitmap (RemoteViews no acepta drawables arbitrarios), opacidad, radio, escala de texto, negrita sintética vía `setPaintFlags` (RemoteViews no puede invocar `setTypeface`).
  - Editor con preview real: las mismas RemoteViews que verá el launcher, aplicadas en un contenedor; formulario dinámico generado del esquema de cada diseño; modos nuevo/editar/configurar (se abre al añadir el widget desde el launcher).
  - UI principal con biblioteca («Mis widgets» con mini-preview), catálogo de 12 tarjetas y botón de añadir a pantalla de inicio (`requestPinAppWidget`).
  - Tests unitarios de la lógica pura (progreso de periodos, cuenta atrás, formatos, esquema del catálogo) — 13 tests.
  - CI en `.github/workflows/build.yml`: build + tests en cada push, artefacto de APKs, Release en tags `v*`, firma estable con fail-fast si faltan secrets (lección MihonFix aplicada).
  - Firma: keystore PKCS12 generado localmente, `CN=WidgetCraft`, válido hasta 2056, huella SHA-256 `7A:AF:B2:70:F7:51:AD:13:0A:3D:56:5C:47:63:E3:9B:79:F8:37:98:D9:2F:F4:D0:75:A1:B6:18:84:F4:86:50`, montado como secrets del repo vía API. Copia de la identidad de firma entregada al usuario fuera del repo.
  - Estado: primera build en CI pendiente de verificarse verde; APK se entregará al usuario tras verificar firma y versionCode.
