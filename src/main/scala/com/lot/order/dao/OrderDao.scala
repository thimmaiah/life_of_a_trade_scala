package com.lot.order.dao

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import slick.driver.JdbcProfile
import com.lot.utils.DbModule
import scala.concurrent.Future
import com.lot.order.model.OrderTable
import slick.driver.MySQLDriver.api._
import com.lot.order.model.Order
import com.lot.utils.DB._
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import com.lot.order.model.OrderType

object OrderDao extends TableQuery(new OrderTable(_)) {

  val insertQuery = this returning this.map(_.id) into ((order, id) => order.copy(id = Some(id)))
  
  def save(order: Order) : Future[Order] = {
    val action = insertQuery += order
    db.run(action)
  }

  def get(id: Long) = {
    db.run(this.filter(_.id === id).result.headOption)
  }
  

  def update(order: Order): Future[Int] = {
    val now = new DateTime()
    val o:Order = order.copy(updated_at=Some(now))    
    db.run(this.filter(_.id === order.id).update(o))
  }
  
  def delete(order: Order) = {
    db.run(this.filter(_.id === order.id).delete)
  }
  
  def delete(delete_id: Long) = {
    db.run(this.filter(_.id === delete_id).delete)
  }

  def createTables(): Future[Unit] = {
    db.run(DBIO.seq(this.schema.create))
  }

  def list = {
    val allOrders = for {
        o <- this
      } yield (o)
    db.run(allOrders.sortBy(_.id.desc).result)
  }
  
  /**
   * List of unfilled orders
   */
  def unfilled(security_id: Long, buy_sell: String) = {
    val allOrders = for {
        o <- this if (o.security_id == security_id && o.buy_sell == buy_sell)       
      } yield (o)
    db.run(allOrders.sortBy(_.price.desc).result)
  }
  
  def unfilled_buys(security_id: Long) = {
    val allOrders = for {
        o <- this if (o.security_id == security_id && o.buy_sell == OrderType.BUY)       
      } yield (o)
    // TODO - cannot sort by created_at, fix that  
    db.run(allOrders.sortBy(x => (x.price.desc, x.id.desc)).result)
  }
  
  def unfilled_sells(security_id: Long) = {
    val allOrders = for {
        o <- this if (o.security_id == security_id && o.buy_sell == OrderType.SELL)       
      } yield (o)
    // TODO - cannot sort by created_at, fix that  
    db.run(allOrders.sortBy(x => (x.price.asc, x.id.desc)).result)
  }
  
  
  
  def findByQuantity(quantity: Double) = {
    db.run(this.filter(_.quantity === quantity).result)
  }

}
