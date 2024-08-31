package forex.http
package rates

import cats.effect.Sync
import cats.syntax.flatMap._
import com.typesafe.scalalogging.LazyLogging
import forex.domain.Rate
import forex.programs.RatesProgram
import forex.programs.rates.errors.Error.RateLookupFailed
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import io.circe.syntax.EncoderOps
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] with LazyLogging {

  import Converters._
  import Protocol._
  import QueryParams._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromAndToQueryParams(validatedParams) =>
      handleRequestWithQueryParams(
        validatedParams,
        (request: RatesProgramProtocol.GetRatesRequest) =>
          rates.get(request).flatMap {
            case Left(error: RateLookupFailed) =>
              logger.error(s"Failed to look up rates due to ${error.msg}", error)
              InternalServerError(ErrorResponse(error.msg).asJson(ErrorResponse.errorResponseEncoder))
            case Right(rate: Rate) => Ok(rate.asGetApiResponse)
        }
      )
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
