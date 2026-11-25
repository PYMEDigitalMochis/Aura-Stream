# AURA — acta adversarial incremental Android v0.1.7 · 2026-11-23

## Resultado y alcance
**NO GO Android.** Las pruebas unitarias y de integración portátil son evidencia Linux/JVM/Chromium, no pruebas de APK instalable ni de MediaCodec/SAF Android real. Se preservan originales.

## Hallazgos nuevos

| ID | Riesgo | Defecto comprobado en fuente 0.1.6 | Corrección 0.1.7 | Regresión específica |
| --- | --- | --- | --- | --- |
| AUR-PCM-21 | Alto | Pista PCM al 85 % de la duración esperada podía aprobar. | Máximo margen 0,25–1 s y control de discontinuidades PTS acumuladas. | `TestAudioAdversarial` (cobertura 90 % rechazada y PTS discontinuas), `TestAudioProperty` (20 segmentaciones). |
| AUR-PCM-22 | Medio | Se comparaba duración de audio con duración de video, sin comprobar la duración individual declarada para la pista. | Se extrae `MediaFormat.KEY_DURATION` por pista si es válida; en ausencia se conserva postura restrictiva. | Prueba de pista corta con su duración y comprobación de acoplamiento de `AndroidCodecAuditor`. |
| AUR-RAM-23 | Alto | No existía límite explícito de píxeles de video enviados al decodificador de buffers MediaCodec. | Rechazo conservador a `not_run` de resolución superior a 1920x1080 o sin dimensiones confiables. | Compilación estática con stubs + comprobación de cableado, pendiente Android. |
| AUR-UX-24 | Medio | No había botón para cancelar un análisis largo sin volver a escoger archivo o rotar. | Cancelación del Future, invalidación de callbacks y conservación del informe anterior. | Compilación estática y prueba de cableado, pendiente bloqueo nativo Android. |
| AUR-SEC-25 | Alto | Verificador APK usaba `assert`: controles de firma, permisos y assets desaparecían bajo `python -O`. | `require()` explícito, límites de expansión ZIP y rechazo de rutas peligrosas. | Casos con `-O`: permisos inesperados, assets alterados y rutas inseguras rechazadas. |

## Matriz de evidencia
- **EJECUTADO / VERIFICADO EN LINUX:** suite `tools/run_qa_isolated.sh` en copia temporal; manifest SHA-256 de todos los archivos antes y después; pruebas Java y Chromium 360 px. Ver log QA portable junto al paquete.
- **PENDIENTE:** compilación `:app:assembleDebug :app:lintDebug`, firma/manifest/apk real, emulador, SAF, MediaCodec, RAM y uso físico.
- **SUPUESTO:** los API de Android usados por el código son compatibles con API 28+; el compilador de SDK auténtico puede revelar incompatibilidades no detectadas por stubs.

## Controles omitidos deliberadamente
Pruebas físicas AND-01..15, LUFS/true-peak, ASR, análisis de semántica real de diálogos/guion, auditoría sustantiva de derechos, SEO humano y publicación. Sin archivo de video real en esta pasada. **No se acredita auditoría integral del producto.**

## Dictamen
**No aprobar APK ni solicitar prueba física.** Siguiente evidencia verificable: salida Gradle `assembleDebug/lintDebug` y salida `check_apk_gate.py` ejecutada contra APK genuina. Budget $0 MXN, sin publicaciones ni originales alterados.
