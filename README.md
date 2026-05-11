

# 🎾 WishPort — Gestión de Reservas Deportivas

WishPort es una aplicación nativa para Android conectada a un backend en la nube, diseñada para agilizar el proceso de reserva de instalaciones deportivas y validación de accesos mediante códigos QR.

---

## 📋 Funcionalidades

### 👤 Usuario

* Registro e inicio de sesión seguro con JWT.
* Explorar pistas deportivas disponibles.
* Ver el detalle de cada pista con horarios y estado.
* Reservar una pista en una franja horaria específica.
* Proceso de checkout con simulación de pago.
* Gestionar y cancelar reservas propias.
* Editar el perfil personal.
* Auto-login: la sesión se mantiene entre cierres de la app.

### 🔧 Administrador

* Todas las funcionalidades del usuario estándar.
* Escanear códigos QR para validar el acceso de los usuarios a las instalaciones.
* Panel visual para comprobar las reservas del día en curso.

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
| --- | --- |
| **App Android** | Java 11, MVVM, Hilt, Retrofit 2, LiveData |
| **Seguridad** | JWT, EncryptedSharedPreferences, AuthInterceptor |
| **Escáner QR** | ZXing (zxing-android-embedded) |
| **Backend** | Spring Boot, Spring Security, JPA/Hibernate |
| **Base de Datos** | MySQL 8 (Aiven Cloud) |
| **Despliegue** | Render (contenedores Docker) |

---

## 🏗️ Arquitectura

El proyecto sigue el patrón **MVVM** con separación estricta de responsabilidades:

**Flujo de Datos:**
UI (Activities) ➔ ViewModels ➔ Repositories ➔ ApiService (Retrofit) ➔ Backend REST

**Estructura del Proyecto:**

* `ui/`: Pantallas (Activities) y sus ViewModels correspondientes.
* `data/`: Repositorios y objetos de transferencia (DTOs).
* `api/`: Configuración de Retrofit, Interceptores y gestión de Tokens.
* `models/`: Entidades de negocio (Usuario, Pista, Reserva).
* `di/`: Módulos de inyección de dependencias (Hilt).
* `adapters/`: Controladores para las listas de RecyclerView.

---

## 🚀 Configuración y Ejecución

### Requisitos Previos

* **Android Studio** Ladybug o superior.
* **JDK 11** o superior.
* Dispositivo físico o emulador con **Android 7.0 (API 24)** o superior.
* Conexión a internet activa.

### Pasos para ejecutar

1. Clona este repositorio en tu equipo.
2. Abre la carpeta del proyecto desde Android Studio.
3. Espera a que termine de sincronizar Gradle.
4. Pulsa **Run 'app'** para compilar e instalar en tu emulador o móvil.

> **⚠️ Aviso sobre el servidor Cloud:** El backend está alojado en la capa gratuita de Render, por lo que entra en suspensión tras un periodo de inactividad. **La primera vez que abras la app puede tardar entre 40 y 50 segundos** en responder mientras el servidor despierta. Después funcionará con total fluidez.

---

## 🛡️ Cuentas de Prueba

Para facilitar la evaluación del proyecto, la base de datos cuenta con este perfil preconfigurado:

| Rol | Email | Password |
| --- | --- | --- |
| **Usuario normal** (Reservas) | usuario@prueba.com | prueba1234 |

*(Alternativamente, puedes registrar un nuevo usuario desde la pantalla inicial).*

---

## 🔮 Mejoras a Futuro

El proyecto cuenta con una base sólida y escalable preparada para futuras integraciones:

1. **🔔 Notificaciones Push:** Integración con Firebase (FCM) para enviar recordatorios previos al partido.
2. **📍 Geolocalización:** Uso de Google Maps API para mostrar la ubicación del club y trazar la ruta óptima.
3. **💳 Pagos Reales:** Sustituir la pasarela simulada por el SDK oficial de Stripe.
4. **📅 Sincronización de Calendario:** Exportación automática de la reserva a Google Calendar mediante Intents nativos.
