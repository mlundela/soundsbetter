package controllers

import play.api.mvc._
import play.libs.Akka
import play.api.libs.json.Json
import actors._
import akka.actor.Props
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import akka.routing.RoundRobinRouter
import models.Event

object Application extends Controller {
  implicit val eventFormat = Json.format[Event]
  implicit val timeout: Timeout = 30.seconds
  val webCrawler = Akka.system.actorOf(Props[WebCrawler].withRouter(RoundRobinRouter(10)))
  val spotify = Akka.system.actorOf(Props(classOf[Spotify], webCrawler), "spotify")
  val soundcloud = Akka.system.actorOf(Props(classOf[SoundCloud], webCrawler), "soundcloud")
  val kvarteret = Akka.system.actorOf(Props(classOf[Kvarteret], webCrawler, spotify,soundcloud), "kvarteret")
  val bergenlive = Akka.system.actorOf(Props(classOf[BergenLive], webCrawler, spotify, soundcloud), "bergenlive")
  val bergenfest = Akka.system.actorOf(Props(classOf[BergenFest], webCrawler, spotify, soundcloud), "bergenfest")



  def index = Action.async {
    val f2 = (bergenfest ? "get").mapTo[List[Event]]
    val f1 = (bergenlive ? "get").mapTo[List[Event]]
    //val f = (kvarteret ? "get").mapTo[List[Event]]
    val list = List(f2, f1)
    val l1: Future[List[List[Event]]] = Future.sequence(list)
    l1.map {
      events =>
        Ok(views.html.index(events.flatten))

    }
  }
  def getAllConcerts = Action.async {
    val f2 = (bergenfest ? "get").mapTo[List[Event]]
    val f1 = (bergenlive ? "get").mapTo[List[Event]]
    //val f = (kvarteret ? "get").mapTo[List[Event]]
    val list = List(f2, f1)
    val l1: Future[List[List[Event]]] = Future.sequence(list)
    l1.map {
      events =>
        val eventList : List[Event] = events.flatten
        val event = Json.obj("event" -> eventList)
        Ok(event)
    }
  }

  def test = Action {
    Ok(views.html.test())
  }
}