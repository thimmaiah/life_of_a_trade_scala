package com.lot.utils

import com.typesafe.scalalogging.LazyLogging
import akka.actor.ActorSystem
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import spray.http._
import spray.client.pipelining._
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }


class WebSocket extends LazyLogging {

  val apiLocation = "http://localhost:9292/faye" //Make sure Faye is running on this port
  val timeout = 5.seconds

  //Spray needs an implicit ActorSystem and ExecutionContext
  implicit val system = ActorSystem("WebSocket")  
  
}

/**
 * Publishes TriggeredEvents
 */
object TriggeredEventPublisher extends WebSocket {

  import com.lot.marketEvent.model.TriggeredEvent
  import com.lot.marketEvent.model.TriggeredEventJsonProtocol._
  import system.dispatcher
  
  case class FayeTriggeredEventMessage(channel: String, data: TriggeredEvent)

  implicit val triggeredEventMsgFormat = jsonFormat2(FayeTriggeredEventMessage)

  def publishTriggerEvent(e: TriggeredEvent) = {
    logger.debug(s"Publishing $e on $apiLocation/triggered_events")
    val pipeline: HttpRequest => Future[FayeTriggeredEventMessage] = sendReceive ~> unmarshal[FayeTriggeredEventMessage]
    val f = pipeline(Post(s"$apiLocation", FayeTriggeredEventMessage("/triggered_events", e)))
    val robot = Await.result(f, timeout)
    println(s"got response $robot")
  }
  
}

/**
 * Publishes Price events
 */
object PricePublisher extends WebSocket {

  import com.lot.security.model.Security
  import com.lot.security.model.SecurityJsonProtocol._  
  import system.dispatcher
  
  case class FayePriceEventMessage(channel: String, data: Security)

  implicit val pricingEventMsgFormat = jsonFormat2(FayePriceEventMessage)

  def publishPriceEvent(e: Security) = {
    logger.debug(s"Publishing $e on $apiLocation/pricing_events")
    val pipeline: HttpRequest => Future[FayePriceEventMessage] = sendReceive ~> unmarshal[FayePriceEventMessage]
    val f = pipeline(Post(s"$apiLocation", FayePriceEventMessage("/pricing_events", e)))
    val robot = Await.result(f, timeout)
    println(s"got response $robot")
  }
}