FROM eclipse-temurin:17-jre

ARG SENTINEL_VERSION=1.8.9

RUN wget --tries=5 --waitretry=3 \
    -O /opt/sentinel-dashboard.jar \
    https://github.com/alibaba/Sentinel/releases/download/${SENTINEL_VERSION}/sentinel-dashboard-${SENTINEL_VERSION}.jar

EXPOSE 8858

ENV JAVA_OPTS=""
ENV SENTINEL_DASHBOARD_PORT=8858

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${SENTINEL_DASHBOARD_PORT:-8858} -Dsentinel.dashboard.auth.username=${SENTINEL_DASHBOARD_USERNAME:-sentinel} -Dsentinel.dashboard.auth.password=${SENTINEL_DASHBOARD_PASSWORD:-sentinel} -jar /opt/sentinel-dashboard.jar"]
