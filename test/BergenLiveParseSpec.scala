import java.text.SimpleDateFormat
import models.Event
import actors._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import scala.util.matching.Regex


@RunWith(classOf[JUnitRunner])
class BergenLiveParseSpec extends Specification {

  "Parsing" should {

    "work" in {
      val html: String = io.Source.fromInputStream(getClass.getResourceAsStream("test/BergenLive-source.html")).mkString
      val kvelertak: Event = Event(new SimpleDateFormat("yyyy-MM-dd").parse("2014-02-06"), "Kvelertak")
      val parse: List[Event] = BergenLive.parse(html)
      //parse === List()
      parse.contains(kvelertak)

    }

    "parseTest" in {
      val s = "søndag 2. februar 2014"
      //val s =   "søndag 2. februar 2014"
      //BergenLive.parseDate(s) === "2014-01-02"

      val pattern: Regex = """(\D+) (\d+)\. (\D+) (\d+)""".r
      val m = pattern.findAllIn(s).matchData.next()
      m.group(1) === "søndag"
      m.group(2).toInt === 2
      m.group(3) === "februar"
      m.group(4).toInt === 2014
    }
  }
}
