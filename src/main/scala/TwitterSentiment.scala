import java.util.Properties

import com.johnsnowlabs.nlp.pretrained.pipelines.en.SentimentPipeline
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.ConfigurationBuilder


object TwitterSentiment {

  def main(args: Array[String]): Unit = {

    if (args.length < 6) {
      System.err.println("Usage: TwitterSentiment <kafka server and port> <topic> <consumer key> <consumer secret> " +
        "<access token> <access token secret> [<filters>]")
      System.exit(1)
    }

    val Array(kafkaServer, topic, consumerKey, consumerSecret, accessToken, accessTokenSecret) = args.take(6)
    val filters = args.takeRight(args.length - 6)

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)
    Logger.getLogger("twitter4j").setLevel(Level.OFF)

    val spark = SparkSession
      .builder()
      .appName("TwitterSentiment")
      .master("local[*]")
      .getOrCreate()

    val sc = spark.sparkContext
    val ssc = new StreamingContext(sc, Seconds(1))

    sc.setLogLevel("Error")

    val cb = new ConfigurationBuilder
    cb.setDebugEnabled(true)
      .setOAuthConsumerKey(consumerKey)
      .setOAuthConsumerSecret(consumerSecret)
      .setOAuthAccessToken(accessToken)
      .setOAuthAccessTokenSecret(accessTokenSecret)

    val auth = new OAuthAuthorization(cb.build)

    val stream = TwitterUtils.createStream(ssc, Some(auth), filters)
      .filter(_.getLang() == "en")

    val statuses = stream.map(status => {

      val pipeline = SentimentPipeline()
      val text = status.getText()
      val sentiment = pipeline.annotate(text)("sentiment").head

      (sentiment, text, status.getUser.getName(), status.getUser.getScreenName(), status.getCreatedAt.toString)

    })

    statuses.foreachRDD { rdd =>

      rdd.foreachPartition { line =>

        val props = new Properties()
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        props.put("bootstrap.servers", kafkaServer)

        val producer = new KafkaProducer[String, String](props)

        line.foreach { elem =>
          producer.send(new ProducerRecord[String, String](topic, null, elem.toString()))
          println(elem.toString())
        }

        producer.flush()
        producer.close()
      }
    }
    ssc.start()
    ssc.awaitTermination()

  }
}