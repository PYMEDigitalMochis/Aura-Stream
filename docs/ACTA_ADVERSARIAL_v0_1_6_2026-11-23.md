# AURA — acta adversarial de código v0.1.6 (23-11-2026)

**DICTAMEN: NO GO PARA PRUEBA FÍSICA.** El código de la candidata v0.1.6 supera pruebas portátiles en Linux pero no se ha compilado una APK ni ejecutado Android real. La aprobación del producto permanece bloqueada; ninguna prueba basada en stubs equivale al SDK.

## Procedencia e integridad

- Fuente histórica original: `AURA_QA_PRE_FISICA_v0_1_5_2026-11-22.zip` SHA-256 `8d4d17e928f3db507bca58eb5c65a4f6227f13cb12c382876fe963ce3bc478a1`. Extraída de Biblioteca ChatGPT; CRC PASS, fuentes internas SHA `eec200ff3c321eeee1dddfab7ad8c7a2dfc6680ccf6e87b6ef9045384b8a029d`, intactas. El motor separado v0.3.3 no fue modificado ni reauditorizado.
- Derivado aditivo `AURA_ANDROID_v0_1_6_CANDIDATE`; sin alterar archivos originales, video NFC ni Centro editorial.

## Correcciones implementadas

| Control | Evolución | Alcance comprobado | Limitación |
| --- | --- | --- | --- |
| Decodificación | `AndroidCodecAuditor` invoca MediaExtractor/MediaCodec por pista con EOS, muestras parciales, rechazo DRM, límites por pista y globales, interrupciones y salida conservadora | Compilación Java contra stubs; validación de contratos JVM | No Android SDK real, soporte de códecs pendiente. EOS no detecta todos los errores ocultados por el decodificador |
| Silencio | `AudioSignalDetector` PCM16, ventanas 100ms, RMS umbral -48dBFS, mínimo 0.6s, etiquetas por pista | Pruebas de límites, canales, PTS, inmutabilidad, tramo de silencio y FFmpeg→AAC→PCM real | Sin ejecución del decodificador Android ni escucha humana |
| Sonoridad | RMS dBFS y pico por muestra, análisis streaming; métricas por pista y método explícito | Contrato JSON/Node, JVM y referencia sintética AAC | No mide LUFS EBU R128, true peak, ruido ni inteligibilidad |
| Negro/congelado | Muestreo adaptativo 3–3600 s, máximo 120 frames; tiempo y estado reportados, fracaso descarta hallazgos parciales | 24 aserciones adversariales visuales y fixture MP4 FFmpeg | No evalúa todos los fotogramas; falsos positivos/negativos posibles |
| Seguridad/permisos | Original y activo SAF de solo lectura, doble hash, salida JSON verificada, WebView local aislado, sin puentes ni INTERNET | Análisis estático y Chromium 360px | El resultado depende del proveedor SAF y del WebView real |
| Contrato | `aura.audit/1` mantiene 5 controles, hallazgos con intervalos, métricas, estados, fuente intacta y revisión humana obligatoria | Validación JS original sin modificar y pruebas Java | `technical_status=completed` solo acredita que terminaron los controles declarados; no autoriza publicación |

## Batería y reproducibilidad

- `tools/run_qa_isolated.sh`: 14/14 scripts Python, pruebas Java, FFmpeg, Chromium y modelos de APK. Ejecutar en copia temporal; manifiesto SHA antes/después. Probar y registrar la salida de esta versión.
- `tests/check_pcm_native_contract.py`: 15 aserciones PCM + 14 adversariales, salida JSON compatible con `aura.audit/1`, revisión editorial pendiente.
- `tests/check_pcm_ffmpeg.py`: AAC generado con FFmpeg y decodificado a PCM, Java detecta silencio aprox. 1.0–2.0 s, RMS -16.83 dBFS (referencia sintética).
- El gate `tools/check_apk_gate.py` exige APK firmada, identidad y versión `0.1.6-qa`, permisos cero, assets intactos y DEX. Los 5 escenarios del gate son SIMULADOS, no prueba real de firma.

## Fallos / riesgos abiertos prioritarios

1. **P0 BLOQUEADO:** ausencia de Android SDK/Gradle/adb en el entorno; sin acceso DNS para instalarlos; no APK, firma, lint ni prueba física. No se ha probado `MediaCodec` ni `MediaMetadataRetriever` real.
2. **P0:** confirmar que los códecs Android admiten decodificación de video con buffers de salida sin `Surface` y que la detección/medición funciona, sin congelar la UI. Probar SAF con proveedores reales, rotación, consumo de CPU/RAM, accesibilidad y archivos corruptos.
3. **P0:** prueba de aceptación única AND-01…AND-15, tras superar build, lint y pruebas portátiles y de emulación. No solicitar pruebas físicas por corrección aislada.
4. **P1:** audio con LUFS/true peak real, ASR offline, alineación guion/diálogos sobre material real, auditoría narrativa y SEO sustantivo, derechos y escucha humana. Estos requisitos no están implementados en la app Android.
5. **P1 documental:** respaldo binario de esta versión en Dropbox y recuperación para cotejo SHA-256; existe historial documental pero no paridad nueva certificada.

## Restricciones y veredicto

No publicar NFC v8 ni otros videos, no modificar originales, no instalar sin APK certificada, no gastar ni utilizar acciones con costo o cuota dudosa. Esta acta acredita únicamente la implementación y pruebas locales que se ejecuten; **no acredita auditoría completa superada**.
