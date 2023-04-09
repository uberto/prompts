


/*
Can you create the backend for a simple exchange with these api:
/{stock}/bid/{price}    <- order to sell a stock to a minimum price
/{stock}/ask/{price}   <- order to buy a stock to a max price

when bid or ask match an existing ask or bid (meaning that min bid price is lower than max ask price) you can remove the bid and ask and create an order with the names of the users, the time and the actual price (halfway between ask and bid).
To keep things simple let's assume order have all quantity equal to 1.
If a bid or ask is matched, respond with the order details, otherwise respond with "created"

then there are other 2 apis:
/{stock}/orders  <- list of matched orders ordered by time
/{stock}/depth <- list of standing bids and asks not matched

 */

import org.http4k.core.*
import org.http4k.core.Method.*
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

data class Order(val id: Int, val user: String, val stock: String, val price: Double, val type: OrderType)
enum class OrderType { BID, ASK }
data class MatchedOrder(
    val id: Int,
    val stock: String,
    val price: Double,
    val buyer: String,
    val seller: String,
    val timestamp: LocalDateTime
)

val orderIdCounter = AtomicInteger()
val matchedOrderIdCounter = AtomicInteger()

val bidOrders = mutableListOf<Order>()
val askOrders = mutableListOf<Order>()
val matchedOrders = mutableListOf<MatchedOrder>()

fun createOrder(user: String, stock: String, price: Double, type: OrderType): Response {
    val order = Order(orderIdCounter.incrementAndGet(), user, stock, price, type)
    when (type) {
        OrderType.BID -> bidOrders.add(order)
        OrderType.ASK -> askOrders.add(order)
    }
    return matchOrders(order)
}

fun matchOrders(order: Order): Response {
    val bids = bidOrders.sortedByDescending { it.price }
    val asks = askOrders.sortedBy { it.price }

    for (bid in bids) {
        for (ask in asks) {
            if (bid.stock == ask.stock && bid.price >= ask.price) {
                val matchPrice = (bid.price + ask.price) / 2
                val matchedOrder = MatchedOrder(
                    matchedOrderIdCounter.incrementAndGet(),
                    bid.stock,
                    matchPrice,
                    bid.user,
                    ask.user,
                    LocalDateTime.now()
                )
                matchedOrders.add(matchedOrder)

                bidOrders.remove(bid)
                askOrders.remove(ask)

                if (order.id == bid.id || order.id == ask.id) {
                    return Response(OK).body("Matched order: $matchedOrder")
                }
            }
        }
    }

    return Response(OK).body("Created")
}

val exchangeRoutes: RoutingHttpHandler = routes(
    "/{stock}/bid/{price}" bind POST to { req: Request ->
        val user = req.header("WWW-Authenticate")
        val stock = req.path("stock")
        val price = req.path("price")?.toDoubleOrNull()

        if (user != null && stock != null && price != null) {
            createOrder(user, stock, price, OrderType.BID)
        } else {
            Response(BAD_REQUEST).body("Invalid input.")
        }
    },

    "/{stock}/ask/{price}" bind POST to { req: Request ->
        val user = req.header("WWW-Authenticate")
        val stock = req.path("stock")
        val price = req.path("price")?.toDoubleOrNull()

        if (user != null && stock != null && price != null) {
            createOrder(user, stock, price, OrderType.ASK)
        } else {
            Response(BAD_REQUEST).body("Invalid input.")
        }
    },

    "/{stock}/orders" bind GET to { req: Request ->
        val stock = req.path("stock")
        if (stock != null) {
            val orders = matchedOrders.filter { it.stock == stock }.sortedBy { it.timestamp }
            Response(OK).body("Matched orders for $stock: $orders")
        } else {
            Response(BAD_REQUEST).body("Invalid stock symbol.")
        }
    },

    kotlin

    "/{stock}/depth" bind GET to { req: Request ->
        val stock = req.path("stock")
        if (stock != null) {
            val bids = bidOrders.filter { it.stock == stock }.sortedByDescending { it.price }
            val asks = askOrders.filter { it.stock == stock }.sortedBy { it.price }
            Response(OK).body("Bids: $bids, Asks: $asks")
        } else {
            Response(BAD_REQUEST).body("Invalid stock symbol.")
        }
    }

)

fun main() {
    val app = ServerFilters.Cors(UnsafeGlobalPermissive)
        .then(ServerFilters.BasicAuth("realm", "username", "password"))
        .then(exchangeRoutes)

    scss

    app.asServer(Jetty(9000)).start()

}