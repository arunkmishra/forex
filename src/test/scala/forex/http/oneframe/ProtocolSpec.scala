package forex.http.oneframe

import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.oneframe.Errors.OneFrameServiceErrorResponse
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import io.circe.parser.decode

import java.time.OffsetDateTime
class ProtocolSpec extends AnyFlatSpecLike with Matchers {
  import Protocol._

  it should "correctly decode a Currency" in {
    val json    = "\"USD\""
    val decoded = decode[Currency](json)
    decoded shouldBe Right(Currency.USD)
  }

  it should "correctly decode a Timestamp" in {
    val json    = "\"2024-09-01T10:00:00Z\""
    val decoded = decode[OffsetDateTime](json)
    decoded shouldBe a[Right[_, _]]
    decoded.getOrElse(OffsetDateTime.now) shouldEqual OffsetDateTime.parse("2024-09-01T10:00:00Z")
  }

  it should "correctly decode a Rate" in {
    val json =
      """
        |{
        |  "from": "USD",
        |  "to": "EUR",
        |  "price": 1.1,
        |  "time_stamp": "2024-09-01T10:00:00Z"
        |}
        |""".stripMargin
    val decoded = decode[Rate](json)
    decoded shouldBe a[Right[_, _]]
    val expectedRate = Rate(
      Rate.Pair(Currency.USD, Currency.EUR),
      Price(1.1),
      Timestamp(OffsetDateTime.parse("2024-09-01T10:00:00Z"))
    )
    decoded.getOrElse(fail("Decoding failed")) shouldEqual expectedRate
  }

  it should "correctly decode a OneFrameServiceErrorResponse" in {
    val json =
      """
        |{
        |  "error": "Some error occurred"
        |}
        |""".stripMargin
    val decoded = decode[OneFrameServiceErrorResponse](json)
    decoded shouldBe a[Right[_, _]]
    decoded.getOrElse(fail("Decoding failed")).error shouldEqual "Some error occurred"
  }

  it should "correctly decode Either[OneFrameServiceErrorResponse, List[Rate]]" in {
    val jsonSuccess =
      """
        |[
        |  {
        |    "from": "USD",
        |    "to": "EUR",
        |    "price": 1.1,
        |    "time_stamp": "2024-09-01T10:00:00Z"
        |  }
        |]
        |""".stripMargin

    val decodedSuccess = decode[Either[OneFrameServiceErrorResponse, List[Rate]]](jsonSuccess)

    decodedSuccess shouldBe a[Right[_, _]]
    decodedSuccess.getOrElse(fail("Decoding failed")).isRight shouldBe true
  }

}
