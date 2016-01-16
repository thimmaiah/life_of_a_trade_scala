package com.lot.trade.model

import java.sql.Date
import slick.driver.MySQLDriver.api._
import com.github.tototoshi.slick.H2JodaSupport._
import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import spray.json.JsString
import spray.json.JsValue
import spray.json.DeserializationException
import org.joda.time.format.ISODateTimeFormat

/**
 * The model class
 */
case class Trade(id: Option[Long],
                 trade_date: DateTime, settlement_date: DateTime,
                 security_id: Long, quantity: Double, price: Double,
                 user_id: Long, order_id: Long, matched_order_id: Long,
                 created_at: Option[DateTime],
                 updated_at: Option[DateTime])

/**
 * DB schema
 */
class TradeTable(tag: Tag) extends Table[Trade](tag, "trades") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def trade_date = column[DateTime]("trade_date")
  def settlement_date = column[DateTime]("settlement_date")
  def security_id = column[Long]("security_id")
  def quantity = column[Double]("quantity")
  def price = column[Double]("price")
  def user_id = column[Long]("user_id")
  def order_id = column[Long]("order_id")
  def matched_order_id = column[Long]("matched_order_id")
  def created_at = column[DateTime]("created_at")
  def updated_at = column[DateTime]("updated_at")

  def * = (id.?, trade_date, settlement_date, security_id, quantity, price, user_id, order_id, matched_order_id, created_at.?, updated_at.?) <> (Trade.tupled, Trade.unapply)
}

/**
 * To convert to and from JSON
 */
object TradeJsonProtocol extends DefaultJsonProtocol {
  import com.lot.utils.CustomJson._
  implicit val tradeFormat = jsonFormat11(Trade)
}

/**
 * Message singleton
 */
object TradeMessage {
  /*
   * These are the messages that the Exchange can receive
   */
  case class New(trade: Trade)
  case class Modify(trade: Trade)
  case class Cancel(trade: Trade)

}

