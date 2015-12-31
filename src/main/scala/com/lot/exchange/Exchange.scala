package com.lot.exchange

import scala.concurrent.duration.DurationInt
import com.typesafe.scalalogging.LazyLogging
import akka.actor.Actor
import akka.util.Timeout
import com.lot.utils.Configuration
import com.lot.order.model.Order
import org.joda.time.DateTime
import scala.collection.immutable.HashMap
import akka.actor.Props
import akka.actor.ActorRef
import com.lot.utils.ConfigurationModuleImpl
import com.lot.utils.ActorModuleImpl
import akka.actor.ActorSystem
import scala.collection.immutable.Map
import akka.actor.ActorLogging

class Exchange(name: String) extends Actor with ActorLogging  {

  import com.lot.exchange.Message._
  implicit val timeout = Timeout(5.seconds)

  var matchers = new HashMap[Long, ActorRef]

  /**
   * This simply finds the appropriate matcher and forwards the message
   */
  def receive = {
    case msg @ NewOrder(order, at)    => { getMatcher(order) ! msg }
    case msg @ ModifyOrder(order, at) => { getMatcher(order) ! msg }
    case msg @ CancelOrder(order, at) => { getMatcher(order) ! msg }
    case msg                          => { log.error(s"Exchange received invalid message $msg") }
  }

  /**
   * Find the matcher for the given security_id in the order
   * @order: The order for which the matcher is required.
   * @return: The ActorRef of the matcher which will match this order
   */
  def getMatcher(order: Order) = {
    val security_id = order.security_id
    val matcher = matchers.get(security_id)
    matcher match {
      // We have a matcher for the give security_id - lets use that
      case Some(m) => m
      // No matcher found - lets create, cache and use
      case None => {
        log.info(s"Creating matcher OrderMatcher-$security_id")
        val m = context.actorOf(Props(classOf[OrderMatcher], security_id), s"OrderMatcher-$security_id")
        matchers += (security_id -> m)
        m
      }
    }
  }

}

/**
 * The place where we startup all exchanges
 */
object Exchange extends ConfigurationModuleImpl with LazyLogging {
  
  val NYSE = "NYSE"
  val NASDAQ = "NASDAQ"
  
  var exchanges = new HashMap[String, ActorRef]()
  
  val system = ActorSystem("lot-om", config)
  
  val entries = config.getConfig("exchanges").entrySet().iterator()
  while(entries.hasNext()) {
    val kv = entries.next()
    val key = kv.getKey()
    
    val e = system.actorOf(Props(classOf[Exchange], key), name=key)
    
    logger.info(s"Started exchange $key on " + e.path)
    exchanges += (key -> e)
  }
  
}
/**
 * Message singleton
 */
object Message {
  /*
   * These are the messages that the Exchange can receive
   */
  case class NewOrder(order: Order, at: DateTime)
  case class ModifyOrder(order: Order, at: DateTime)
  case class CancelOrder(order: Order, at: DateTime)

}