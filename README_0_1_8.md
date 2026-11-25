# AURA Android v0.1.8 — candidata de código; NO GO físico

Base conservada: AURA v0.1.7. Motor editorial Python/FFmpeg v0.3.3 separado.

## Correcciones
- Ignorar los buffers PCM decodificados sin muestras; no alteran cobertura PTS.
- Desactivar acceso content:// y selector arbitrario desde el WebView; interfaz de carga visible deshabilitada; solo SAF nativo para fuente/JSON.
- Corregir dos falsos positivos de firma y validar límite de ZIP antes de descomprimir.
- GitHub Actions manual con 17 pruebas portátiles como condición de compilación, lint y puerta de APK.

## GitHub
Este paquete es independiente y no debe publicarse dentro de repositorios ajenos.
Crear un **repositorio privado dedicado** a AURA, subir el contenido de la carpeta raíz,
comprobar la capacidad gratuita y el saldo de Actions del propietario **antes de disparar**
`workflow_dispatch` del archivo `.github/workflows/android-debug-manual.yml`.
No se realizó ningún build remoto ni se generó APK en este entorno.

## Condiciones de cierre
APK real compilada y comprobada; lint; prueba Android emulada de SAF, MediaCodec, WebView y cancelación;
solo después, una sesión física integral; auditoría guion/SEO pendiente. Los detectores son
candidatos de revisión, no juicio final ni autorización de publicación.
