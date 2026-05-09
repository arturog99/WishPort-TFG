WishPort
Aplicación desarrollada como Trabajo de Fin de Grado (TFG) para la gestión y reserva de pistas deportivas en tiempo real. El proyecto está formado por una app Android desarrollada en Java y una API REST construida con Spring Boot y MySQL.
Funcionalidades
    • Registro e inicio de sesión de usuarios. 
    • Consulta de pistas deportivas disponibles. 
    • Creación y cancelación de reservas. 
    • Validación de horarios para evitar solapamientos. 
    • Generación y lectura de códigos QR para reservas. 
    • Panel de administración para gestión de usuarios y pistas. 
    • Conexión en tiempo real entre aplicación móvil y backend. 
Tecnologías utilizadas
Frontend
    • Java 
    • Android SDK 
    • Material Design 
    • Retrofit 
    • Glide 
    • ZXing 
Backend
    • Spring Boot 
    • Spring Security 
    • MySQL 
    • JPA / Hibernate 
    • BCrypt 
    • Gradle 
Requisitos
    • Android Studio 
    • JDK 17 o superior 
    • Gradle 8+ 
    • MySQL 8 o acceso a la base de datos configurada 
    • Dispositivo Android o emulador (API 24+) 
Instalación
1. Clonar el proyecto
git clone <URL_DEL_REPOSITORIO>

2. Backend
Entrar en la carpeta del backend y ejecutar:
./gradlew bootRun

El servidor se iniciará en:
http://localhost:8080

Configurar las credenciales de la base de datos en application.properties o mediante variables de entorno: 

DB_USERNAME=usuario
DB_PASSWORD=contraseña

3. Frontend Android
Abrir el proyecto en Android Studio y comprobar que la URL base de Retrofit coincide con la del backend:
http://10.0.2.2:8080/
Después, ejecutar la aplicación en un emulador o dispositivo físico.
Estructura del proyecto
WishPort/
│
├── backend/         # API REST Spring Boot
└── frontend/        # Aplicación Android

API REST
Usuarios
    • Registro 
    • Inicio de sesión 
    • Consulta de usuarios 
Pistas
    • Listado de pistas disponibles 
Reservas
    • Crear reservas 
    • Consultar reservas 
    • Cancelar reservas 
    • Filtrar por usuario y fecha 
Seguridad
Actualmente el sistema utiliza:
    • Contraseñas encriptadas con BCrypt. 
    • Validación de usuarios únicos. 
    • Conexión SSL para la base de datos. 
Mejoras futuras
    • Implementación de JWT. 
    • Sistema de notificaciones. 
    • Dashboard de administración avanzado. 
    • Pasarela de pagos. 
    • Publicación en dispositivos móviles. 
Autor
Proyecto desarrollado por Arturo, Antonio y Sergio como Trabajo de Fin de Grado.
