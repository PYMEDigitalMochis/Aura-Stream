# AURA Android v0.1.6 — candidato experimental (sin APK)

Herramienta privada de preauditoría audiovisual para Detrás del Botón. No publica, modifica ni autoriza videos. Motor Android nativo independiente del Centro editorial y del motor Python/FFmpeg v0.3.3. Presupuesto $0 MXN y sin Termux.

## Comportamiento implementado en código

- Selector de documentos SAF de solo lectura; comprobación SHA-256 en dos lecturas, cotejando tamaño antes/después. Conserva el informe anterior si se rechaza un segundo video.
- Inventario `MediaExtractor` y duración `MediaMetadataRetriever`.
- Decodificación de cada pista identificada con `MediaCodec` hasta EOS, con interrupción, límite de tiempo y controles conservadores ante DRM/códec no compatible. **No se ha validado en Android real.**
- Nivel RMS y pico por muestra PCM16, por pista, sin cargar todo el audio en RAM. Candidatos de silencio RMS inferior a -48 dBFS de 0.6 segundos o más; **RMS no equivale a LUFS ni pico por muestra a true peak.** Si hay datos insuficientes o discontinuidad, marca `failed`.
- Candidatos visuales negros/estáticos con `MediaMetadataRetriever` a 80×45 y **hasta 120 fotogramas en videos de 3–3600 s**. En videos largos se espacian más las muestras; no detecta todos los cuadros ni garantiza ausencia de defectos.
- Informe JSON `aura.audit/1` con controles `decode`, `black`, `freeze`, `silence`, `loudness`, métricas objetivas, advertencias de cobertura y revisión editorial humana obligatoria. Exportación SAF cotejada byte a byte; visor HTML local original conservado y funciones de decisión/desexportación no verificadas bloqueadas.
- Sin permisos de almacenamiento total, red, cámara o micrófono; sin puente WebView JavaScript.

## Auditoría

QA JVM/FFmpeg/Chromium y mocks prueban lógica y contrato, **no sustituyen APK firmada, SDK genuino ni dispositivo Android**. Si el códec del móvil no admite decodificación con buffers de salida sin Surface, el control falla de forma cerrada. No se han implantado ASR local ni revisión semántica automática de guion, SEO sustantivo o derechos. La interfaz editorial externa ya admite SRT/TXT y rúbrica manual bajo limitaciones documentadas.

Para certificar: compilar `:app:assembleDebug :app:lintDebug` con Android SDK 36 + AGP 9.4 + Gradle 9.6 + JDK 17, verificar firma `apksigner`, paquete y permisos con `aapt`, comprobar fuentes SHA-256, probar códecs/SAF/WebView y consumo en Android, **en una sola sesión AND-01…15 después de cerrar todos los bloqueos**. No certificar sin esa evidencia.

Fuente oficial de versión AGP: https://developer.android.com/build/releases/about-agp. API oficial MediaCodec: https://developer.android.com/reference/android/media/MediaCodec.
