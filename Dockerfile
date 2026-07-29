FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /project

COPY pom.xml /project/pom.xml
RUN mvn -B -q -e -DskipTests dependency:go-offline

COPY src /project/src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /work

RUN useradd -r -u 1001 -g root -d /work -s /sbin/nologin appuser \
    && chown -R 1001:0 /work

COPY --from=build /project/target/*.jar /work/app.jar
RUN chown 1001:0 /work/app.jar

USER 1001

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

CMD ["sh", "-c", "java $JAVA_OPTS -jar /work/app.jar"]
