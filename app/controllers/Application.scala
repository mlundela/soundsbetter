package controllers

import play.api.mvc._
import play.libs.Akka
import actors.{Spotify, Kvarteret, WebCrawler}
import akka.actor.Props
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import akka.routing.RoundRobinRouter
import models.Event

object Application extends Controller {

  implicit val timeout: Timeout = 30.seconds

  val webCrawler = Akka.system.actorOf(Props[WebCrawler].withRouter(RoundRobinRouter(10)))
  val spotify = Akka.system.actorOf(Props(classOf[Spotify], webCrawler), "spotify")
  val kvarteret = Akka.system.actorOf(Props(classOf[Kvarteret], webCrawler, spotify), "kvarteret")

  def index = Action.async {
    val f = (kvarteret ? "get").mapTo[List[(Event, Option[String])]]
    f.map {
      events =>
        Ok(views.html.index(events))
    }
  }

}