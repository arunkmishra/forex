package forex.http.rates

import forex.domain.Currency
import org.http4s.{ ParseFailure, QueryParamDecoder }
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import cats.implicits._
import enumeratum.NoSuchMember

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap { inputParam: String =>
      Currency.withNameEither(inputParam).leftMap { err: NoSuchMember[Currency] =>
        ParseFailure(s"Unknown Currency ${err.notFoundName}", err.getMessage())
      }
    }

  object FromQueryParam extends QueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends QueryParamDecoderMatcher[Currency]("to")

}
