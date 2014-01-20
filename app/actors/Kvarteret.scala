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

  def parse(html: String): List[(Date, String)] = {
      var list: List[(Date, String)] = List()
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
          list = list :+ (d, name)
        }
      }
      list
  }

  def band: String => String =
    e =>
      if (e.contains("Up & Coming:"))
        e.split(""":""")(1).split( """[\+,]""")(0).trim.replace(" ", "+")
      else
        e.split( """[\+,]""")(0).trim.replace(" ", "+")
}

class Kvarteret(webCrawler: ActorRef, spotify: ActorRef, soundcloud: ActorRef) extends Actor {

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  implicit val timeout: Timeout = 30.seconds
  var cache: List[Event] = List()

  def receive = {
    case "get" =>
      val client = sender
      if (cache.isEmpty) {
        val f = (webCrawler ? Get("http://kvarteret.no/program")).mapTo[Response]
        f.flatMap {
          response =>
            val events: List[(Date, String)] = Kvarteret.parse(response.body)
            (spotify ? events.map(_._2).map(e => Kvarteret.band)).mapTo[List[Option[String]]].map {
              links =>
                cache = events.zip(links).map {
                  case ((date, name), link) => Event(date, name, "Kvarteret", None, link)
                }
                client ! cache
            }
//            (soundcloud ? events.map(Kvarteret.band)).mapTo[List[Option[String]]].map {
//              links =>
//                cache = events.zip(links)
//                client ! cache
//            }
        }
      }
      else {
        client ! cache
      }
  }
}
