//add
//testImplementation "org.http4k:http4k-testing:4.16.0"
//testImplementation "io.strikt:strikt-core:0.37.0"



import io.strikt.assertions.isEqualTo
import io.strikt.assertions.isNotNull
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class ExchangeRoutesTests {
    private val testApp = exchangeRoutes

    @Test
    fun `should create a bid order`(approver: Approver) {
        val response = testApp(Request(POST, "/AAPL/bid/150").header("WWW-Authenticate", "user1"))
        approver.assertApproved(response)
        strikt.api.expectThat(response.status).isEqualTo(OK)
    }

    @Test
    fun `should create an ask order`(approver: Approver) {
        val response = testApp(Request(POST, "/AAPL/ask/155").header("WWW-Authenticate", "user2"))
        approver.assertApproved(response)
        strikt.api.expectThat(response.status).isEqualTo(OK)
    }

    @Test
    fun `should match bid and ask orders`(approver: Approver) {
        testApp(Request(POST, "/AAPL/bid/150").header("WWW-Authenticate", "user1"))
        val response = testApp(Request(POST, "/AAPL/ask/150").header("WWW-Authenticate", "user2"))
        approver.assertApproved(response)
        strikt.api.expectThat(response.status).isEqualTo(OK)
    }

    @Test
    fun `should get matched orders`(approver: Approver) {
        testApp(Request(POST, "/AAPL/bid/150").header("WWW-Authenticate", "user1"))
        testApp(Request(POST, "/AAPL/ask/150").header("WWW-Authenticate", "user2"))

        val response = testApp(Request(GET, "/AAPL/orders"))
        approver.assertApproved(response)
        strikt.api.expectThat(response.status).isEqualTo(OK)
    }

    @Test
    fun `should get depth`(approver: Approver) {
        testApp(Request(POST, "/AAPL/bid/145").header("WWW-Authenticate", "user1"))
        testApp(Request(POST, "/AAPL/ask/155").header("WWW-Authenticate", "user2"))

        val response = testApp(Request(GET, "/AAPL/depth"))
        approver.assertApproved(response)
        strikt.api.expectThat(response.status).isEqualTo(OK)
    }
}
