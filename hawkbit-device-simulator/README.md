# hawkBit GCP Device Simulator


## Spin a VM

Use the installation script: [vmInstallDependencies.sh](./vmInstallDependencies.sh)

or 
install the following:
### git:
`sudo apt-get install git`

### java 8

- `sudo apt-get install openjdk-8-jdk`

- `sudo update-alternatives --config java`

### docker

Please read the following if you want to know more about how to install it [here](https://docs.docker.com/install/linux/docker-ce/debian/)

- `sudo apt-get install apt-transport-https ca-certificates curl gnupg2 software-properties-common`
- `curl -fsSL https://download.docker.com/linux/debian/gpg | sudo apt-key add -`
- `sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"`
- `sudo apt-get update`
- `sudo apt-get install docker-ce docker-ce-cli containerd.io`
    
### maven

`sudo apt-get install maven`

### create a service account
### add the service account to the VM in the configuration 

## First Credentials for GCP
- use the same service account
- Create a json file [link](https://docs.cloudendure.com/Content/Generating_and_Using_Your_Credentials/Working_with_GCP_Credentials/Generating_the_Required_GCP_Credentials/Generating_the_Required_GCP_Credentials.htm)

- Rename the downloaded file to `keys.json`

- Add it to `src/main/resources`

## Device Registry

For now, this handler supports only one registry

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


## Firebase config
Follow these steps to configurate firebase with the java sdk [steps](https://firebase.google.com/docs/admin/setup)
Generate the file and place it in `src/main/resources` and name it `firebasekeys.json`

## MySQL Info
  MYSQL_DATABASE: "hawkbit"
      MYSQL_USER: "root"
      port : 3306

## Run on your own workstation
```
mvn spring-boot:run
```
or use the the [runSpring.sh](./runSpring.sh)

## Create Software Distribution

## Tag your Devices

## Create a Target Filter

## Configure the Rollout

- Error threshold 0
- Trigger threshold 100 



Follow the same config as in the 
![image](./images/rolloutConfig.png)



## Notes

The simulator has user authentication enabled in **cloud profile**. Default credentials:
*  username : admin
*  passwd : admin

This can be configured/disabled by spring boot properties

## hawkBit APIs

In case there is no AMQP message broker (like rabbitMQ) running, you can disable the AMQP support for the device simulator, so the simulator is not trying to connect to an amqp message broker.

Configuration property `hawkbit.device.simulator.amqp.enabled=false`

## Populate GCP devices
```
http://localhost:8083/gcp
```