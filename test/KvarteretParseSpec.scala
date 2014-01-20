import java.text.SimpleDateFormat
import models.Event
import actors._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import scala.util.matching.Regex
import scala.util.matching.Regex.MatchIterator

@RunWith(classOf[JUnitRunner])
class KvarteretParseSpec extends Specification {

  "Parsing" should {
    "work" in {
      val html: String = io.Source.fromInputStream(getClass.getResourceAsStream("test/kvarteret-source.html")).mkString
      val lemaitre: Event = Event(new SimpleDateFormat("yyyy-MM-dd").parse("2014-01-23"), "Lemâitre", "Kvarteret")
      Kvarteret.parse(html).contains(lemaitre)
    }
    "blah" in {
      val str = "lørdag 21. januar 2014"
      val pattern: Regex = """(\D+) (\d+)\. (\D+) (\d+)""".r
      val m = pattern.findAllIn(str).matchData.next()
      m.group(1) === "lørdag"
      m.group(2).toInt === 21
      m.group(3) === "januar"
      m.group(4).toInt === 2014
    }
  }
}
