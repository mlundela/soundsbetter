import java.text.SimpleDateFormat
import java.util.Date
import actors._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._


@RunWith(classOf[JUnitRunner])
class KvarteretParseSpec extends Specification {


  "Parsing" should {
    "work" in {
      val html: String = io.Source.fromInputStream(getClass.getResourceAsStream("test/kvarteret-source.html")).mkString
      val date: Date = new SimpleDateFormat("yyyy-MM-dd").parse("2014-01-23")
      Kvarteret.parse(html).contains(date -> "Lemâitre")
    }
    "band" in {
      val bands: List[(Date, String)] = List((new Date(), "A"), (new Date(), "B"))
      val mapped: List[String] = Kvarteret.getBandNames(bands)
      mapped === List("A", "B")
    }
  }
}
