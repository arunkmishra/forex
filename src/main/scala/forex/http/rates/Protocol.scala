package forex.http
package rates

import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.encoding.{ EnumerationEncoder, UnwrappedEncoder }
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  implicit val currencyEncoder: Encoder[Currency] = Currency.circeEncoder

  implicit val pairEncoder: Encoder[Pair] = deriveConfiguredEncoder[Pair]

  implicit val rateEncoder: Encoder[Rate] = deriveConfiguredEncoder[Rate]

  implicit val responseEncoder: Encoder[GetApiResponse]           = deriveConfiguredEncoder[GetApiResponse]
  implicit def valueClassEncoder[A: UnwrappedEncoder]: Encoder[A] = implicitly
  implicit def enumEncoder[A: EnumerationEncoder]: Encoder[A]     = implicitly

}
