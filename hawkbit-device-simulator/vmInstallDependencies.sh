echo 'Installing git'
sudo apt-get install git

echo 'Installing jdk 8'
sudo apt-get install openjdk-8-jdk

echo 'Installing maven'
sudo apt-get install maven
sudo update-alternatives --config java

echo 'Installing docker'	
sudo apt-get install apt-transport-https ca-certificates curl gnupg2 software-properties-common
curl -fsSL https://download.docker.com/linux/debian/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io
	
echo 'cloning hawbkit and installing it'
mkdir hawkbit
git clone https://github.com/eclipse/hawkbit.git  
cd hawkbit
mvn clean install

echo 'cloning gcp hawkbit module'
git clone https://github.com/charbull/hawkbit-examples.git
cd hawkbit-examples
mvn clean install
cd hawkbit-device-simulator/
chmod 777 runSpring.sh
#./runSpring.sh

echo 'Creating topic state and subscription state'
gcloud pubsub topics create state
gcloud pubsub subscriptions create --topic state state 

echo 'things you should do manually'
echo '- Creating a service account hawkbit-poc'  
echo '- Get the keys and put it in: hawkbit-examples/hawkbit-device-simulator/src/main/resources'  
  
#sudo docker swarm init
#sudo docker stack deploy -c docker-compose-stack.yml hawkbit