package com.lot.order.model

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
import java.sql.Timestamp
import com.lot.security.model.SecurityTable
import com.lot.utils.CustomJson
import com.lot.security.model.Security



case class Order(id: Option[Long],
                 exchange: String,
                 buy_sell: String,
                 order_type: String,
                 user_id: Long = 0,
                 security_id: Long,
                 ticker: String,
                 quantity: Double,
                 var unfilled_qty: Double = 0,
                 price: Double = 0,
                 var pre_trade_check_status: String = "", // InadequateCurrentBalance
                 var trade_status: String = "", // Filled, PartiallyFilled
                 var status:String = "", //Active, Inactive, Cancelled 
                 created_at: Option[DateTime],
                 updated_at: Option[DateTime]) {
    
  def setUnfilledQty(q: Double) = {
    unfilled_qty = q
    unfilled_qty match {
      case 0 => trade_status = "Filled"
      case _ => trade_status = "Partially Filled"
    }
  }
  
}

case class OrderSec(order: Order, sec: Security)

object OrderType {
  val BUY = "BUY"
  val SELL = "SELL"
  val LIMIT = "LIMIT"
  val MARKET = "MARKET"
  val CANCELLED = "CANCELLED"
}



class OrderTable(tag: Tag) extends Table[Order](tag, "orders") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def exchange = column[String]("exchange", O.Length(10,varying=true))
  def buy_sell = column[String]("buy_sell", O.Length(5,varying=true))
  def order_type = column[String]("order_type", O.Length(10,varying=true))
  def user_id = column[Long]("user_id")
  def security_id = column[Long]("security_id")
  def ticker = column[String]("ticker", O.Length(6,varying=true))
  def quantity = column[Double]("quantity")
  def unfilled_qty = column[Double]("unfilled_qty")
  def price = column[Double]("price")
  def pre_trade_check_status = column[String]("pre_trade_check_status", O.Length(20,varying=true))
  def trade_status = column[String]("trade_status", O.Length(20,varying=true))
  def status = column[String]("status", O.Length(20,varying=true))
  def created_at = column[DateTime]("created_at", O.Nullable)
  def updated_at = column[DateTime]("updated_at", O.Nullable)
  def * = (id.?, exchange, buy_sell, order_type, user_id, security_id, ticker, quantity, unfilled_qty, price, pre_trade_check_status, trade_status, status, created_at.?, updated_at.?) <> (Order.tupled, Order.unapply)
  
  def security = foreignKey("SEC_FK", security_id, TableQuery[SecurityTable])(_.id, onUpdate=ForeignKeyAction.Restrict)
  
}



object OrderJsonProtocol extends CustomJson {
  implicit val orderFormat = jsonFormat15(Order)
  implicit val secFormat = jsonFormat12(Security)
  implicit val ordersecFormat = jsonFormat2(OrderSec)
}

