package forex.http.rates

import cats.data.ValidatedNel
import cats.implicits._
import enumeratum.NoSuchMember
import forex.domain.Currency
import forex.programs.rates.Protocol.GetRatesRequest
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher
import org.http4s.{ ParseFailure, QueryParamDecoder }

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap { inputParam: String =>
      Currency.withNameEither(inputParam).leftMap { err: NoSuchMember[Currency] =>
        ParseFailure(s"Unknown Currency ${err.notFoundName}", err.getMessage())
      }
    }

  private type ValidationResult[A] = ValidatedNel[ParseFailure, A]

  private object FromQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("from")
  private object ToQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("to")

  object FromAndToQueryParams {

    /**
      * Function to validate the input params
      * @param params Validateable paramters
      * @return validation result
      * */
    def unapply(params: Map[String, collection.Seq[String]]): Option[ValidationResult[GetRatesRequest]] = {
      val from: Option[ValidationResult[Currency]] = FromQueryParam.unapply(params)
      val to: Option[ValidationResult[Currency]]   = ToQueryParam.unapply(params)
      (from, to).mapN {
        case tuple: (ValidationResult[Currency], ValidationResult[Currency]) =>
          tuple.mapN { case (f, t) => GetRatesRequest(f, t) }
      }
    }
  }
}
