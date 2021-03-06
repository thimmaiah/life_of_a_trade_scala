package com.lot.generators

import org.scalacheck.Gen.choose
import org.scalacheck.Gen.oneOf

import com.lot.exchange.Exchange
import com.lot.order.model.Order
import com.lot.order.model.OrderType

object OrderFactory {
  
  def generate(exchange: String = oneOf(Exchange.NASDAQ, Exchange.NYSE).sample.get, 
          buy_sell: String = oneOf(OrderType.BUY, OrderType.SELL).sample.get, 
          order_type: String = oneOf(OrderType.MARKET, OrderType.LIMIT).sample.get, 
          user_id: Long = choose(1L, 50L).sample.get, 
          security_id: Long, 
          ticker: String = "Tick",
          quantity: Double = choose(1, 10).sample.get * 1000.0, 
          unfilled_qty: Double = 0.0, 
          price: Double = choose(1, 10).sample.get * 100.0,
          preTradeCheckStatus: String = "",
          tradeStatus: String = "",
          status: String ="") = {

    val aprice = if (order_type == OrderType.MARKET)  0.0  else price 
    
    Order(None, exchange, buy_sell, order_type, user_id, security_id, ticker, quantity, unfilled_qty, 
          aprice, preTradeCheckStatus, tradeStatus, status, None, None)
  }
  
   
}

