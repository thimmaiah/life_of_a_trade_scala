package com.lot.user.dao

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import persistence.entities.{ Suppliers, Supplier }
import slick.driver.JdbcProfile
import utils.{ DbModule }
import scala.concurrent.Future
import com.lot.user.model.UserTable
import slick.driver.MySQLDriver.api._
import com.lot.user.model.User
import utils.DB._
import scala.concurrent.ExecutionContext.Implicits.global

object UserDao extends TableQuery(new UserTable(_)) {

  def save(user: User): Future[Int] = { db.run(this += user).mapTo[Int] }

  def get(id: Long) = {
    db.run(this.filter(_.id === id).result.headOption)
  }

  def createTables(): Future[Unit] = {
    db.run(DBIO.seq(this.schema.create))
  }

  def list = {
    val allUsers = for (o <- this) yield o
    db.run(allUsers.result)
  }

}