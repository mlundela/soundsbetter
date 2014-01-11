package actors

import akka.actor.Actor
import akka.pattern.pipe
import play.api.libs.ws.{Response, WS}
import play.mvc.Http.Request
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import akka.event.LoggingReceive

object WebCrawler {

  case class Get(url: String)

}

class WebCrawler extends Actor {

  import WebCrawler._

  def receive = LoggingReceive {
    case Get(url) =>
      val response: Future[Response] = WS.url(url).get()
      response pipeTo sender


  }

}
