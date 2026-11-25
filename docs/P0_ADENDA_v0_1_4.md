# AURA v0.1.4 — avance P0 autónomo (2026-11-22)

**EJECUTADO:** derivado aditivo de v0.1.3 con sampler visual limitado.
**VERIFICADO LINUX:** algoritmo puro Java y JSON `aura.audit/1` validado por motor 0.3.2; pruebas con frames sintéticos y MP4 real sintético decodificado por FFmpeg. Stubs Java NO equivalen al Android SDK.

## Cobertura real

- `black`: candidatos de pantalla negra por muestreo 1 fps para clips de 3–120 s, no frame-by-frame.
- `freeze`: candidatos de imagen casi inmóvil por muestreo 1 fps, NO distinción automática de pausa legítima/imagen de archivo/congelamiento.
- `decode`: **not_run**, no se realiza decodificación integral.
- `silence`, `loudness`: **not_run** cuando existe audio; **not_applicable** cuando no.
- Si no se obtiene alguno de los fotogramas requeridos, controles visuales `failed` y ningún hallazgo parcial engañoso. Siempre `technical_status=incomplete` y revisión editorial pendiente.
- SHA-256 antes/después y tamaño contra proveedor, fuente original sin modificación.

## Corrección de flujo

Rechazar un segundo archivo no elimina el informe anterior. Se mantiene el visor de solo lectura sin exportaciones ni decisiones no persistidas.

## PENDIENTE P0

SDK real y ensamblado APK; proveedor SAF real, dispositivos de varias versiones, codecs/MediaMetadataRetriever reales, rendimiento/memoria y accesibilidad. Después implementar decodificación integral, audio y otros defectos. El usuario no debe instalar ninguna supuesta APK de esta entrega; son fuentes.


QA pre-física v0.1.5: se agregó límite comprobado después de decodificar, estado visual explícito y checkpoint de integridad. No es APK.
