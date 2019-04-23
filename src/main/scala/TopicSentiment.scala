import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.ConfigurationBuilder

object TopicSentiment {

  /** Create pipeline of Stanford CoreNLP */
  val props = new Properties()
  props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
  val pipeline = new StanfordCoreNLP(props)

  /** Create method for obtaining sentiment of a text */
  def getSentiment(text: String): String = {
    var mainSentiment = 0
    if (text != null && text.length() > 0) {
      var longest = 0
      val annotation = pipeline.process(text)
      val list = annotation.get(classOf[CoreAnnotations.SentencesAnnotation])
      val it = list.iterator()
      while (it.hasNext) {
        val sentence = it.next()
        val tree = sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])
        val sentiment = RNNCoreAnnotations.getPredictedClass(tree)
        val partText = sentence.toString()
        if (partText.length() > longest) {
          mainSentiment = sentiment
          longest = partText.length()
        }
      }
    }
    if (mainSentiment < 2)
      "Negative"
    else if (mainSentiment == 2)
      "Neutral"
    else
      "Positive"
  }

  /** Define the main method **/
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
      .builder()//.master("local[*]")
      .appName("TwitterSentiment")
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
      val text = status.getText()
      val sentiment = getSentiment(text)
      sentiment
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