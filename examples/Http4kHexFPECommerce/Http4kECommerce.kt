import org.http4k.core.*
import org.http4k.core.Method.*
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer

data class Product(val id: Int, val name: String, val price: Double)
data class CartItem(val productId: Int, val quantity: Int)
data class Cart(val id: Int, val items: List<CartItem>)


object JProduct : JAny<Product>() {
    val id by num(Product::id)
    val name by str(Product::name)
    val price by num(Product::price)

    override fun JsonNodeObject.deserializeOrThrow() =
        Product(
            id = +id,
            name = +name,
            price = +price
        )
}

object JCartItem : JAny<CartItem>() {
    val productId by num(CartItem::productId)
    val quantity by num(CartItem::quantity)

    override fun JsonNodeObject.deserializeOrThrow() =
        CartItem(
            productId = +productId,
            quantity = +quantity
        )
}

object JCart : JAny<Cart>() {
    val id by num(Cart::id)
    val items by list(JCartItem, Cart::items)

    override fun JsonNodeObject.deserializeOrThrow() =
        Cart(
            id = +id,
            items = +items
        )
}


interface ECommerceHub {
    fun addToCart(cartId: Int, cartItem: CartItem): Cart?
    fun removeFromCart(cartId: Int, productId: Int): Cart?
    fun getCartTotal(cartId: Int): Double?
    fun checkout(cartId: Int): Cart?
}

import java.util.concurrent.ConcurrentHashMap


class RealECommerceHub : ECommerceHub {
    private val products = ConcurrentHashMap<Int, Product>()
    private val carts = ConcurrentHashMap<Int, Cart>()

    override fun addProductToCart(cartId: Int, productId: Int, quantity: Int): Cart? {
        val product = products[productId]
        val cart = carts[cartId]

        return if (product != null && cart != null) {
            val updatedItems = cart.items.toMutableList()
            val existingItem = updatedItems.find { it.product.id == productId }
            if (existingItem != null) {
                val updatedItem = existingItem.copy(quantity = existingItem.quantity + quantity)
                updatedItems.replaceAll { if (it.product.id == productId) updatedItem else it }
            } else {
                updatedItems.add(CartItem(product, quantity))
            }
            val updatedCart = cart.copy(items = updatedItems)
            carts[cartId] = updatedCart
            updatedCart
        } else {
            null
        }
    }

    override fun removeProductFromCart(cartId: Int, productId: Int, quantity: Int): Cart? {
        val cart = carts[cartId]

        return if (cart != null) {
            val updatedItems = cart.items.toMutableList()
            val existingItem = updatedItems.find { it.product.id == productId }
            if (existingItem != null) {
                if (existingItem.quantity > quantity) {
                    val updatedItem = existingItem.copy(quantity = existingItem.quantity - quantity)
                    updatedItems.replaceAll { if (it.product.id == productId) updatedItem else it }
                } else {
                    updatedItems.removeIf { it.product.id == productId }
                }
            }
            val updatedCart = cart.copy(items = updatedItems)
            carts[cartId] = updatedCart
            updatedCart
        } else {
            null
        }
    }

    override fun getCartTotal(cartId: Int): Double? {
        val cart = carts[cartId]
        return cart?.items?.sumByDouble { it.product.price * it.quantity }
    }

    override fun checkout(cartId: Int): Cart? {
        val cart = carts.remove(cartId)
        // You can add further logic here for processing the payment, creating an order, etc.
        return cart
    }
}


fun prepareRoutes(hub: ECommerceHub) =
    routes(
        "/cart/{cartId}/add" bind POST to { req: Request ->
            val cartId = req.path("cartId")?.toIntOrNull()
            val cartItem = JCartItem.fromJson(req.bodyString()).orThrow()

            if (cartId != null) {
                hub.addToCart(cartId, cartItem)?.let { JCart.toJson(it) }
            } else {
                Response(BAD_REQUEST)
            }
        },
        "/cart/{cartId}/remove" bind POST to { req: Request ->
            val cartId = req.path("cartId")?.toIntOrNull()
            val productId = JCartItem.fromJson(req.bodyString()).orThrow().productId

            if (cartId != null) {
                hub.removeFromCart(cartId, productId)?.let { JCart.toJson(it) }
            } else {
                Response(BAD_REQUEST)
            }
        },
        "/cart/{cartId}/total" bind GET to { req: Request ->
            val cartId = req.path("cartId")?.toIntOrNull()

            if (cartId != null) {
                hub.getCartTotal(cartId)?.let { it.toString() }
            } else {
                Response(BAD_REQUEST)
            }
        },
        "/cart/{cartId}/checkout" bind POST to { req: Request ->
            val cartId = req.path("cartId")?.toIntOrNull()

            if (cartId != null) {
                hub.checkout(cartId)?.let { JCart.toJson(it) }
            } else {
                Response(BAD_REQUEST)
            }
        }
    )


fun main() {
    val hub = RealECommerceHub()
    val ecommerceRoutes = prepareRoutes(hub)

    ServerFilters.Cors(UnsafeGlobalPermissive)
        .then(timingFilter)
        .then(ecommerceRoutes)
        .asServer(Jetty(9000)).start()
}
