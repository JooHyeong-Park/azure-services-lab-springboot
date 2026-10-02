FROM docker.io/eclipse-temurin:21-jdk-alpine AS base

USER root

RUN addgroup -g 10000 app && \
    adduser -D -u 10000 -G app -s /bin/sh -h /home/app app && \
    mkdir -p /home/app && \
    chown -R 10000:10000 /home/app

FROM base AS builder

WORKDIR /workspace
USER app

COPY --chown=app:app . .

RUN echo "[INFO] Executing gradle build" && \
    chmod +x ./gradlew && \
    ./gradlew clean build --project-dir . --exclude-task test --refresh-dependencies --no-daemon && \
    echo "[INFO] Check Build Result in 'build/libs'" && \
    ls -al build/libs

FROM base AS runner

ENV TZ=Asia/Seoul

ARG DEPLOY_PATH='/home/app'
ARG JAR_FILE='azure-services-lab-springboot.jar'

COPY --from=builder --chown=10000:10000 \
    /workspace/build/libs/"${JAR_FILE}" \
    "${DEPLOY_PATH}"/app.jar

WORKDIR "${DEPLOY_PATH}"
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
