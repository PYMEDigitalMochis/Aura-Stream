# P0 · Puerta Android reproducible · 2026-11-22

Estado de fuente: **EJECUTADO Y QA PORTÁTIL VERIFICADO**. Estado APK: **PENDIENTE**. No declarar Android físico probado.

## Compilación sin Termux en runner/Android Studio (si existe entorno gratuito y autorizado)

- JDK 17, Gradle 9.6.0, Android SDK API 36, build-tools 36.0.0, Android Gradle Plugin 9.4.0.
- Proyecto privado, sin activar facturación ni ampliar límite de gasto. No iniciar GitHub Actions privado si no se conoce saldo y límite efectivo $0. El workflow es manual.
- Con las fuentes de este ZIP en la raíz del proyecto:

```bash
sh tools/preflight_P0.sh
python3 tests/check_contract.py
python3 tests/check_android_stubs.py
python3 tests/check_export.py
python3 tests/check_webview_lifecycle.py
gradle --no-daemon --stacktrace :app:assembleDebug
APK=app/build/outputs/apk/debug/app-debug.apk
test -s "$APK"
sha256sum "$APK"
"$ANDROID_SDK_ROOT/build-tools/36.0.0/apksigner" verify --verbose "$APK"
"$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt" dump badging "$APK"
```

A `gradle --version` NO prueba que el SDK/AndroidX esté instalado. Un APK sin firma válida tampoco es una prueba satisfactoria. No entregar como APK hasta que exista y se haya verificado con herramientas genuinas.

## Matriz de QA Android físico (PENDIENTE; no ejecutar hasta contar con APK legítima)

| ID | Prueba | Esperado (no acreditar sin captura/log del dispositivo) |
| --- | --- | --- |
| AND-01 | Instalar APK debug verificada | Instalación en Android 9+ y arranque sin cierre inesperado |
| AND-02 | Elegir `AURA_VALIDO_720p_60s_AAC.mp4` | SHA `f45df86e904fc2e1adb974c59aa230c540d5d8d86a3ae79b236137ce02e89af7`; 60 s; 2 pistas; video 1280×720 |
| AND-03 | Exportar JSON SAF y leerlo de vuelta | `aura.audit/1`; fuente SHA coincidente; controles visuales `completed` solo si muestreo 3–120 s exitoso; otros `not_run`/`not_applicable`, dictamen siempre incompleto |
| AND-04 | Elegir `AURA_SIN_AUDIO_320p_4s.mp4` | 4 s, 1 pista video; `silence`/`loudness` no aplicables |
| AND-05 | Elegir `AURA_VIDEO_INVALIDO.mp4` (si el selector lo permite) | Error sin archivo exportable; no sobrescribe el informe anterior sin advertir |
| AND-06 | Rotar durante SHA-256 | Cancelación o reintento explícito, nunca informe mezclado |
| AND-07 | Retroceder del visor 10 veces | Regreso a pantalla nativa, sin cierres inesperados ni fugas crecientes |
| AND-08 | Provocar pérdida de proceso WebView | Vuelta segura a pantalla nativa, sin usar WebView destruido |
| AND-09 | Desconectar internet | La app no realiza solicitudes; SAF de proveedores externos podría depender de su propia red |
| AND-10 | Verificar permisos instalados | Sin INTERNET, cámara, micrófono ni almacenamiento global |
| AND-11 | Verificar cancelación SAF | Sin escribir y sin cambiar video |
| AND-12 | Revisar RAM/temperatura con archivos hasta 512 MiB | Registrar recursos reales y cancelación bajo presión |
| AND-13 | Reabrir app e importar JSON | JSON nativo legible por contrato, las decisiones web no se guardan |
| AND-14 | Pasar TalkBack/navegación con fuente grande | Controles accesibles; revisar contraste y lectura completa |
| AND-15 | Someter archivo con error de decodificación | No confundir POC de metadatos con detector; los estados dependen de cobertura visual real; `decode` y audio siguen `not_run` |

## Bloqueos

- SDK/Gradle ausentes del contenedor actual; no DNS externa. El pipeline no se ejecutó.
- No existe repositorio propio AURA en GitHub conectado. No se gastaron minutos de Actions.
- Detectores nativos de decodificación, negro, congelamiento, bajo audio y sonoridad pendientes de implementación y QA real.
- La suite con stubs y Chromium mide comportamiento portable; nunca demuestra instalación Android.

## Clasificación

- **VERIFICADO**: hashes, ZIP CRC, ffprobe de vectores, pruebas Linux y controles estáticos.
- **EJECUTADO**: cambios en fuente derivada v0.1.4, nunca en los originales.
- **HIPÓTESIS**: el retiro correcto del WebView prevendrá fallas de ciclo de vida reales.
- **PENDIENTE**: ensamblar/firmar/instalar APK; medir rendimiento y detectores.
