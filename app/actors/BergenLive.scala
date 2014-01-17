package actors

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.pattern.ask
import play.api.libs.ws.Response
import actors.WebCrawler.Get
import models.Event
import org.jsoup.nodes.{Element, Document}
import org.jsoup.Jsoup
import java.util.{Locale, Date}
import java.text.SimpleDateFormat
import scala.concurrent.ExecutionContext
import akka.util.Timeout
import com.ning.http.util.DateUtil


class BergenLive(webCrawler: ActorRef, spotify: ActorRef) extends Actor with ActorLogging{

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  implicit val timeout: Timeout = 30.seconds
  var cache: List[(Event, Option[String])] = List()

  def parse: Response => List[Event] = {
    response =>
      var list: List[Event] = List()
      val doc: Document = Jsoup.parse(response.body)
      val days = doc.getElementsByClass("event-list")
      val it = days.iterator()
      while (it.hasNext) {
        val day: Element = it.next()

        val children = day.getElementsByClass("item").iterator()
        while(children.hasNext){
          val child = children.next()
          val name = child.getElementsByTag("h4").text()
          val date = child.getElementsByClass("tidspunkt").text().replaceAll("&nbsp;","").trim.split(" ")(1)


          println(date)
          val d: Date = new SimpleDateFormat("yyyy-MM-dd").parse(s"2014-01-12")
          list = list :+ Event(d, name)
        }
        log.info(list.toString())
      }
      list
  }

  def parseDate : String => Date = {
    s =>
      new SimpleDateFormat("yyyy-MM-dd").parse(
        s.split(" ")(2)+ "-" + findMonrth(s.split(" ")(1)) + "-" + s.split(" ")(0).replaceAll(".", "")
      )
  }
  def findMonrth : String => String = {
    s => s.toLowerCase match {
      case "januar" => "01"
      case "februar" => "02"
      case "mars" => "03"
      case "april" => "04"
      case "mai" => "05"
      case "juni" => "06"
      case "juli" => "07"
      case "august" => "08"
      case "september" => "09"
      case "oktober" => "10"
      case "november" => "11"
      case "desember" => "12"
      case _ => null
    }
  }


  def band: Event => String =
    e =>
      if (e.name.contains("Up & Coming:"))
        e.name.split(""":""")(1).split( """[\+,]""")(0).trim.replace(" ", "+")
      else
        e.name.split( """[\+,]""")(0).trim.replace(" ", "+")

  def receive = {
    case "get" =>
      val client = sender
      if (cache.isEmpty) {
        val f = (webCrawler ? Get("http://bergenlive.no/konsertkalender/")).mapTo[Response]
        f.flatMap {
          response =>
            val events = parse(response)
            (spotify ? events.map(band)).mapTo[List[Option[String]]].map {
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
