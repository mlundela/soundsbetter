package controllers

import play.api.mvc._
import play.libs.Akka
import actors.{BergenLive, Spotify, Kvarteret, WebCrawler}
import akka.actor.Props
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import akka.routing.RoundRobinRouter
import models.Event

object Application extends Controller {

  implicit val timeout: Timeout = 30.seconds

  val webCrawler = Akka.system.actorOf(Props[WebCrawler].withRouter(RoundRobinRouter(10)))
  val spotify = Akka.system.actorOf(Props(classOf[Spotify], webCrawler), "spotify")
  val kvarteret = Akka.system.actorOf(Props(classOf[Kvarteret], webCrawler, spotify), "kvarteret")
  val bergenlive = Akka.system.actorOf(Props(classOf[BergenLive], webCrawler, spotify), "Bergenlive")

  def index = Action.async {
    val f1 = (kvarteret ? "get").mapTo[List[(Event, Option[String])]]
    val f = (bergenlive ? "get").mapTo[List[(Event, Option[String])]]
    val list = List(f1, f)
    val l1: Future[List[List[(Event, Option[String])]]] = Future.sequence(list)
    l1.map {
      events =>
        Ok(views.html.index(events.flatten))
    }
  }

}