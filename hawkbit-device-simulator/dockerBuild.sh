mvn clean install
docker build -f ./docker/0.3.0-SNAPSHOT/Dockerfile -t charbull/ota .
docker run -d --name=jetty -p 8083:8083 charbull/ota:latest
