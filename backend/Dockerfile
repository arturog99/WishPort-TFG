# Etapa 1: Construcción (Build)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copiamos todos los archivos del backend al contenedor
COPY . .

# Damos permisos al wrapper de Gradle y compilamos
RUN chmod +x gradlew
RUN ./gradlew build -x test

# Etapa 2: Ejecución (Run)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copiamos el archivo .jar generado en la etapa anterior
COPY --from=build /app/build/libs/*.jar app.jar

# Exponemos el puerto 8080 que usa Spring Boot
EXPOSE 8080

# Comando para encender el servidor
ENTRYPOINT ["java", "-jar", "app.jar"]