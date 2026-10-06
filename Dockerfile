#######################################################################
# Stage 1 — build: compila e empacota o jar executável (Maven + JDK 21)
#######################################################################
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# pom + wrapper + fonte. A imagem maven já traz o `mvn`, por isso o build usa
# `mvn` diretamente (o wrapper fica no contexto por paridade com o build local).
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
COPY src src

# -DskipTests: os testes rodam no CI/local; aqui só interessa o pacote.
# O spring-boot-maven-plugin está declarado no pom.xml (build/plugins), então o
# `package` já embute o repackage e gera o jar executável (BOOT-INF + Main-Class).
RUN mvn -B -DskipTests package

#######################################################################
# Stage 2 — runtime: só o JRE 21 + o jar (imagem final enxuta)
#######################################################################
FROM eclipse-temurin:21-jre

WORKDIR /app

# Curinga pega o único jar gerado (o `.jar.original` não casa com `*.jar`).
COPY --from=build /app/target/*.jar app.jar

# O Render injeta a env var PORT e a aplicação lê ${PORT:8080} no
# application.yml; o default 8080 mantém o container utilizável fora do Render.
ENV PORT=8080
EXPOSE 8080

# Exec-form: a JVM recebe os sinais (SIGTERM) diretamente -> parada limpa.
ENTRYPOINT ["java", "-jar", "app.jar"]
