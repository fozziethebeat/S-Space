import scala.io.Source

val labelMap = (Source.fromFile(args(0)).getLines.map{ line =>
    val Array(word, id, label) = line.split("\\s+")
    (id, label)}).toMap
for (header <- Source.fromFile(args(1)).getLines)
    println(labelMap.get(header).get)
