# 🎾 WishPort - Gestión de Reservas Deportivas

WishPort es una aplicación nativa para Android conectada a un backend en la nube, diseñada para agilizar el proceso de reserva de instalaciones deportivas y validación de accesos mediante códigos QR.

## 🚀 Configuración y Ejecución

### Requisitos Previos
* Android Studio (recomendado Ladybug o superior).
* JDK 11 o superior.
* Dispositivo físico o emulador con Android 7.0 (API 24) o superior.
* Conexión a internet activa para conectar con la base de datos.

### Pasos para probar la app
1. Clona este repositorio en tu ordenador.
2. Abre la carpeta del proyecto desde Android Studio.
3. Espera a que termine de sincronizar Gradle.
4. Dale al play (`Run 'app'`) para compilar e instalar en tu emulador o móvil.

> **⚠️ Aviso sobre el servidor (Render):** El backend está alojado en la capa gratuita de Render, por lo que si lleva un rato sin usarse, el servidor se "duerme". **La primera vez que abras la app o intentes hacer login puede tardar unos 40-50 segundos** en responder mientras el servidor despierta. Después de eso, irá rapidísimo.

## 🛡️ Cuentas de Prueba

Para facilitar la corrección del proyecto, la base de datos ya tiene estas cuentas preparadas con diferentes roles:

**Cuenta de Administrador (Permite escanear QR de acceso):**
* **Email:** admin@wishport.com
* **Password:** admin123 

**Cuenta de Usuario Normal (Permite ver pistas y reservar):**
* **Email:** usuario@prueba.com
* **Password:** 1234
*(También puedes crear una cuenta nueva directamente desde la pantalla de registro de la app).*

## 🛠️ Stack Tecnológico
* **Aplicación Android:** Desarrollada en Java usando arquitectura MVVM, Hilt para inyección de dependencias y Retrofit 2 para consumo de API. Las contraseñas y tokens JWT se guardan seguros con *EncryptedSharedPreferences*.
* **Backend:** Spring Boot (Java) con Spring Security para proteger las rutas.
* **Base de Datos:** MySQL 8 alojada en la nube (Aiven Cloud).
* **Despliegue:** API alojada mediante contenedores en Render.

## 🔮 Mejoras a Futuro
El proyecto tiene una base sólida y modular, por lo que está preparado para añadirle funcionalidades extra más adelante:
1. **Notificaciones Push:** Integrar Firebase (FCM) para avisar al usuario una hora antes de su partido.
2. **Geolocalización:** Usar la API de Google Maps para mostrar dónde está el club y cómo llegar.
3. **Pagos Reales:** Cambiar la actual simulación de pago por la integración oficial del SDK de Stripe.
4. **Sincronización de Calendario:** Que la reserva se guarde automáticamente en el Google Calendar del móvil.
