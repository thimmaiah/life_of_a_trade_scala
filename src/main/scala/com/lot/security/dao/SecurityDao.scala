package com.lot.security.dao

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import slick.driver.JdbcProfile
import com.lot.utils.{ DbModule }
import scala.concurrent.Future
import com.lot.security.model.SecurityTable
import slick.driver.MySQLDriver.api._
import com.lot.security.model.Security
import com.lot.utils.DB._
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTime
import com.lot.marketEvent.model.MarketEvent
import com.lot.marketEvent.model.MarketEvent

object SecurityDao extends TableQuery(new SecurityTable(_)) with LazyLogging {

  /**
   * Saves the Security to the DB
   * @return The Id of the saved entity
   */

  val insertQuery = this returning this.map(_.id) into ((security, id) => security.copy(id = Some(id)))

  def save(security: Security): Future[Security] = {
    val action = insertQuery += security
    db.run(action)
  }

  /**
   * Returns the Security
   * @id The id of the Security in the DB
   */
  def get(id: Long) = {
    db.run(this.filter(_.id === id).result.headOption)
  }
  
  def first() = {
    db.run(this.result.headOption)
  }

  def findByTicker(ticker: String) = {
    db.run(this.filter(_.ticker === ticker).result.headOption)
  }

  /**
   * Updates the Security
   * @security The new fields will be updated in the DB
   */
  def update(security: Security) = {
    // update the updated_at timestamp
    val now = new DateTime();
    val new_security: Security = security.copy(updated_at = Some(now))

    db.run(this.filter(_.id === security.id).update(new_security))
  }

  def updatePrice(security_id: Long, newPrice: Double): Future[Int] = {
    // update the updated_at timestamp
    logger.debug(s"updatePrice $security_id, $newPrice")
    val now = new DateTime();
    db.run( this.filter(_.id === security_id).map(s => s.price).update(newPrice) )
  }

  /**
   * Deletes the security from the DB. Warning this is permanent and irreversable
   * @security This has the id which will be removed from the DB
   */
  def delete(security: Security) = {
    db.run(this.filter(_.id === security.id).delete)
  }

  /**
   * Deletes the security
   * @id The id of the security to be deleted
   */
  def delete(delete_id: Long) = {
    db.run(this.filter(_.id === delete_id).delete)
  }

  /**
   * Creates the tables
   * Warning - do not call this in production
   */
  def createTables(): Future[Unit] = {
    db.run(DBIO.seq(this.schema.create))
  }

  /**
   * Returns all the security in the DB
   */
  def list = {
    val allSecurities = for (o <- this) yield o
    db.run(allSecurities.result)
  }

  def list(marketEvent: MarketEvent) = {

    val query = marketEvent match {
      case MarketEvent(id, name, event_type, summary, description, direction, intensity, _, _, _, Some(ticker), external_url, created_at, updated)      => this.filter(_.ticker === ticker)
      case MarketEvent(id, name, event_type, summary, description, direction, intensity, _, _, Some(sector), _, external_url, created_at, updated)      => this.filter(_.sector === sector)
      case MarketEvent(id, name, event_type, summary, description, direction, intensity, _, Some(region), _, _, external_url, created_at, updated)      => this.filter(_.region === region)
      case MarketEvent(id, name, event_type, summary, description, direction, intensity, Some(asset_class), _, _, _, external_url, created_at, updated) => this.filter(_.asset_class === asset_class)
    }

    db.run(query.result)
  }

}
