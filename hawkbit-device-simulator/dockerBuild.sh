mvn clean install
mvn package
docker build -f ./docker/0.3.0-SNAPSHOT/Dockerfile -t charbel/ota .
docker run --network="docker_default" -v /Users/charbelk/dev/OSS/HawkBit-GCP/hawkbit-gcp-integrator/src/main/resources/:/opt/resources -d --name=HawkBit-GCP -p 8083:8083 charbel/ota:latest --PROJECT_ID=ota-iot-231619 --CLOUD_REGION=us-central1 --REGISTRY_NAME=OTA-DeviceRegistry --BUCKET_NAME=ota-iot-231619.appspot.com --KEYS=/opt/resources/keys.json