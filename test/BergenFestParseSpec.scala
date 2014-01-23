import java.text.SimpleDateFormat
import models.Event
import actors._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import scala.util.matching.Regex


@RunWith(classOf[JUnitRunner])
class BergenFestParseSpec extends Specification {

  "Parsing" should {

    "work" in {
      val html: String = io.Source.fromInputStream(getClass.getResourceAsStream("test/bergenfest-source.html")).mkString
      val zzTop: Event = Event(new SimpleDateFormat("yyyy-MM-dd").parse("2014-06-11"), "ZZ Top", "Bergenhus Festning - Plenen")
      BergenFest.parse(html).contains(zzTop.date, zzTop.name, zzTop.venue)
    }

    "parseTest" in {
      val s = "l&oslash;rdag14.juni2014"
      val pattern: Regex = """\D*(\d+)\.(\D+)(\d+)""".r
      val m = pattern.findAllIn(s).matchData.next()
      m.group(1).toInt === 14
      m.group(2) === "juni"
      m.group(3).toInt === 2014
    }

  }
}
