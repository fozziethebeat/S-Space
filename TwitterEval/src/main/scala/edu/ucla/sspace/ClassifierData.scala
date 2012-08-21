package edu.ucla.sspace

case class ClassifierData(numCategories: Int, priorTokenCounts: Double, priorCategoryCounts: Array[Double], categories: Array[Category])
case class Category(instanceCounts: Double, tokenCounts: Map[String, Double])
