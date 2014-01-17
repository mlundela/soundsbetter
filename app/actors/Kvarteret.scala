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
import scala.concurrent.ExecutionContext
import akka.util.Timeout

object Kvarteret {

  def parse(html: String): List[Event] = {
      var list: List[Event] = List()
      val doc: Document = Jsoup.parse(html)
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
      list
  }

  def band: Event => String =
    e =>
      if (e.name.contains("Up & Coming:"))
        e.name.split(""":""")(1).split( """[\+,]""")(0).trim.replace(" ", "+")
      else
        e.name.split( """[\+,]""")(0).trim.replace(" ", "+")
}

class Kvarteret(webCrawler: ActorRef, spotify: ActorRef) extends Actor {

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  implicit val timeout: Timeout = 30.seconds
  var cache: List[(Event, Option[String])] = List()

  def receive = {
    case "get" =>
      val client = sender
      if (cache.isEmpty) {
        val f = (webCrawler ? Get("http://kvarteret.no/program")).mapTo[Response]
        f.flatMap {
          response =>
            val events = Kvarteret.parse(response.body)
            (spotify ? events.map(Kvarteret.band)).mapTo[List[Option[String]]].map {
              links =>
                cache = events.zip(links)
                client ! cache
            }
        }
      }
      else {
        client ! cache
      }
  }
}
