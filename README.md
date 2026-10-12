# PS5 Notify (Android)

**Estado: beta.** PS5 Notify reenvía a la PS5 las notificaciones de las aplicaciones que selecciones en el móvil. La app recibe las notificaciones en tiempo real y, además, puede volver a consultar periódicamente las notificaciones que Android mantiene activas.

## Configuración

1. Instala el APK generado por GitHub Actions: pestaña **Actions** -> último **Build APK** -> **Artifacts** -> `ps5-notify-apk`.
2. Transfiere el archivo `notify-bridge.elf` a la PS5 usando el método de transferencia disponible en tu configuración.
3. Abre **Payload Manager** en la PS5, selecciona `notify-bridge.elf` y ejecútalo como payload. El bridge debe estar ejecutándose para que la PS5 reciba los mensajes.
4. En PS5 Notify, introduce la IP de la PS5 y el puerto del receptor. El puerto predeterminado es `9055`.
5. El token predeterminado es `ps5notify`. No lo cambies: debe coincidir con el token del `notify-bridge.elf`. El campo aparece bloqueado en la app.
6. Pulsa **Conceder acceso a notificaciones** y autoriza PS5 Notify en los ajustes de Android.
7. Selecciona las aplicaciones que quieras reenviar y guarda la configuración.
8. Usa **Guardar y enviar notificación de prueba** para comprobar la conexión.

Si reinicias la PS5 o vuelves a cargar el entorno de payloads, ejecuta otra vez `notify-bridge.elf` desde Payload Manager antes de usar la app.

## Recepción y comprobación

La recepción en tiempo real se mantiene activa. El intervalo adicional permite revisar de nuevo las notificaciones activas, con un mínimo de 1 minuto y un valor predeterminado de 2 minutos. Puedes cambiarlo desde **Verificar también cada (minutos)**; el nuevo intervalo se aplica al guardar.

Después de conceder el permiso, Android mantiene el servicio de notificaciones aunque la pantalla esté apagada o estés usando otra app, así que puedes dejar el móvil bloqueado mientras juegas. El teléfono debe permanecer encendido, con PS5 Notify instalada, el permiso habilitado y conexión a la misma red que la PS5. Apagar el móvil por completo o forzar la detención de la app impedirá recibir notificaciones.

La opción **Mostrar el contenido del mensaje en la TV** está desactivada por defecto para proteger la privacidad.