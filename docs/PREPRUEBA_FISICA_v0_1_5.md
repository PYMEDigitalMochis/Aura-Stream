# PREPRUEBA FÍSICA AURA v0.1.5

**NO INSTALAR NI PROBAR EN ANDROID AÚN.** Primero obtener APK real y pasar el gate estático, build, lint, firma y permisos.

Correcciones incrementales respecto a v0.1.4:
- Detección visual no declara `completed` si `getScaledFrameAtTime` tarda más que el plazo, incluso si demora en la última llamada. El bloqueo dentro de un codec no es forzosamente abortable: aún requiere dispositivo real.
- Interrupción durante cómputo se refleja; los resultados parciales se descartan.
- La pantalla informa el estado real `completed`, `failed` o `not_run`, el número de candidatos y la razón.
- Multiplicación de ancho×alto se realiza en `long`; lectura asset JS sin progreso se detiene.
- Se preservan exportación segura, salvaguarda del informe anterior y bloqueo editorial sin persistencia.

Pruebas físicas solo tras el gate de build: un recorrido de aceptación exhaustivo, ninguna repetición salvo fallo reproducible y corrección.
