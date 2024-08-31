package forex.http.rates

import cats.effect.IO
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.programs.RatesProgram
import forex.programs.rates.Protocol.GetRatesRequest
import forex.programs.rates.errors.Error.RateLookupFailed
import forex.utils.AsyncHelperSpec
import io.circe.Json
import io.circe.literal._
import org.http4s.circe._
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Method, Request, Status }
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers

import java.time.{ OffsetDateTime, ZoneOffset }

class RatesHttpRoutesSpec extends AsyncFlatSpecLike with Matchers with AsyncHelperSpec {
  import RatesHttpRoutesSpec._

  it should "return rate for valid http route" in {
    val ratesProgram           = mkRatesProgram(Right(testRate))
    val routes                 = new RatesHttpRoutes[IO](ratesProgram).routes
    val request                = Request[IO](Method.GET, uri"/rates?from=CAD&to=CHF")
    val expectedResponse: Json = json"""
                  {
                    "from": "CAD",
                    "to": "CHF",
                    "price": 100.0,
                    "timestamp": "2024-09-01T05:00:00Z"
                  }
                  """

    routes.apply(request).value.flatMap { response =>
      val actualResponse = response
        .map(_.asJson)
        .getOrElse(IO.raiseError(new Exception("empty response")))
      response.map(_.status shouldBe Status.Ok)
      actualResponse.map(s => s shouldEqual expectedResponse)
    }
  }

  it should "return error JSON response" in {
    val ratesProgram   = mkRatesProgram(Left(RateLookupFailed("Rate is missing")))
    val routes         = new RatesHttpRoutes[IO](ratesProgram).routes
    val request        = Request[IO](Method.GET, uri"/rates?from=CAD&to=CHF")
    val expected: Json = json"""
                  {
                    "message": "Rate is missing"
                  }
                  """
    routes.apply(request).value.flatMap { response =>
      val actualResponse = response
        .map(_.asJson)
        .getOrElse(IO.raiseError(new Exception("empty response")))
      response.map(_.status shouldBe Status.InternalServerError)
      actualResponse.map(s => s shouldEqual expected)
    }
  }

  it should "return error for invalid currency param" in {
    val ratesProgram   = mkRatesProgram(Right(testRate))
    val routes         = new RatesHttpRoutes[IO](ratesProgram).routes
    val request        = Request[IO](Method.GET, uri"/rates?from=INR&to=THB")
    val expected: Json = json"""
                  {
                    "message": "Unknown Currency INR; Unknown Currency THB"
                  }
                  """
    routes.apply(request).value.flatMap { response =>
      val actualResponse = response
        .map(_.asJson)
        .getOrElse(IO.raiseError(new Exception("empty response")))
      response.map(_.status shouldBe Status.BadRequest)
      actualResponse.map(s => s shouldEqual expected)
    }
  }

}

object RatesHttpRoutesSpec {
  private val timestamp = Timestamp(OffsetDateTime.of(2024, 9, 1, 5, 0, 0, 0, ZoneOffset.UTC))
  private val testRate  = Rate(Rate.Pair(Currency.CAD, Currency.CHF), Price(BigDecimal(100.0)), timestamp)
  private def mkRatesProgram(result: Either[RateLookupFailed, Rate]): RatesProgram[IO] =
    (_: GetRatesRequest) => IO.pure(result)
}
