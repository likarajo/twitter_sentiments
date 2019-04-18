Twitter Sentiment Analysis

Refer to http://kafka.apache.org/quickstart for setting up Kafka in local machine.
Refer to https://www.elastic.co/downloads for setting up Elasticsearch, Kibana, and Logstash in local machine.

1) Zookeeper: Administrator

Open a new Terminal, get into the KAFKA Home directory

Run
$ bin/zookeeper-server-start.sh config/zookeeper.properties

2) Kafka: Broker

Open a new Terminal, get into the KAFKA Home directory

Run
$ bin/kafka-server-start.sh config/server.properties
(Kafka runs in localhost:9092)

3) Create Scala program jar, and run using arguments

$ sbt
> assembly

$ spark-submit --class ClassName PathToJarFile
    <kafka server and port> <topic> <consumer key> <consumer secret> <access token> <access token secret> [<filters>]"

4) Elastic Search: Distributed, RESTful search and analytics

Open a new Terminal, get into the ELASTICSEARCH Home directory

Run
$ bin/elasticsearch

5) Logstash: Ingest, transform, enrich, and output

Open a new Terminal, get into LOGSTASH Home directory

Create a file logstash-simple.conf with following content:
input {
kafka {
bootstrap_servers => "localhost:9092"
topics => ["new_topic"]
}
}
output {
elasticsearch {
hosts => ["localhost:9200"]
index => "new_topic-index"
}
}

Run
$ bin/logstash -f logstash-simple.conf

6) Kibana: Visualize your data. Navigate the Stack

Open a new Terminal, get into the KIBANA Home directory

Run
$ bin/kibana

Goto http://localhost:5601 in web browser
Search for the appropriate topic index: new_topic-index
Create personal dashboard referring to https://www.elastic.co/guide/en/kibana/current/index.html

===================================

Twitter US Airlines Sentiment Analysis

A sentiment analysis job about the problems of each major U.S. airline.
Twitter data scraped from February of 2015.
To classify positive, negative, and neutral tweets, followed by categorizing negative reasons (such as "late flight" or "rude service").




