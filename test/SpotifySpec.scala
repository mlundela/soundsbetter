import actors._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.libs.json.{JsValue, Json}


@RunWith(classOf[JUnitRunner])
class SpotifySpec extends Specification {

  "Parsing" should {
    "work" in {
      val html: String = io.Source.fromInputStream(getClass.getResourceAsStream("test/spotify.json")).mkString
      val json: JsValue = Json.parse(html)
      Spotify.parse(json).get === "spotify:track:5T6hai5yeaakZcs1QE8rFD"
    }
  }
}
