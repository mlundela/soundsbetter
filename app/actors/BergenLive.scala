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
import scala.util.matching.Regex

object BergenLive {

  def parse(html: String) : List[Event] = {
      var list: List[Event] = List()
      val doc: Document = Jsoup.parse(html)
      val days = doc.getElementsByClass("event-list")
      val it = days.iterator()
      while (it.hasNext) {
        val day: Element = it.next()

        val children = day.getElementsByClass("item").iterator()
        while(children.hasNext){
          val child = children.next()
          val name = child.getElementsByTag("h4").text()
          val date = child.getElementsByClass("tidspunkt").text().replaceAll("&nbsp;","").trim
          val pt = """(\D+) (\d+)\. (\D+) (\d+)""".r

          date match {
            case pt(dd,d,m,y) =>{
              println("Dagen i dag " + dd)
              println("Datoen " + d)
              println("Måned " + m)
              println("År " + y)
            }
          }
          val d: Date = new SimpleDateFormat("yyyy-MM-dd").parse(s"2014-01-12")
          //println(date + "--" + name)
          list = list :+ Event(d, name)
        }
       // log.info(list.toString())
      }
      list
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
}

class BergenLive(webCrawler: ActorRef, spotify: ActorRef) extends Actor with ActorLogging{

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  implicit val timeout: Timeout = 30.seconds
  var cache: List[(Event, Option[String])] = List()

  def receive = {
    case "get" =>
      val client = sender
      if (cache.isEmpty) {
        val f = (webCrawler ? Get("http://bergenlive.no/konsertkalender/")).mapTo[Response]
        f.flatMap {
          response =>
            val events = BergenLive.parse(response.body)
            (spotify ? events.map(BergenLive.band)).mapTo[List[Option[String]]].map {
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
