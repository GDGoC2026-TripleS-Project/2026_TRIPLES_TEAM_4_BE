FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

# ✅ build/libs 안에서 plain.jar 제거 + 결과물을 app.jar로 고정
RUN ls -al /app/build/libs && \
    rm -f /app/build/libs/*-plain.jar && \
    cp /app/build/libs/*.jar /app/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ENV TZ=Asia/Seoul
ENV LANG=C.UTF-8

COPY --from=builder /app/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
