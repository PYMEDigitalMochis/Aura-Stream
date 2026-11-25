# AURA Android 0.1.6 — candidata experimental

Se añadieron intentos nativos de decodificación completa de pistas con MediaExtractor/MediaCodec, medición de RMS/sample peak PCM16 y detección candidata de silencios por ventanas. Sin Termux. Mantiene visor e integración JSON `aura.audit/1`.

**Limitaciones obligatorias:** sin APK compilada, sin emulador ni prueba Android física. Las APIs se compilan únicamente con firmas simuladas. MediaCodec con salida de video sin Surface puede no ser compatible con determinados dispositivos; en ese caso el control debe ser `failed`, jamás `completed`. Las métricas RMS no son LUFS y sample peak no es true peak. Silencios son candidatos, no errores narrativos confirmados. La comprobación visual de 1 fps cubre solo clips de 3–3600 s y máximo 120 muestras. Aunque los cinco controles se ejecuten, no se certifican narrativa, diálogos, SEO ni licencias.

No se tocó ninguno de los ZIP de origen. El instalable no puede considerarse disponible hasta compilación contra SDK oficial, lint y verificación de firma.
