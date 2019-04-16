Download Kafka (kafka_2.11-2.2.0.tgz)

Open Terminal and cd to download location

Un-tar the downloaded file
> tar -xzf kafka_2.11-2.2.0.tgz

Get into the directory of the extracted file
> cd kafka_2.11-2.2.0

Run Zookeeper
> bin/zookeeper-server-start.sh config/zookeeper.properties

Open new terminal and go to the kafka_2.11-2.2.0 folder

Run Kafka
> bin/kafka-server-start.sh config/server.properties
Kafka runs in localhost:9092

Run the program jar file with the required arguments
> spark-submit --class ClassName PathToJarFile
    <kafka server and port> <topic> <consumer key> <consumer secret> <access token> <access token secret> [<filters>]"

