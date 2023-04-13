Hexagonal architecture, also known as Ports-and-Adapters architecture, is a software architecture pattern that aims to enhance the modularity, maintainability, and testability of software applications.

The primary objective of Hexagonal architecture is to enable an application to be driven by various entities, such as users, programs, automated tests, or batch scripts, and to develop and test the application in isolation from the runtime devices and databases.

When a user or a driver wants to use the application at a port, it sends a request that is converted by an adapter, specifically designed for the driver's technology, into a usable procedure call or message. The adapter then passes this request to the application port. Notably, the application remains unaware of the driver's technology. Similarly, when the application has to send out information, it passes the information through a port to an adapter that creates the required signals for the receiving technology, whether human or automated. This way, the application interacts semantically with the adapters on all sides without knowing the nature of the things on the other end.

The Hexagonal or Ports-and-Adapters architecture resolves these issues by recognizing the symmetry in the situation - there is an application on the inside communicating over some number of ports with things on the outside, which can be treated symmetrically.

To adopt Hexagonal architecture pattern it in conjunction with Http4k we define and interface, called the Hub, with all the domain methods needed by the routes.


~~~kotlin
interface UserHub{
    fun userById(id: Int): User?
    fun newUser(userData: User): Int
    fun updateUser(id: Int, userData: User): User?
    fun deleteUser(id): User?
    fun allProductsForUser(userName: String): List<Product>
    fun productByName(productName: String): List<Product>
}

fun prepareRoutes(hub: UserHub) =
    routes(
        "/user/{id}" bind GET to { req: Request ->
            val id = req.path("id")?.toIntOrNull()
            if (id != null) {
                userById(id)
            } else {
                Response(BAD_REQUEST)
            }

        },
        "/user" bind POST to { req: Request ->
            val userData = req.bodyString()
            if (userData != null) {
                val newId = newUser(userData)
                Response(OK).bodyString()
            } else {
                Response(BAD_REQUEST)
            }
        },
        "/user/{id}" bind PUT to { req: Request ->
            val id = req.path("id")?.toIntOrNull()
            if (id != null) {
                val userData = req.bodyString()
                updateUser(id, userData)
            } else {
                Response(BAD_REQUEST)
            }
        },
        "/user/{id}" bind DELETE to {req: Request ->
            val id = req.path("id")?.toIntOrNull()
            if (id != null) {
                deleteUser(id)
            } else {
                Response(BAD_REQUEST)
            }
        },
        "/products" bind GET to { req: Request ->
            val prodName = req.query("name")
            val user = req.header("WWW-Authenticate")
            if (user == null) {
                Response(UNAUTHORIZED)
            } else {
                if (prodName == null) {
                    getAllProductsForUser(user)
                } else {
                    getProductByName(user, prodName)
                }
            }
        }
    )

fun main() {
    val hub =
    val userRoutes = prepareRoutes(hub)
    ServerFilters.Cors(UnsafeGlobalPermissive)
        .then(ServerFilters.BasicAuth("realm", "username", "password"))
        .then(userRoutes)
        .asServer(Jetty(9000)).start()
}
~~~

Filters in Http4k are functions of type (HttpHandler) -> HttpHandler.

They are very convenient to implement some logic common to many routes, for example collecting metrics, authentication, caching, etc.

Filters can performs pre/post processing on a request or response. 

We can compose filters together to form another filter using 

`val filter3 = filter1.then(filter2)`

or decorate an HttpHandler with a filter to create another HttpHandler using 

`val decordatedHttpHandler = filter1.then(httpHandler)`

In this example we can create a filter to measure the timing to process each request:

~~~kotlin
val timingFilter = Filter {
    next: HttpHandler -> { request: Request ->
        val start = System.currentTimeMillis()
        next(request).also {
            val latency = System.currentTimeMillis() - start
            println("Request to ${request.uri} took ${latency}ms")
        }
    }
}

fun main(){
    val filteredApp: HttpHandler = CachingFilters.Response.NoCache()
        .then(greetingApp)
    filteredApp.asServer(Jetty(9000)).start()
}

//    curl "http://localhost:9000/greet/Bob"
// Produces:
//    Request to /api/greet/Bob took 1ms
//    HTTP/1.1 200
//    cache-control: private, must-revalidate
//    content-length: 9
//    date: Thu, 08 Jun 2017 13:01:13 GMT
//    expires: 0
//    server: Jetty(9.3.16.v20170120)
//
//    hello Bob
~~~

There are many filters ready to use for caching and authentication.

Finally, we can compose routes and filters to create our application:

~~~kotlin
fun main() {
    val compositeRoutes = routes(productRoutes, userRoutes)

    ServerFilters.Cors(UnsafeGlobalPermissive)
        .then(ServerFilters.BasicAuth("realm", "username", "password"))
        .then(compositeRoutes)
        .asServer(Jetty(9000)).start()
}
~~~
