import java.text.SimpleDateFormat
import models.Event
import actors._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._

@RunWith(classOf[JUnitRunner])
class KvarteretParseSpec extends Specification {

  "Parsing" should {
    "work" in {
      val html: String = io.Source.fromInputStream(getClass.getResourceAsStream("test/kvarteret-source.html")).mkString
      val lemaitre: Event = Event(new SimpleDateFormat("yyyy-MM-dd").parse("2014-01-23"), "Lemâitre")
      Kvarteret.parse(html).contains(lemaitre)
    }
  }
}
