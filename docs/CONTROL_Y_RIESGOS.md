# AURA Android v0.1.1 — control de riesgos

**VERIFICADO LINUX:** pruebas Python y Java portable con 15 rechazos; compilación de los tres archivos Java con firmas simuladas Android (NO SDK); validación de JSON con contrato v0.3.2; visor original sin modificación, pruebas Chromium 360 px con informe inyectado; sin red; exportaciones WebView bloqueadas a propósito.

**IMPLEMENTADO, SIN QA FÍSICO:** SAF solo lectura, SHA-256 doble, MediaExtractor, MediaMetadataRetriever, exportación SAF, estado preservado tras rotación si el cálculo ya terminó, control de resultados antiguos. Si un proveedor no soporta FD seekable se rechaza: no afirmar lectura exitosa.

**PENDIENTE P0:** APK real y Android SDK; pruebas en Android 14/16 de selectors content://, salida JSON, reinicio/rotación en lectura, WebView, carga archivo y memoria. El flujo de GitHub Actions es manual, no ejecutado, privado y sujeto a verificación de cuota/billing.

**PENDIENTE P0:** No existen aún detectores nativos FFmpeg-equivalentes: decode, black, freeze, silence, loudness (todos not_run). El JSON NO acredita ausencia de errores.

**PENDIENTE P1:** Exportación de decisiones/dossier del visor y Centro. En este prototipo se deshabilitan intencionadamente para evitar falsas indicaciones de guardado. El JSON nativo de metadatos sí ofrece un flujo SAF separado (no probado físicamente).

**PENDIENTE P1:** Identidad y derechos audiovisuales; QA narrativo; respaldo ZIP binario recuperado desde Dropbox. El canal y el video NFC v8 NO PUBLICAR.

**Aprobación:** ninguna. No se modificó el motor original AURA v0.3.2 ni el POC v0.1.0. No consumidos minutos GitHub Actions.
