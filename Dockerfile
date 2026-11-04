FROM maven:3.9.8-eclipse-temurin-21@sha256:de18e4e51396ad9266f76669779ed6d6b49fbb0f45ed6f39169d87378d548d7d AS build

WORKDIR /project

COPY pom.xml /project/pom.xml
RUN mvn -B -q -e -DskipTests dependency:go-offline

COPY src /project/src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a

WORKDIR /work

# Correções de segurança da base publicadas depois da imagem entram em cada build.
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get upgrade -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 -g root -d /work -s /sbin/nologin appuser \
    && chown -R 1001:0 /work

COPY --from=build /project/target/*.jar /work/app.jar
RUN chown 1001:0 /work/app.jar

USER 1001

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

CMD ["sh", "-c", "java $JAVA_OPTS -jar /work/app.jar"]
