FROM openjdk:8-jre
FROM jetty

MAINTAINER Charbel Kaed <charbelk@google.com>

EXPOSE 8083

ADD target/hawkbit-gcp-iot-manager-0.3.0-SNAPSHOT.jar /opt/hawkbit-gcp-iot-manager-0.3.0-SNAPSHOT.jar

ENTRYPOINT ["java","-jar","/opt/hawkbit-gcp-iot-manager-0.3.0-SNAPSHOT.jar"]