import org.http4k.core.*
import org.http4k.server.Jetty
import org.http4k.server.asServer

data class Order(val orderId: String, val side: Side, val price: Int, val quantity: Int)

enum class Side { BUY, SELL }

class StockExchange {
    private val buyOrders = mutableListOf<Order>()
    private val sellOrders = mutableListOf<Order>()

    fun placeOrder(order: Order) {
        when (order.side) {
            Side.BUY -> buyOrders.add(order)
            Side.SELL -> sellOrders.add(order)
        }
        matchOrders()
    }

    fun getOrderBook(): List<Order> {
        val orders = mutableListOf<Order>()
        orders.addAll(buyOrders)
        orders.addAll(sellOrders)
        return orders
    }

    private fun matchOrders() {
        var matched = false
        while (!matched && buyOrders.isNotEmpty() && sellOrders.isNotEmpty()) {
            val buyOrder = buyOrders.first()
            val sellOrder = sellOrders.first()
            if (buyOrder.price >= sellOrder.price) {
                val quantity = minOf(buyOrder.quantity, sellOrder.quantity)
                buyOrders.remove(buyOrder)
                sellOrders.remove(sellOrder)
                if (buyOrder.quantity > quantity) {
                    buyOrders.add(buyOrder.copy(quantity = buyOrder.quantity - quantity))
                }
                if (sellOrder.quantity > quantity) {
                    sellOrders.add(sellOrder.copy(quantity = sellOrder.quantity - quantity))
                }
                matched = true
            } else {
                matched = false
            }
        }
    }
}

fun main() {
    val exchange = StockExchange()
    val app: HttpHandler = routes(
        "/orders" bind Method.POST to { req ->
            val order = Order(
                orderId = req.bodyString(),
                side = Side.valueOf(req.query("side")!!.toUpperCase()),
                price = req.query("price")!!.toInt(),
                quantity = req.query("quantity")!!.toInt()
            )
            exchange.placeOrder(order)
            Response(Status.OK)
        },
        "/orders" bind Method.GET to { Response(Status.OK).body(exchange.getOrderBook().toString()) }
    )

    app.asServer(Jetty(9000)).start()
}
