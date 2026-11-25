# Acta CURA/ALL — AURA Android v0.1.8

Estado: **EJECUTADO / VERIFICADO EN LINUX, NO GO ANDROID**.

### Origen
- ZIP biblioteca AURA Android fuente v0.1.7 del 23/11/2026, fuente SHA-256 `0133bb8dd53b4b6fb44a7bf9916227061d56fdab2fecaeab01af3a2161e6ce67`.
- Motor editorial Python/FFmpeg v0.3.3 separado y preservado.

### Correcciones
1. El PCM vacío ya no adelanta el tiempo o cobertura del detector de audio.
2. WebView de lectura deshabilita `content://`, selector web y entrada de archivo no respaldada.
3. Verificador APK rechaza la cadena `NOT verified`, firma `false` y ZIP con tamaño expandido excesivo antes de descomprimir.
4. GitHub Actions manual solo compila si la QA portátil pasa; no se lanza sin control de cuota.

### Seguridad/QA
- Pruebas hostiles de firma y expansión ZIP en ejecutor local; **simulaciones, NO apksigner Android real**.
- Chromium escritorio, audio sintético FFmpeg y javac con stubs Android; **NO equivalen a un dispositivo**.
- Manifiesto hash íntegro antes y después de cada ejecución aislada.

### Bloqueos
- Este entorno carece de SDK/Gradle/ADB y no resuelve DNS externos; no se ha producido APK ni artefactos firmados.
- No existe repo GitHub dedicado a AURA entre los 17 repositorios visibles. No se ocupó uno de otro proyecto ni se utilizó cuota de Actions.
- Quedan pruebas del comportamiento MediaCodec/SAF, apertura y exportación Android, accesibilidad, editorial y física.

### Dictamen
NO GO Android y NO PUBLICAR video hasta auditoría técnica+editorial con insumos, firma y 15 casos físicos de aceptación. No hay evidencia de ejecución remota.
