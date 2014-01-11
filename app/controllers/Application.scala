package controllers

import play.api.mvc._
import play.libs.Akka
import actors.WebCrawler
import akka.actor.Props
import akka.pattern.ask
import actors.WebCrawler.Get
import play.api.libs.ws.Response
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Document}
import models.Event
import java.text.SimpleDateFormat
import java.util.Date
import com.fasterxml.jackson.databind.JsonMappingException

object Application extends Controller {

  implicit val timeout: Timeout = 30.seconds

  val webCrawler = Akka.system.actorOf(Props[WebCrawler])

  def index = Action.async {
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
            list = Event(d, name) :: list
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
            Ok(views.html.index(list.zip(links)))
        }
    }
  }

}