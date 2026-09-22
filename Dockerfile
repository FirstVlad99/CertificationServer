# syntax=docker/dockerfile:1.4
FROM amazoncorretto:25-alpine AS builder

RUN apk add --no-cache maven

WORKDIR /compile

COPY pom.xml .
COPY src ./src
#COPY src/main/resources/components ./components

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests

FROM amazoncorretto:25-alpine

WORKDIR /app
COPY --from=builder /compile/target/*.jar /app/service.jar

ENTRYPOINT ["java","-jar","/app/service.jar"]