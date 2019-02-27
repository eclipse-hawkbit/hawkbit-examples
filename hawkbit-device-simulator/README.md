# hawkBit GCP Device Simulator


## Spin a VM

install the following:
- git
- java openjdk8
- docker
- maven
- create a service account
- add the service account to the VM in the configuration 

## First Credentials for GCP
- use the same service account
- Create a json file [link](https://docs.cloudendure.com/Content/Generating_and_Using_Your_Credentials/Working_with_GCP_Credentials/Generating_the_Required_GCP_Credentials/Generating_the_Required_GCP_Credentials.htm)

- Rename the downloaded file to `keys.json`

- Add it to `src/main/resources`


## GCP Config

- Set the projectId and the cloud region in the GCP_OTA.java
- Create a `state` subscription on the state topic
- Create a bucket: gsutil mb gs:/firmware-ota/
- enable the Token Service API: `cloud iot token`


# hawkBit Device Simulator

The device simulator handles software update commands from the update server. It is designed to be used very conveniently, 
for example, from within a browser. Hence, all the endpoints use the GET verb.
-Dhawkbit.device.simulator.amqp.enabled=true

# Open Ports

- 8080/tcp —> hawkbit   
- 8083/tcp —> gcp manager
- 3306/tcp, 33060/tcp  —> Mysql
- 4369/tcp, 5671-5672/tcp, 15671-15672/tcp, 25672/tcp  —> rabbitMQ

# docker on debian

if you had any difficulty installing docker compose follow the following

1- Open `docker-compose-stack.yml` and remove the hawkBit simulator part, since we want to run the GCP Manager on the same port
```
  
    image: "hawkbit/hawkbit-device-simulator:latest"
    networks:
    - hawknet
    ports:
    - "8083:8083"
    deploy:
      restart_policy:
        condition: on-failure
    environment:
    - 'HAWKBIT_DEVICE_SIMULATOR_AUTOSTARTS_[0]_TENANT=DEFAULT'
    - 'SPRING_RABBITMQ_VIRTUALHOST=/'
    - 'SPRING_RABBITMQ_HOST=rabbitmq'
    - 'SPRING_RABBITMQ_PORT=5672'
    - 'SPRING_RABBITMQ_USERNAME=guest'
    - 'SPRING_RABBITMQ_PASSWORD=guest'
```

2 - Run the following to start it
```
sudo docker swarm init
sudo docker stack deploy -c docker-compose-stack.yml hawkbit
```


## MySQL Info
  MYSQL_DATABASE: "hawkbit"
      MYSQL_USER: "root"
      port : 3306

## Run on your own workstation
```
java -jar examples/hawkbit-device-simulator/target/hawkbit-device-simulator-*-SNAPSHOT.jar
```
Or:
```
run org.eclipse.hawkbit.simulator.DeviceSimulator
```

## Deploy to cloud foundry environment

- Go to ```target``` subfolder.
- Run ```cf push```

## Notes

The simulator has user authentication enabled in **cloud profile**. Default credentials:
*  username : admin
*  passwd : admin

This can be configured/disabled by spring boot properties

## hawkBit APIs

The simulator supports `DDI` as well as the `DMF` integration APIs.

In case there is no AMQP message broker (like rabbitMQ) running, you can disable the AMQP support for the device simulator, so the simulator is not trying to connect to an amqp message broker.

Configuration property `hawkbit.device.simulator.amqp.enabled=false`

## Usage

### REST API
The device simulator exposes an REST-API which can be used to trigger device creation.

Optional parameters:
* name : name prefix simulated devices (default: "dmfSimulated"), followed by counter
* amount : number of simulated devices (default: 20, capped at: 4000)
* tenant : in a multi-tenant ready hawkBit installation (default: "DEFAULT")
* api : the API which should be used for the simulated device either `dmf` or `ddi` (default: "dmf")
* endpoint :  URL which defines the hawkbit DDI base endpoint (default: "http://localhost:8080")
* polldelay : number in seconds of the delay when DDI simulated devices should poll the endpoint (default: "30")
* gatewaytoken : an hawkbit gateway token to be used in case hawkbit does not allow anonymous access for DDI devices (default: "")


Example: for 20 simulated devices by DMF API (default)
```
http://localhost:8083/start
```

Example: for 10 simulated devices that start with the name prefix "activeSim":
```
http://localhost:8083/start?amount=10&name=activeSim
```

Example: for 5 simulated devices that start with the name prefix "ddi" using the Direct Device Integration API (http) authenticated by given gateway token, a pool interval of 10 seconds and a custom port for the DDI service.:
```
http://localhost:8083/start?amount=5&name=ddi&api=ddi&gatewaytoken=d5F2mmlARiMuMOquRmLlxW4xZFHy4mEV&polldelay=10&endpoint=http://localhost:8085
```
