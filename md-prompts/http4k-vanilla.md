http4k is a lightweight but fully-featured HTTP toolkit written in pure Kotlin that enables the serving and consuming of HTTP services in a functional and consistent way. http4k applications are just Kotlin functions. For example, here's a simple echo server:

~~~kotlin
 val app: HttpHandler = { request: Request -> Response(OK).body(request.body) }
 val server = app.asServer(Jetty(9000)).start()
~~~

http4k consists of a lightweight core library, http4k-core, providing a base HTTP implementation and Server/Client implementations based on the JDK classes. Further servers, clients, serverless, templating, websockets capabilities are then implemented in add-on modules. http4k apps can be simply mounted into a running Server, Serverless platform, or compiled to GraalVM and run as a super-lightweight binary.

The starting point are HttpHandlers (which are just functions of type (Request) -> Response).

We can bind them to a paths and a HTTP method to create a Route.

Then we can combine many Routes together to make another HttpHandler.

Example of a webserver with a "/ping" route and a greet route that read a person name from a path variable and return "hello $name":

~~~kotlin
val greetingApp: HttpHandler = routes(
    "/ping" bind GET to { _: Request -> Response(OK).body("pong!") },
    "/greet/{name}" bind GET to { req: Request ->
        val name= req.path("name") ?: "anon!"
        Response(OK).body("hello ${name}")
    }
)

fun main() {
    // start the server
    greetingApp.asServer(Jetty(9000)).start()
}    

// curl http://localhost:9000/greet/Bob
// Produces:
//    HTTP/1.1 200 OK
//
//
//    hello Bob
~~~

In the same way we can handle different type of methods and error responses.
Example of routes for a CRUD application on users. We assume the functions to process user data---allUsers, newUser, updateUser, and deleteUser---are already defined:

~~~kotlin
val userRoutes =
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
                newUser(userData)
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
    )
~~~

We can also read parameters from query string and headers.

Example of a route to search the products by name, reading the username from the BasicAuth header (see also the filter example)

~~~kotlin
val productRoutes =
    routes(
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
