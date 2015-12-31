package com.lot.utils

import com.lot.order.dao.OrderDao
import com.lot.order.model.Order
import com.lot.order.model.OrderType
import com.lot.exchange.Exchange

object InitData {

  def init() = {
    val dao = OrderDao
    
    val o1 = Order(Some(1), Exchange.NASDAQ, OrderType.BUY, OrderType.MARKET, 1, 10, 100, 102.3, None, None)
    val o2 = Order(Some(2), Exchange.NASDAQ, OrderType.SELL, OrderType.MARKET, 1, 10, 120, 103.3, None, None)
    val o3 = Order(Some(3), Exchange.NYSE, OrderType.BUY, OrderType.LIMIT, 1, 10, 150, 98.3, None, None)
    dao.save(o1)
    dao.save(o2)
    dao.save(o3)
  }
}