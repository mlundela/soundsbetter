import java.text.SimpleDateFormat
import models.Event
import actors._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._


@RunWith(classOf[JUnitRunner])
class BergenLiveParseSpec extends Specification {

  "Parsing" should {
    "work" in {
      val html: String = io.Source.fromInputStream(getClass.getResourceAsStream("test/BergenLive-source.html")).mkString
      val kvelertak: Event = Event(new SimpleDateFormat("yyyy-MM-dd").parse("2014-02-06"), "Kvelertak")
      val parse: List[Event] = BergenLive.parse(html)
      parse === List()
      //parse.contains(kvelertak)

    }
  }
}
