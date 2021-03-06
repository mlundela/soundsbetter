package actors

import akka.actor.{ActorRef, Actor}
import akka.pattern.ask
import play.api.libs.ws.Response
import actors.WebCrawler.Get
import scala.concurrent.{ExecutionContext, Future}
import com.fasterxml.jackson.databind.JsonMappingException
import akka.util.Timeout
import akka.event.LoggingReceive
import play.api.libs.json.JsValue

object Spotify {

  def parse(json: JsValue): Option[String] =
    try {
      ((json \ "tracks")(0) \ "href").asOpt[String]
    }
    catch {
      case e: JsonMappingException => None
    }
}

class Spotify(webCrawler: ActorRef) extends Actor {

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  implicit val timeout: Timeout = 30.seconds
  val search = "http://ws.spotify.com/search/1/track.json?q="

  def receive = LoggingReceive {

    case bands: List[String] =>
      val client = sender

      val fSpotify: List[Future[Response]] = bands.map {
        name =>
          (webCrawler ? Get(search + name)).mapTo[Response]
      }

      val fResponses: Future[List[Response]] = Future.sequence(fSpotify)

      fResponses.map {
        responses =>
          val links = responses.map(r => Spotify.parse(r.json))
          client ! links
      }

  }
}
