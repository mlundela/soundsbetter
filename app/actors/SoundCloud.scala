package actors

import akka.actor.{ActorRef, Actor}
import akka.pattern.ask
import play.api.libs.ws.Response
import actors.WebCrawler.Get
import models.Event
import scala.concurrent.{ExecutionContext, Future}
import com.fasterxml.jackson.databind.JsonMappingException
import akka.util.Timeout
import akka.event.LoggingReceive
import play.api.libs.json.JsValue

object SoundCloud {
  def parse(json : JsValue): Option[String] = {
    try {

      (json(0) \ "uri").asOpt[String]
    }
    catch {
      case e: Exception => None
    }

  }
}

class SoundCloud(webCrawler: ActorRef) extends Actor{

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global



  implicit val timeout: Timeout = 30.seconds
  val clientID = "93aeec858b32e616e30e5cdcb0d49c08"
  val filter = "&filter=streamable&=playback_count&limit=1"
  val search = "http://api.soundcloud.com/tracks.json?client_id=" + clientID +"&q="
  def receive = LoggingReceive {

    case bands : List[String] =>{
      val client = sender

      val fSoundCloud: List[Future[Response]] = bands.map {
        name =>
          (webCrawler ? Get(search + name + filter)).mapTo[Response]
      }
      val fResponses: Future[List[Response]] = Future.sequence(fSoundCloud)

      fResponses.map {
        responses =>
          val links = responses.map(r => SoundCloud.parse(r.json))
          client ! links
      }

    }


  }



}
