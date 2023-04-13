


/*
Can you create the backend in Kotlin using http4k for a simple exchange with these api:
/{stock}/bid/{price}    <- order to sell a stock to a minimum price
/{stock}/ask/{price}   <- order to buy a stock to a max price

when bid or ask match an existing ask or bid (meaning that min bid price is lower than max ask price) you can remove the bid and ask and create an order with the names of the users, the time and the actual price (halfway between ask and bid).
To keep things simple let's assume order have all quantity equal to 1.
If a bid or ask is matched, respond with the order details, otherwise respond with "created"

then there are other 2 apis:
/{stock}/orders  <- list of matched orders ordered by time
/{stock}/depth <- list of standing bids and asks not matched


I'll give you a description of what is http4k and some examples with the style I want you to follow when writing the applicaiton
 ---prompt vanilla
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

data class Bid(val stock: String, val price: Double, val user: String)
data class Ask(val stock: String, val price: Double, val user: String)
data class Order(val stock: String, val price: Double, val buyer: String, val seller: String, val time: LocalDateTime)

val bidOrdersByStock = mutableMapOf<String, MutableList<Bid>>()
val askOrdersByStock = mutableMapOf<String, MutableList<Ask>>()
val matchedOrders = mutableListOf<Order>()

fun addBidOrder(bid: Bid) {
    bidOrdersByStock.getOrPut(bid.stock) { mutableListOf() }.add(bid)
    bidOrdersByStock[bid.stock]?.sortByDescending { it.price }
}

fun addAskOrder(ask: Ask) {
    askOrdersByStock.getOrPut(ask.stock) { mutableListOf() }.add(ask)
    askOrdersByStock[ask.stock]?.sortBy { it.price }
}

fun createOrder(user: String, stock: String, price: Double, type: OrderType): Response {
    val order = Order(orderIdCounter.incrementAndGet(), user, stock, price, type)
    when (type) {
        OrderType.BID -> bidOrders.add(order)
        OrderType.ASK -> askOrders.add(order)
    }
    return matchOrders(order)
}

fun matchOrders(stock: String): Order? {
    val bidOrders = bidOrdersByStock[stock]
    val askOrders = askOrdersByStock[stock]

    if (bidOrders.isNullOrEmpty() || askOrders.isNullOrEmpty()) {
        return null
    }

    val highestBid = bidOrders.first()
    val lowestAsk = askOrders.first()

    if (highestBid.price >= lowestAsk.price) {
        bidOrders.removeAt(0)
        askOrders.removeAt(0)

        val matchedPrice = (highestBid.price + lowestAsk.price) / 2
        val order = Order(stock, matchedPrice, highestBid.user, lowestAsk.user, LocalDateTime.now())
        matchedOrders.add(order)
        return order
    }

    return null
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