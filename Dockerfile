# Etapa 1: Compilar usando Gradle
FROM gradle:8.5.0-jdk17 AS builder

WORKDIR /app

# 🔧 Copiar gradlew manualmente primero
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat

# 🔁 Luego copiar el resto del proyecto
COPY . .

# ✅ Mostrar archivos para depurar
RUN echo "📁 Archivos en /app:" && ls -la

# 🔐 Dar permisos de ejecución
RUN chmod +x ./gradlew

# 🛠 Generar stubs gRPC y compilar todo usando Shadow Jar
RUN ./gradlew clean generateProto shadowJar -x check -x test

# 📄 Verifica archivos generados
RUN echo "📄 Archivos gRPC generados:" && find build/generated/source/proto

# Etapa 2: Imagen final y ligera
FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY --from=builder /app/build/libs/app.jar app.jar

EXPOSE 7000
CMD ["java", "-jar", "app.jar"]
