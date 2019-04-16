name := "TwitterSentiment"

version := "0.1"

scalaVersion := "2.11.8"

val sparkVersion = "2.4.0"

libraryDependencies += "org.apache.spark" %% "spark-streaming" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion
libraryDependencies += "org.apache.bahir" %% "spark-streaming-twitter" % "2.3.2"
libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-mllib" % sparkVersion
libraryDependencies += "com.johnsnowlabs.nlp" %% "spark-nlp" % "1.8.0"
libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "3.5.1"
libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "3.5.1" classifier "models"

resolvers ++= Seq(
  "Akka Repository" at "http://repo.akka.io/releases/",
  "Ooyala Bintray" at "http://dl.bintray.com/ooyala/maven"
)