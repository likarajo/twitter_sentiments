Open a new Terminal, get into the KAFKA Home directory

Run Zookeeper
> bin/zookeeper-server-start.sh config/zookeeper.properties

Open a new Terminal, get into the KAFKA Home directory

Run Kafka
> bin/kafka-server-start.sh config/server.properties
Kafka runs in localhost:9092

Run the program jar file with the required arguments
> spark-submit --class ClassName PathToJarFile
    <kafka server and port> <topic> <consumer key> <consumer secret> <access token> <access token secret> [<filters>]"

