import scala.io.Source
import java.io.PrintWriter

args.foreach(println)
val word = args(1)
// Read the assignment file.
val lines = Source.fromFile(args(0)).getLines
// Discard the header as we don't care for it.
lines.next
val w = new PrintWriter(args(2))
(for ( (clust, i) <- lines.zipWithIndex;
       item <- clust.split("\\s+").map(_.toInt) ) yield
    (item, "%s.%d".format(word, i))).toList.sorted.map(_._2).foreach(w.println)
w.close
