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
import scala.util.matching.Regex

object BergenFest {

  def parse(html: String) : List[(Date, String, String)] = {
    var list: List[(Date, String, String)] = List()
    val doc: Document = Jsoup.parse(html)
    val days = doc.getElementsByClass("event-list")
    val it = days.iterator()
    while (it.hasNext) {
      val day: Element = it.next()

      val children = day.getElementsByClass("item").iterator()
      while(children.hasNext){
        val child = children.next()
        val name = child.getElementsByClass("title").text().replaceAll("&", "and")
        val date = child.getElementsByClass("tidspunkt").html().replaceAll("&nbsp;","").trim
        val venue = child.getElementsByClass("Scene").html()
        val d: Date = new SimpleDateFormat("yyyy-MM-dd").parse(parseDate(date))
        list = list :+(d, name, venue)
      }
    }
    list
  }
  def parseDate(s: String) : String = {
    val pattern: Regex = """\D*(\d+)\.(\D+)(\d+)""".r
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
  def getBandName(elems : List[(Date, String, String)]) : List[String] = elems.map{
    case (date, name, venue) =>
      name.replaceAll(" ","""+""")
  }
}
class BergenFest(webCrawler: ActorRef, spotify: ActorRef, soundcloud: ActorRef) extends Actor with ActorLogging{

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  implicit val timeout: Timeout = 30.seconds
  var cache: List[Event] = List()

  def receive = {
    case "get" =>
      val client = sender
      if (cache.isEmpty) {
        val f = (webCrawler ? Get("http://bergenfest.no")).mapTo[Response]
        f.flatMap {
          response =>
            val dateAndNames: List[(Date, String, String)] = BergenFest.parse(response.body)

            val fSpotify = (spotify ? BergenFest.getBandName(dateAndNames)).mapTo[List[Option[String]]]
            val fSoundcloud = (soundcloud ? BergenFest.getBandName(dateAndNames)).mapTo[List[Option[String]]]

            for {
              links1 <- fSpotify
              links2 <- fSoundcloud
            } yield {
              val links: List[(Option[String], Option[String])] = links1.zip(links2)
              cache = dateAndNames.zip(links).map {
                case ((date, name, venue), (linkSpotify, linkSoundcloud)) => Event(date, name, venue, linkSoundcloud, linkSpotify)
              }
              client ! cache
            }
        }
      }
      else {
        client ! cache
      }
  }
}


