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
import scala.collection.mutable.ListBuffer
import com.lot.order.dao.OrderDao
import com.lot.trade.service.TradeGenerator
import akka.routing.FromConfig
import com.lot.trade.service.SecurityManager
import com.lot.position.service.PositionManager
import akka.actor.Terminated
import com.typesafe.config.ConfigValue
import com.typesafe.config.Config

class Exchange(name: String, tradeGenerator: ActorRef, securityManager: ActorRef) extends Actor with ActorLogging {

  import com.lot.exchange.Message._
  implicit val timeout = Timeout(5.seconds)

  var matchers = new HashMap[String, ActorRef]

  override def preStart = {
    tradeGenerator ! "Started"
  }

  /**
   * This simply finds the appropriate matcher and forwards the message
   */
  def receive = {
    case Terminated(deadMatcher) => {
      log.info(s"Matcher terminated: $deadMatcher")
      /*
       * Remove the OrderMatcher from the list of cached matchers
       */
      matchers -= deadMatcher.path.name
    }
    case msg @ NewOrder(order, at) => {
      val matcher = getMatcher(order)
      log.debug(s"Routing message to $matcher")
      matcher ! msg
    }
    case msg @ ModifyOrder(order, at) => { getMatcher(order) ! msg }
    case msg @ CancelOrder(order, at) => { getMatcher(order) ! msg }
    case msg @ StopMatchers => {
      for {
        (path, matcher) <- matchers
      } yield (context.stop(matcher))
    }
    case msg => { log.error(s"Exchange received invalid message $msg") }

  }

  /**
   * Find the matcher for the given security_id in the order
   * @order: The order for which the matcher is required.
   * @return: The ActorRef of the matcher which will match this order
   */
  private def getMatcher(order: Order) = {
    val security_id = order.security_id
    val matcher = matchers.get(getOMName(security_id))
    matcher match {
      // We have a matcher for the give security_id - lets use that
      case Some(m) => m
      // No matcher found - lets create, cache and use
      case None => {
        log.info(s"Creating matcher OrderMatcher-$security_id")
        val m = buildMatcher(security_id)
        matchers += (m.path.name -> m)
        /*
         * We also watch the new matcher and remove it from the matchers hash when it dies
         */
        context.watch(m)
        m
      }
    }
  }

  private def getOMName(security_id: Long) = {
    s"OrderMatcher-$security_id"
  }

  /**
   * Builds an OrderMatcher actor by passing it all the unfilled orders in the DB
   */
  private def buildMatcher(security_id: Long) = {

    val unfilledOM = UnfilledOrderManager(security_id)
    /*
     * Create the OrderMatcher actor
     */
    context.actorOf(Props(classOf[OrderMatcher], security_id, unfilledOM, tradeGenerator, securityManager), getOMName(security_id))
  }

}

/**
 * The place where we startup all exchanges
 */
object Exchange extends ConfigurationModuleImpl with LazyLogging {

  /*
   * Some constants
   */
  val NASDAQ = "NASDAQ"
  val NYSE = "NYSE"

  val system = ActorSystem("lot-om", config)

  /*
   * The actor that manages the positions based on the trades created
   */
  val positionManager = system.actorOf(FromConfig.props(Props[PositionManager]), "positionManagerRouter")

  
  /*
   * The actor that handles trade creation / enrichment for the exchange
   * NOTE: Actually used inside the OrderMatcher post matching to create trades
   */
  val tradeGenerator = system.actorOf(FromConfig.props(Props(classOf[TradeGenerator], positionManager)), "tradeGeneratorRouter")

  /*
   * The actor that handles price update and broadcasting of prices
   */
  val securityManager = system.actorOf(FromConfig.props(Props[SecurityManager]), "securityManagerRouter")

  /*
   * The map of all exchanges and their actorRefs
   */
  var exchanges = new HashMap[String, ActorRef]()
  var exchangeProps = new HashMap[String, Config]()

  /*
   * Create all the exchanges specified in the config
   */
  val exchangeConfig = config.getConfig("exchanges")
  val venues = exchangeConfig.getStringList("venues").listIterator()
  while (venues.hasNext()) {
    val venue = venues.next()
    /*
     * Pass in the exchange name and the tradeGenerator
     */
    val e = system.actorOf(Props(classOf[Exchange], venue, tradeGenerator, securityManager), name = venue)

    logger.info(s"Started exchange $venue on " + e.path)
    exchanges += (venue -> e)
    exchangeProps += (venue -> exchangeConfig.getConfig(venue))
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
  case object StopMatchers
}
