package forex.services.oneframe.interpreters

import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.services.oneframe.Algebra
import cats.implicits._
import cats._
import cats.effect.Async
import forex.services.oneframe.Errors.{OneFrameError, OneFrameServiceErrorResponse}
import sttp.client.circe.asJson
import sttp.client._
import sttp.model.Uri
import forex.http.oneframe.Protocol._
import io.circe.{Error => CError}
import forex.services.oneframe.Errors.OneFrameError.{OneFrameTimeoutError, OneFrameUnknownError, OneFrameUnreachableError}

import java.net.{ConnectException, SocketTimeoutException}

class OneFrameLive[F[_]: Async](config: OneFrameConfig, backend: SttpBackend[Identity, Nothing, NothingT])
    extends Algebra[F] {
  override def fetchRatesForPairs(pairs: List[Rate.Pair]): F[Either[OneFrameError, List[Rate]]] = {
    val params   = pairs.map((pair: Rate.Pair) => "pair" -> s"${pair.from}${pair.to}")
    val url: Uri = uri"http://${config.http.host}:${config.http.port}/rates".params(params.toMap)

    val request = basicRequest
      .header("token", config.authToken)
      .get(url)
      .response(asJson[Either[OneFrameServiceErrorResponse, List[Rate]]])
    val response = Async[F]
      .delay {
        backend.send(request)
      }
      .map[Either[OneFrameError, List[Rate]]] {
        resp: Response[Either[ResponseError[CError], Either[OneFrameServiceErrorResponse, List[Rate]]]] =>
          resp.body match {
            case Left(circeError)    => Left(OneFrameUnknownError(s"Circe failure ${circeError.getMessage}"))
            case Right(Left(error))  => Left(OneFrameUnknownError("lef: "+error.error))
            case Right(Right(rates)) => Right(rates)
          }
      }.handleError {
        case _: SocketTimeoutException => Left(OneFrameTimeoutError)
        case conn: ConnectException => Left(OneFrameUnreachableError(conn.getMessage))
        case t: Throwable              => Left(OneFrameUnknownError(t.getMessage))
      }

    response
  }
}

object OneFrameLive {
  def apply[F[_]: Async](config: OneFrameConfig): OneFrameLive[F] = {
    val conn = HttpURLConnectionBackend()
    new OneFrameLive[F](config, conn)
  }
}
