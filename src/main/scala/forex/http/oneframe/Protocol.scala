package forex.http.oneframe

import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.oneframe.Errors.OneFrameServiceErrorResponse
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{ Decoder, HCursor }

import java.time.OffsetDateTime
import scala.util.Try

object Protocol {

  implicit val currencyDecoder: Decoder[Currency] = Currency.circeDecoder
  implicit val timestampDecoder: Decoder[OffsetDateTime] =
    Decoder.decodeString.emapTry((str: String) => Try(OffsetDateTime.parse(str)))

  implicit val rateDecoder: Decoder[Rate] = (cursor: HCursor) =>
    for {
      from <- cursor.downField("from").as[Currency]
      to <- cursor.downField("to").as[Currency]
      price <- cursor.downField("price").as[BigDecimal]
      timestamp <- cursor.downField("time_stamp").as[OffsetDateTime]
    } yield Rate(Rate.Pair(from, to), Price(price), Timestamp(timestamp))

  implicit val errorResponseDecoder: Decoder[OneFrameServiceErrorResponse] = deriveDecoder
  implicit val rateOrErrorDecoder: Decoder[Either[OneFrameServiceErrorResponse, List[Rate]]] =
    errorResponseDecoder.either(Decoder.decodeList(rateDecoder))
}
