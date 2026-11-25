# P0 · Una única sesión de prueba física (no iniciada)

**NO GO antes de APK verificada.** No usar el teléfono para depurar problemas de compilación, firma, permisos ni contrato.

Requisitos para abrir la sesión AND-01…15:
1. Las 12 suites locales aprobadas en copia, fuente preservada con SHA/CRC.
2. Compilación *real* con SDK 36 + Gradle 9.6 + AGP 9.4 + JDK 17; lint sin errores críticos, sin permisos agregados.
3. APK con `app-debug.apk`, certificado de depuración verificado mediante `apksigner` y paquete/versión cotejados mediante `aapt`.
4. Dos vectores de 60s 720p y 15s negro/congelado + 4s sin audio + 1 archivo inválido disponibles sin datos personales.
5. Una única sesión de aceptación: registro AND-01…AND-15 en el HTML offline `AURA_PREPRUEBA_ACEPTACION_UNICA_2026-11-22.html`, exportando un JSON. No volver a ejecutar casos aprobados sin cambios que los afecten.

**No equivalencia:** 1fps no demuestra todos los fotogramas; faltan decodificación integral y tres detectores nativos de audio/decodificación. Un programa que registra `technical_status=incomplete` no puede aprobar automáticamente videos; NFC v8: NO PUBLICAR.

**Riesgos pendientes:** real Android WebView/DataTransfer/SAF, rendimiento, bloqueo no interrumpible de códec, detección de falsos positivos, accesibilidad, seguridad de proveedores content://, recursos del dispositivo y licencias. Ninguno queda certificado solo con QA Linux.

**Estado de la puerta (22/11/2026):** SIN APK; SDK/Gradle ausentes, DNS de servidor Gradle bloqueado en contenedor. Contenedor de Colab preparado pero no ejecutado. Costo 0 MXN.

## Adenda 0.1.6 (23/11/2026)

- Gate de lógica autónoma ampliado a 14 scripts QA (incluye fuente AAC creada y decodificada por FFmpeg y medidor JVM PCM).
- Los tres controles `decode`/`silence`/`loudness` ya tienen implementación tentativa Android con MediaCodec; **sin build ni prueba real no se marcan verificados**.
- Visual ampliado a 3–3600 segundos, máximo 120 muestras con intervalos adaptativos; hallazgos son candidatos, no inspección exhaustiva.
- El gate previo físico permanece **NO GO** hasta APK, firma, lint, permisos, códec real, persistencia y cobertura probados.
- ASR/guion semántico y SEO sustantivo siguen fuera del alcance automatizado actual. No se repiten pruebas físicas históricas.
