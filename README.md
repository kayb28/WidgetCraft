# WidgetCraft

App Android para **aplicar y crear widgets** en la pantalla de inicio: catálogo de 12 diseños base + editor de personalización, todo ajustable al tamaño que quieras y adaptado al modo claro/oscuro del sistema.

## Qué puede hacer

- **12 diseños base**: reloj digital, fecha, batería, contador de toques, nota rápida, cita del día, cuenta atrás, progreso del tiempo (día/mes/año), control de tiempo, lista de tareas, estado del dispositivo y accesos directos.
- **Editor con preview real**: el preview del editor usa exactamente las mismas RemoteViews que verás en la pantalla de inicio.
- **Personalización**: color de fondo (o automático claro/oscuro), opacidad, esquinas redondeadas, color de texto con contraste automático, color de acento, escala de texto y negrita — más las propiedades propias de cada diseño.
- **Ajustable a la pantalla**: los widgets se pueden redimensionar en horizontal y vertical desde el launcher; el fondo redondeado se re-renderiza al nuevo tamaño.
- **Temas**: la app sigue el modo claro/oscuro del sistema (o lo fuerza desde Ajustes); los widgets calculan sus colores automáticamente según el modo del sistema.
- **Añadir desde la app**: botón «Añadir a la pantalla de inicio» (solicitud de pin) o desde el selector de widgets del launcher.

## Instalación

Los APK firmados se publican en dos sitios:

1. **Releases**: [github.com/kayb28/WidgetCraft/releases](https://github.com/kayb28/WidgetCraft/releases) — en cada tag `v*`.
2. **Actions**: artefacto `widgetcraft-apks-<sha>` en cada build de `main`.

El certificado de firma es **estable desde la primera build** (CN=WidgetCraft): todas las versiones se actualizan encima de la anterior sin desinstalar (lección aprendida en MihonFix, donde cada build se firmaba con un keystore efímero y ninguna actualizaba).

## Compilar

```bash
./gradlew :app:assembleRelease :app:testReleaseUnitTest
```

Requisitos: JDK 21, Android SDK (compileSdk 35). Sin firma local el release sale sin firmar; la CI firma con los secrets del repo.

## Estructura

```
app/src/main/java/com/widgetcraft/app/
├── core/       WidgetData.kt (12 diseños + esquema + persistencia), StyleEngine.kt (colores auto, fondos, tiempo)
├── render/     WidgetRenderer.kt (configuración → RemoteViews por diseño)
├── widget/     WidgetProvider.kt (único receptor de widgets + acciones de toque)
└── ui/         MainActivity (biblioteca + catálogo), EditorActivity + FormBuilder, SettingsActivity
```

## Convenciones del repo

Ver `AGENTS.md`: firma estable en secrets de CI, bump de `versionCode` en cada versión, registro cronológico en `docs/REGISTRO.md`, todo entregado documentado.
