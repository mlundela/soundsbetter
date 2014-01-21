import java.text.SimpleDateFormat
import models.Event
import actors._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

@RunWith(classOf[JUnitRunner])
class SoundcloudSpec extends Specification{


  "Parsing" should {

    "work" in {
      val html: String = io.Source.fromInputStream(getClass.getResourceAsStream("test/soundcloud.json")).mkString

    }
  }


}
