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
      val kvelertak: Event = Event(new SimpleDateFormat("yyyy-MM-dd").parse("2014-02-06"), "Kvelertak", "USF Verftet")
      val parse: List[Event] = BergenLive.parse(html)
      //parse === List()
      parse.contains(kvelertak)

    }

    "parseTest" in {
      val s = "s&oslash;ndag 2.februar2014"
      val pattern: Regex = """.* (\d+)\.(\D+)(\d+)""".r
      val m = pattern.findAllIn(s).matchData.next()
      m.group(1).toInt === 2
      m.group(2) === "februar"
      m.group(3).toInt === 2014
    }
  }
}
