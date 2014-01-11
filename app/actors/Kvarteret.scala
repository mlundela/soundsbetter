package actors

import akka.actor.{ActorRef, Actor}
import akka.pattern.ask
import play.api.libs.ws.Response
import actors.WebCrawler.Get
import models.Event
import org.jsoup.nodes.{Element, Document}
import org.jsoup.Jsoup
import java.util.Date
import java.text.SimpleDateFormat
import scala.concurrent.{ExecutionContext, Future}
import com.fasterxml.jackson.databind.JsonMappingException
import akka.util.Timeout

class Kvarteret(webCrawler: ActorRef) extends Actor {

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  implicit val timeout: Timeout = 30.seconds
  var events: List[(Event, Option[String])] = List()

  def receive = {
    case "get" =>
      val client = sender
      if (events.isEmpty) {
        val f = (webCrawler ? Get("http://kvarteret.no/program")).mapTo[Response]
        f.flatMap {
          response =>
            var list: List[Event] = List()
            val doc: Document = Jsoup.parse(response.body)
            val days = doc.getElementsByClass("agenda_day")
            val it = days.iterator()
            while (it.hasNext) {
              val day: Element = it.next()
              val date = day.getElementsByClass("agenda_day_number").text()
              val d: Date = new SimpleDateFormat("yyyy-MM-dd").parse(s"2014-01-$date")
              val children = day.getElementsByClass("agenda_compact_event_wrapper").iterator()
              while (children.hasNext) {
                val child = children.next()
                val name: String = child.getElementsByTag("a").text()
                list = list :+ Event(d, name)
              }
            }

            val spotify = "http://ws.spotify.com/search/1/track.json?q="
            val fSpotify: List[Future[Response]] = list.map(e => (webCrawler ? Get(spotify + e.name.replace(" ", "+"))).mapTo[Response])
            val fResponses: Future[List[Response]] = Future.sequence(fSpotify)

            fResponses.map {
              responses =>
                val links = responses.map {
                  r =>
                    try {
                      ((r.json \ "tracks")(0) \ "href").asOpt[String]
                    }
                    catch {
                      case e: JsonMappingException => None
                    }
                }
                events = list.zip(links)
                client ! events
            }
        }
      } else {
        client ! events
      }
  }
}
