import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.{DecisionTreeClassifier, NaiveBayes}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature._
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.sql.{Row, SparkSession}

object TwitterUSAirlineSentiment {

  def main(args: Array[String]): Unit = {

    if (args.length < 2) {
      System.err.println("Usage: TwitterUSAirlineSentiment <input file path> <output directory path>")
      System.exit(1)
    }

    val Array(srcDataFile, outputDir) = args.take(2)

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    val debug = false

    val spark = SparkSession
      .builder
      .master("local[*]")
      .appName("Twitter US Airline Sentiment")
      .getOrCreate()

    if (debug) println("Connected to Spark")

    spark.sparkContext.setLogLevel("ERROR")

    /** Load Data */

    // Create a DataFrame from the input data file
    var dataDF = spark.read
      .format("com.databricks.spark.csv") // allows reading CSV files as Spark DataFrames
      .option("delimiter", ",")
      .option("header", "true")
      .option("inferSchema", "true")
      .option("nullValue", "NA") // replace null values with NA
      .load(srcDataFile)
      .withColumnRenamed("airline_sentiment", "sentiment")
      .withColumnRenamed("tweet_id", "id")
      .select("id", "text", "sentiment")

    if (debug) println("Data read into DataFrame | Rows: " + dataDF.count().toString)

    // Remove rows from the DataFrame that have null in text column
    dataDF = dataDF.na.drop(Seq("text"))

    if (debug) println("Dropped rows with null Text | Remaining Rows: " + dataDF.count().toString)

    if (debug) println("Training and Test set formed")

    if (debug) dataDF.show(2)

    /** Set stages Pre-processing */

    //Breaking down the sentence in text column into words
    val tokenizer = new Tokenizer().setInputCol("text").setOutputCol("words")

    // Remove stop-words from the words column
    val remover = new StopWordsRemover().setInputCol(tokenizer.getOutputCol).setOutputCol("cleanWords")

    // Convert words to term-frequency vectors
    val hashingTF = new HashingTF().setInputCol(remover.getOutputCol).setOutputCol("features")
      .setNumFeatures(50)

    // Convert label to numeric format
    val indexer = new StringIndexer().setInputCol("sentiment").setOutputCol("label")

    if (debug) println("Pre-processing stages specified")

    /** Create Pipeline */

    // Create pre-processing timeline with all the steps
    val pipeline = new Pipeline().setStages(Array(tokenizer, remover, hashingTF, indexer))

    if (debug) println("Pipeline created")

    /** Transform the dataset to pre-process */

    dataDF = pipeline.fit(dataDF).transform(dataDF)

    if (debug) println("Pre-processed dataset")

    if (debug) dataDF.show(2)
    if (debug) dataDF.dtypes.foreach(println)

    /** Specify Model */

    val dt = new DecisionTreeClassifier()
      .setImpurity("entropy")

    val nb = new NaiveBayes()
      .setModelType("multinomial")

    if (debug) println("Model specified")

    /** Create Parameter builder for Hyper-parameter tuning */

    val paramGrid = new ParamGridBuilder()
      //.addGrid(dt.maxDepth, Array(3, 5, 10))
      .addGrid(nb.smoothing, Array(0.01, 0.1, 1.0, 10))
      .build()

    if (debug) println("Parameter grid built")

    /** Set evaluator */

    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol("label")
      .setPredictionCol("prediction")
      // "f1" (default), "weightedPrecision", "weightedRecall", "accuracy"
      .setMetricName("accuracy")

    /** Find best model */

    val cv = new CrossValidator()
      .setEstimator(nb)
      .setEvaluator(evaluator)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(5)
      .setParallelism(3) // Evaluate up to 3 parameter settings in parallel

    if (debug) println("Cross validator set for finding best model parameters")

    /** Split the data into training and test sets */

    var Array(training, test) = dataDF.randomSplit(Array(0.9, 0.1), seed = 11L)

    /** Training with Best Model */

    // Run cross-validation, and choose the best set of parameters.
    val cvModel = cv.fit(training)

    if (debug) println("Model trained")

    /** Make predictions */

    // Make predictions on test set. cvModel uses the best model found.
    val prediction = cvModel.transform(test)

    if (debug) println("Predictions made on test set")

    prediction.select("text", "label", "prediction")
      .collect()
      .foreach { case Row(text: String, label: Double, prediction: Double) =>
        println(s"prediction=$prediction, label=$label <- $text")
      }

    /** Evaluate Model */

    val accuracy = evaluator.evaluate(prediction)
    println(s"Accuracy is: $accuracy")

    spark.stop()

    if (debug) println("Disconnected from Spark")

  }


}
