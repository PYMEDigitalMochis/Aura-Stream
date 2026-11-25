# AURA · Auditoría adversarial Android v0.1.2 · 22/11/2026

**Estado:** fuente POC corregida, **NO APK**, **NO Android físico verificado**. Derivado aparte de fuente `v0.1.1`, 43.978 B y SHA `a49d4c466d5925e5880a17a81196701f7fabdbc6530535d2e881a08ae9680c4a`. HTML incluido es el visor histórico **sin modificar** (`77d0e5d1c75303ca8dcfbbb65b0017887ae6d3a7d94d211139e9ecff289cd3fa`).

## Hallazgos y correcciones

| ID | Riesgo | Contramedida implementada | Estado |
| --- | --- | --- | --- |
| AUR-A12-01 | Alta: guardar sobre documento existente con modo `w` puede conservar bytes finales del contenido anterior | Solicitar `openOutputStream(uri,"wt")`; comparar bytes de lectura posterior sin aceptar tamaño extra, truncamiento, alteraciones o lectura bloqueada | VERIFICADO en JVM pura, Android pendiente |
| AUR-A12-02 | Alta: el visor permitía decisiones y rúbrica editorial sin exportación/restauración fiable | Controles de exportación, restauración, Centro, decisiones y rúbrica deshabilitados explícitamente; banner persistente de modo lectura | VERIFICADO Chromium 360 px; Android pendiente |
| AUR-A12-03 | Media: UI podía aparentar perder o mantener un análisis durante rotación | Cancelar trabajo al destruir Activity, distinguir resultado obsoleto, advertir análisis interrumpido tras recreación | VERIFICADO con compilación stubs; Android pendiente |
| AUR-A12-04 | Media: proceso de inspección anterior podía devolver resultado después de seleccionar otro video | `Future.cancel` + token de solicitud, sin éxito de resultados obsoletos | VERIFICADO con compilación stubs; Android pendiente |
| AUR-A12-05 | Baja: identificador de motor continuaba diciendo 0.1.1 | Actualizado a `android-native-metadata-poc/0.1.2`; `versionName` 0.1.2-qa | VERIFICADO fuente/contrato |

## Suite actual reproducida en Linux

1. `python tests/check_contract.py` — PASS, 15 entradas inválidas rechazadas, contrato JS compatible.
2. `python tests/check_android_stubs.py` — PASS, tres clases Java compiladas contra stubs mínimos (simulación, no SDK).
3. `python tests/check_export.py` — PASS, **5/5** casos Java: exacto, bytes extra, truncado, alterado y lectura sin progreso.
4. `python tests/check_ui.py` — PASS, Chromium 360 px.
5. `python tests/check_adversarial_ui.py` — PASS, Chromium 360 px, seis controles críticos deshabilitados, decisiones y rúbrica en solo lectura, cero llamadas HTTP.

## Pendientes no sustituibles

- **P0 Bloqueado:** falta Android SDK 36, Gradle 9.6, runner de costo 0 MXN acreditado y APK compilada. **Ninguna instalación Android fue realizada**.
- **P0 Validación humana:** provider SAF real, tamaño final, apertura y relectura `content://`, denegación de permisos, rotación, segunda selección, detención y recuperación, WebView y `Blob`.
- **P0 Funcionalidad:** NO existen cinco detectores nativos. Reporte `aura.audit/1` permanece `technical_status=incomplete`, cinco checks `not_run`/`not_applicable` y revisión humana pendiente. **NO PUBLICAR**.
- **P1:** integración directa con Centro original, ASR, derechos, licencias de distribución, protección de datos y pruebas de consumo/temperatura del teléfono.

Fuentes actuales recomendadas para compilar: https://developer.android.com/build/releases/about-agp y https://developer.android.com/reference/android/content/ContentResolver. No usar cuotas de GitHub Actions privadas sin comprobar consumo gratuito. Mantener código y video originales.
