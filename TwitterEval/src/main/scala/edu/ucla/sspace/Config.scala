package edu.ucla.sspace

import com.codahale.jerkson.Json._

import java.io.File


case class Config(val taggedFile: Option[String],
                  val tokenBasis: Option[String],
                  val neBasis: Option[String],
                  val numGroups: Option[Int],
                  val groupOutput: Option[String],
                  val summaryOutput: Option[String],
                  val featureOutput: Option[String],
                  val splitOutput: Option[String],
                  val featureModel: Option[String],
                  val ngramSize: Option[Int])

object Config {
    def apply(configFile: String) = parse[Config](new File(configFile))
}
