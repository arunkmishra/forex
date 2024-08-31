package forex.services.oneframe.interepreters

import cats.effect.{ ContextShift, IO, Timer }
import forex.config.{ OneFrameConfig, OneFrameHttpConfig }
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.oneframe.Errors.OneFrameError
import forex.services.oneframe.Errors.OneFrameError.OneFrameUnknownError
import forex.services.oneframe.interpreters.OneFrameLive
import forex.utils.AsyncHelperSpec
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers
import sttp.client.Response
import sttp.client.testing.SttpBackendStub

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class OneFrameLiveSpec extends AsyncFlatSpecLike with Matchers with AsyncHelperSpec {
  import OneFrameLiveSpec._
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO]     = IO.timer(ExecutionContext.global)

  it should "fetch rates for pairs successfully" in {

    val response: String =
      s"""
        |[
        |  {
        |    "from": "$fromCurrency",
        |    "to": "$toCurrency",
        |    "bid": 0.8702743979669029,
        |    "ask": 0.8129834411047454,
        |    "price": ${price.value},
        |    "time_stamp": "${dateTime.value}"
        |  }
        |]
        |""".stripMargin

    val expectedRate = List(
      Rate(
        Rate.Pair(fromCurrency, toCurrency),
        price,
        dateTime
      )
    )

    val backendStub = SttpBackendStub.synchronous
      .whenRequestMatches(_.uri.path.endsWith(List("rates")))
      .thenRespond(Response.ok(response))

    val oneFrameService = new OneFrameLive[IO](config, backendStub)
    oneFrameService.fetchRatesForPairs(pairs).map {
      case Right(fetchedRates) => fetchedRates shouldEqual expectedRate
      case Left(error)         => fail(s"Expected successful fetch but got error: $error")
    }
  }

  it should "handle error response " in {
    val errorResponse = s"""
                          |{
                          |  "error": "$errorMessage"
                          |}
                          |""".stripMargin
    val backendStub = SttpBackendStub.synchronous
      .whenRequestMatches(_.uri.path.endsWith(List("rates")))
      .thenRespond(
        Response.ok(errorResponse)
      )
    new OneFrameLive[IO](config, backendStub).fetchRatesForPairs(pairs).map {
      case Left(error) => error shouldEqual OneFrameUnknownError(errorMessage)
      case Right(_)    => fail(s"expected $errorMessage")
    }
  }

  it should "handle circe decode exception" in {
    val incorrectResponseBody = "wrong json"

    val backendStub = SttpBackendStub.synchronous
      .whenRequestMatches(_.uri.path.endsWith(List("rates")))
      .thenRespond(incorrectResponseBody)

    val oneFrameService = new OneFrameLive[IO](config, backendStub)

    oneFrameService.fetchRatesForPairs(pairs).unsafeToFuture().map {
      case Left(OneFrameError.OneFrameUnknownError(message)) => message should include("Circe failure")
      case _                                                 => fail(s"Expected Circe decode failure but got different result ")
    }
  }

}
object OneFrameLiveSpec {
  private val config =
    OneFrameConfig(OneFrameHttpConfig("localhost", 8080, 2.minutes), "auth-Token", 1.seconds, 1.seconds)
  private val fromCurrency = Currency.CHF
  private val toCurrency   = Currency.CAD
  private val pairs        = List(Rate.Pair(fromCurrency, toCurrency))
  private val price        = Price(BigDecimal(0.84162891953582))
  private val dateTime     = Timestamp.now
  private val errorMessage = "An error occurred"
}
