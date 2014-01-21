package actors

import akka.actor.{ActorLogging, ActorRef, Actor}
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
        list = list :+(d, name)
      }
    }
    list
  }

  def getBandNames(elems: List[(Date, String)]): List[String] = elems.map {
    case (date, name) =>
      if (name.contains("Up & Coming:"))
        name.split( """:""")(1).split( """[\+,]""")(0).trim.replace(" ", "+")
      else
        name.split( """[\+,]""")(0).trim.replace(" ", "+")
  }


}

class Kvarteret(webCrawler: ActorRef, spotify: ActorRef, soundcloud: ActorRef) extends Actor with ActorLogging {

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
            val dateAndNames: List[(Date, String)] = Kvarteret.parse(response.body)

            val fSpotify = (spotify ? Kvarteret.getBandNames(dateAndNames)).mapTo[List[Option[String]]]
            val fSoundcloud = (soundcloud ? Kvarteret.getBandNames(dateAndNames)).mapTo[List[Option[String]]]

            for {
              links1 <- fSpotify
              links2 <- fSoundcloud
            } yield {
              val links: List[(Option[String], Option[String])] = links1.zip(links2)
              cache = dateAndNames.zip(links).map {
                case ((date, name), (linkSpotify, linkSoundcloud)) => Event(date, name, "Kvarteret", linkSoundcloud, linkSpotify)
              }
              client ! cache
            }
            */

        }
      }
      else {
        client ! cache
      }
  }
}
