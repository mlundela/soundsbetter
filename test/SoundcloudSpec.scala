import java.text.SimpleDateFormat
import models.Event
import actors._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json.{JsValue, Json}

@RunWith(classOf[JUnitRunner])
class SoundcloudSpec extends Specification{


  "Parsing" should {

    "work" in {
      val html: String = io.Source.fromInputStream(getClass.getResourceAsStream("test/soundcloud.json")).mkString
      val json: JsValue = Json.parse(html)
      SoundCloud.parse(json).get === "http://api.soundcloud.com/tracks/99774949"
    }

    "work2" in {
      val html: String = io.Source.fromInputStream(getClass.getResourceAsStream("test/soundcloud2.json")).mkString
      val json: JsValue = Json.parse(html)
      SoundCloud.parse(json) === None
    }

  }


}
