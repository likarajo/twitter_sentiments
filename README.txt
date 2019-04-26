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

3) Elastic Search: Distributed, RESTful search and analytics

Open a new Terminal, get into the ELASTICSEARCH Home directory

Run
$ bin/elasticsearch

4) Logstash: Ingest, transform, enrich, and output

Open a new Terminal, get into LOGSTASH Home directory

Create a file logstash-simple.conf with following content:
input {
kafka {
bootstrap_servers => "localhost:9092"
topics => ["lemonade"]
}
}
output {
elasticsearch {
hosts => ["localhost:9200"]
index => "lemonade-index"
}
}

Run
$ bin/logstash -f logstash-simple.conf

5) Create Scala program jar, and run using arguments

$ sbt
> assembly

$ spark-submit --class ClassName PathToJarFile
    <kafka server and port> <topic> <consumer key> <consumer secret> <access token> <access token secret> [<filters>]"

6) Kibana: Visualize your data. Navigate the Stack

Open a new Terminal, get into the KIBANA Home directory

Run
$ bin/kibana

Goto http://localhost:5601 in web browser
Search for the appropriate topic index: lemonade-index
Create personal dashboard referring to https://www.elastic.co/guide/en/kibana/current/index.html

Sample Report:

===================================

Twitter US Airlines Sentiment Analysis

Data downloaded from kaggle: https://www.kaggle.com/crowdflower/twitter-airline-sentiment
Twitter data scraped from February of 2015.

Model built to predict the classification of a tweet as positive, negative, and neutral.

1) Upload the data/Tweets.csv to Amazon S3 -> s3://bucket/data/Tweets.csv

2) Create and Upload the jar file to Amazon S3 -> s3://bucket/twittersentiment_2.11-0.1.jar

3) Create an Amazon EMR cluster having Spark 2.4.0

4) Create a job in the cluster and add details:

class name: USAirlineSentiment
path to jar: s3://bucket/twittersentiment_2.11-0.1.jar
argument1 (input file path): s3://bucket/data/Tweets.csv
argument2 (output directory path): s3://bucket

$ spark-submit --deploy-mode cluster
    --class USAirlineSentiment
    s3://bucket/twittersentiment_2.11-0.1.jar
    s3://bucket/data/Tweets.csv
    s3://bucket

Sample output: output_ThuApr2517:44:58CDT2019.txt

'''
Data loaded
Data Pre-processed
Data Pre-processed
Running cross-validation to choose the best model...
Best model found
{
	logreg_bfd22c98ca3c-elasticNetParam: 0.7,
	logreg_bfd22c98ca3c-maxIter: 35,
	logreg_bfd22c98ca3c-regParam: 0.005,
	pipeline_c74df14ce447-stages: [Lorg.apache.spark.ml.PipelineStage;@4c683777
}
Accuracy is: 0.6648389307745031
'''





