# DE LAS SIERRAS RADIO — Android (prototipo 1)

Aplicación Android nativa Kotlin + Media3. Stream: https://server.streamcasthd.com/8682/stream

## Sin instalar herramientas de desarrollo

1. Crear un repositorio **público** en GitHub (el plan gratuito incluye minutos de Actions para repositorios públicos con runners estándar).
2. Subir el contenido de esta carpeta a la **raíz** del repositorio, manteniendo `.github/workflows/android.yml`.
3. Abrir **Actions → Build Android APK → Run workflow** (o hacer push a `main`).
4. Cuando finalice, abrir la ejecución y descargar el artefacto `DeLasSierrasRadio-debug-apk`.
5. Descomprimir el ZIP descargado e instalar `app-debug.apk` en Android autorizando la instalación desde esa fuente.

**Importante:** el APK debug es para pruebas privadas, **no** para distribuir en tiendas. Para publicación es necesario generar un APK/AAB release firmado con una clave propia guardada de forma segura. No compartir ni perder esa clave.

## Funcionalidades

- Stream MP3/AAC según la fuente compatible con ExoPlayer.
- MediaSessionService para reproducción en segundo plano y controles del sistema.
- Reconexión escalonada en errores de reproducción mientras el usuario quiere escuchar.
- Diseño preliminar inspirado en la web (bordó, magenta, cian, amarillo).

## Pendientes antes de publicación

- Verificar la compilación real en GitHub Actions y probar en dispositivos físicos, incluyendo pantalla bloqueada, Bluetooth, llamadas y pérdida de conexión.
- Agregar icono/logo oficial y gráficos definitivos (la primera versión utiliza elementos tipográficos provisionales).
- Validar funcionamiento real del endpoint y formato de stream.
- Integrar API de SonicPanel para canción actual e historial (no se incluyen aún).
- Configurar firma release, política de privacidad y documentación exigida por cada tienda.
- Revisar licencias de emisión de música para las regiones de distribución.
