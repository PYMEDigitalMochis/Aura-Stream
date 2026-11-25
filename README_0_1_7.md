# AURA Android v0.1.7 — candidata, NO GO Android

Derivado del código v0.1.6 sin alterar fuentes ni videos históricos. Corrige cobertura PCM falsamente aceptada (antes podía faltar el 15 %), discontinuidades PTS acumuladas, fallos indebidos para pistas de audio más cortas que el video, riesgo de desbordamiento de RAM al decodificar video >1080p, y añade cancelación explícita. Niveles RMS y pico de muestra **no** equivalen a LUFS/true peak. Todos los hallazgos son candidatos.

Motor Python/FFmpeg v0.3.3 y Centro editorial son componentes independientes que no se han modificado aquí.

**No es una APK:** faltan Android SDK/Gradle en este entorno y pruebas de códec/SAF/WebView reales. Las pruebas de JVM y Chromium no acreditan compilación Android.

QA portable: `sh tools/run_qa_isolated.sh`. Preflight Android: `sh tools/preflight_P0.sh`. Build pendiente en SDK real: `gradle --no-daemon :app:assembleDebug :app:lintDebug`. Tras compilar, verificar firma, permisos y assets mediante `python3 tools/check_apk_gate.py --apk app/build/outputs/apk/debug/app-debug.apk --sdk "$ANDROID_HOME" --source .`.

**No realizar prueba física hasta superar build, lint, APK gate real y QA autónoma adicional.** Estado y pendientes: `estado.md`; acta: `docs/ACTA_ADVERSARIAL_v0_1_7_2026-11-23.md`.
