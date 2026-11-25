# AURA — estado de continuidad v0.1.8 · 2026-11-24

## VERIFICADO (solo entorno Linux)
- Fuente derivada reversible de v0.1.7, sin sobrescribir original.
- 18/18 entradas de pruebas portátiles; manifiesto SHA-256 del código y copia de QA aislada.
- AUDIO: un buffer PCM16 vacío no modifica cobertura PTS; test nuevo de 5 aserciones.
- WEBVIEW: lectura del informe nativo mediante WebViewAssetLoader; contenido externo / file chooser deshabilitados (estático + Chromium).
- SEGURIDAD: verificador APK exige firma con esquema verdadero; rechaza cadenas `NOT verified`/`false` y ZIP de expansión excesiva antes de descomprimir; casos hostiles simulados.
- GITHUB: workflow `workflow_dispatch` manual, QA portátil como dependencia de lint+APK; artefacto con puerta de firma, permisos y hashes. Sin run remoto.

## EJECUTADO
- Se consultaron por conector GitHub 17 repositorios del propietario; ninguno tiene identidad AURA. No se modificó ningún repositorio ajeno.
- Fuentes preservadas; costo externo $0 MXN.

## PENDIENTE / NO GO
- No existe una APK compilada, lint real, firma de APK real ni pruebas con Android MediaCodec/SAF/WebView, por falta de Android SDK/Gradle y DNS en este entorno.
- No se lanzó GitHub Actions: falta repositorio **privado y exclusivo** AURA y verificación de minutos/cuota incluidos; no exponer fuente en repositorio público.
- Android aún no tiene certificación operativa ni AND-01…AND-15 en dispositivo; solo una prueba física final después de QA autónoma.
- Auditoría editorial con transcripción y guion auténticos, LUFS/true peak, SEO y derechos sin cerrar.
- Integración remota de respaldo binario v0.1.8 a Dropbox sin acreditar.

## SUPUESTOS
- La versión `v0.1.8` es candidata de fuente, nunca versión de APK lista para distribución.
- GitHub Actions privado podría consumir cuotas del propietario si se inicia; flujo intencionalmente manual.
- DPF-FALL anterior es un mecanismo de recuperación independiente, no reemplaza los detectores de AURA.

## CRITERIO DE ACEPTACIÓN
1. QA portable PASS.
2. APK y lint Gradle + `check_apk_gate.py` con herramientas SDK oficiales PASS.
3. QA Android automatizado con códecs, SAF, exportación y seguridad PASS.
4. Una sesión física final AND-01…AND-15 PASS.
5. Dictamen editorial delimitado con insumos reales, y sin llamar `completado` a detector omitido.
6. Respaldo remoto cotejado por SHA, documentación sincronizada; video original no modificado.
