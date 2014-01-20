                                                   package actors

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.pattern.ask
import play.api.libs.ws.Response
import actors.WebCrawler.Get
import models.Event
import org.jsoup.nodes.{Element, Document}
import org.jsoup.select.{Elements}
import org.jsoup.Jsoup
import java.util.{Locale, Date}
import java.text.SimpleDateFormat
import scala.concurrent.ExecutionContext
import akka.util.Timeout
import scala.util.matching.Regex

object Garage {

  def parse(html: String) : List[Event] = {
    println(html)
      var list: List[Event] = List()
      val doc: Document = Jsoup.parse(html)
      val table: Elements = doc.select("table")
      val it = table.iterator()
      while (it.hasNext) {
        val day: Element = it.next()
        val name = day.getElementsByClass("header").text()
        //val date = day.getElementsByClass("tidspunkt").html().replaceAll("&nbsp;","").trim
        println(name)
        val d: Date = new SimpleDateFormat("yyyy-MM-dd").parse("2014-12-12")
        list = list :+ Event(d, name, "Garage")

       // log.info(list.toString())
      }
      list
  }
  def parseDate(s: String) : String = {
    val pattern: Regex = """.*(\d+)\.(\D+)(\d+)""".r
    val m = pattern.findAllIn(s).matchData.next()
    val dt: String = m.group(3).toString + "-" + findMonrth(m.group(2).toString) + "-" + m.group(1)
    dt
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

class Garage(webCrawler: ActorRef, spotify: ActorRef) extends Actor with ActorLogging{

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  implicit val timeout: Timeout = 30.seconds
  var cache: List[(Event, Option[String])] = List()

  def receive = {
    case "get" =>
      val client = sender
      if (cache.isEmpty) {
        val f = (webCrawler ? Get("http://www.garage.no/")).mapTo[Response]
        f.flatMap {
          response =>
            val events = Garage.parse(response.body)
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
