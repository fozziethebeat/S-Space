package edu.ucla.sspace

import com.codahale.jerkson.Json._

import edu.ucla.sspace.util.StringProbabilityCounter

import scala.collection.JavaConversions.iterableAsScalaIterable

import java.io.File
import java.io.PrintWriter


/**
 * Lifted from Edwin Chen's Ruby implementation of a Naive Bayes classifier for detecting English only tweets.
 */
class NaiveBayesClassifier(val numCategories: Int,
                           val priorTokenCounts: Double,
                           val priorCategoryCounts: Array[Double]) {
    // For each category, keep a count of the number of times we've seen each token in that category.  The counter will implicityly also
    // store the total number of tokens seen in the category to allow for easy normalization.
    val tokenCounts = Array.fill(numCategories)(new StringProbabilityCounter())
    // For each category, keep a count of the number of times we've seen each category.
    val categoryCounts = Array.fill(numCategories)(0d)

    /**
     * Trains on a single data instance with a given label and the likelihood of the label.
     */
    def train(instance: Seq[String], label: Int, likelihood: Double) {
        val categoryTokenCounts = tokenCounts(label)
        for (token <- instance)
            categoryTokenCounts.count(token, likelihood)
        categoryCounts(label) += likelihood
    }

    /**
     * Trains the {@code NaiveBayesClassifier} using the given labels and returns a classifier with trained weights.
     */
    def trainLabeled(instances: Seq[Seq[String]], labels: Seq[Int]) = {
        instances.zip(labels).foreach{ case(instance, label) =>
            train(instance, label, 1d)}
        this
    }

    /**
     * Trains the {@code NaiveBayesClassifier} using Expectation Maximization.  A new instance of the classifier with trained weights will
     * be returned after {@code maxEpochs} have been run.
     */
    def trainEM(instances: Seq[Seq[String]], maxEpochs: Int) = {
        (0 until maxEpochs).foldLeft(parameterCopy)( (oldClassifier, epoch) => {
            System.err.println("Running epoch [%d]".format(epoch))
            val newClassifier = parameterCopy
            for (instance <- instances)
                // Classify each instance with the old classifier.  Then for each category given a probability, train a new classifier using
                // those lablels.
                oldClassifier.posteriorCategoryProbability(instance)
                             .zipWithIndex
                             .foreach{
                    case(likelihood, label) => newClassifier.train(instance, label, likelihood)}
            // Return the new classifier
            newClassifier
        })
    }

    /**
     * Returns the label index of the most probable class for the given instance.
     */
    def classify(instance: Seq[String]) =
        posteriorCategoryProbability(instance).zipWithIndex.max._2

    /**
     * Computes the probability of each category for a given instance.
     */
    def posteriorCategoryProbability(instance: Seq[String]) = {
        val unnormalizedPosteriorProbs = (0 until numCategories).map(category =>
            instance.map(tokenProbability(_, category)).foldLeft(1d)(_*_) * priorCategoryProbability(category))
        val normalization = unnormalizedPosteriorProbs.sum
        if (normalization == 0d) unnormalizedPosteriorProbs.map(_/1d)
        else unnormalizedPosteriorProbs.map(_/normalization)
    }

    /**
     * Computes the likelihood of each token in an instance.
     */
    def tokenProbability(token: String, label: Int) = {
        val denom = tokenCounts(label).sum + tokenCounts(label).size * priorTokenCounts
        if (denom == 0d) 0d
        else (tokenCounts(label).getCount(token) + priorTokenCounts) / denom
    }

    /**
     * Computes the probability of a given category.
     */
    def priorCategoryProbability(label:Int) = {
        val denom = (categoryCounts.sum + priorCategoryCounts.sum).toDouble
        if (denom == 0d) 0d
        else (categoryCounts(label) + priorCategoryCounts(label))/denom
    }

    def parameterCopy = new NaiveBayesClassifier(numCategories, priorTokenCounts, priorCategoryCounts)
}

object NaiveBayesClassifier {
    def save(classifier: NaiveBayesClassifier, jsonFile: String) {
        val classifierData = classifier.tokenCounts.zipWithIndex.map{ case(tokenCategoryCounts, i) => 
            new Category(classifier.categoryCounts(i),
                         tokenCategoryCounts.map( entry => (entry.getKey.toString, entry.getValue.toDouble)).toMap)}
        val printWriter = new PrintWriter(jsonFile)
        printWriter.print(generate(new ClassifierData(classifier.numCategories,
                                                      classifier.priorTokenCounts, 
                                                      classifier.priorCategoryCounts,
                                                      classifierData)))
        printWriter.close
    }

    def load(jsonFile: String) = {
        val classifierData = parse[ClassifierData](new File(jsonFile))
        val classifier = new NaiveBayesClassifier(classifierData.numCategories, 
                                                  classifierData.priorTokenCounts,
                                                  classifierData.priorCategoryCounts)
        classifierData.categories.zipWithIndex.foreach{ case(category, id) => {
            classifier.categoryCounts(id) = category.instanceCounts
            category.tokenCounts.foreach{ case(word, count) =>
                classifier.tokenCounts(id).count(word, count)}
        }}
        classifier
    }
}
