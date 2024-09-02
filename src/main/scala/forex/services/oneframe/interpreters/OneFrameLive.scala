package forex.services.oneframe.interpreters

import cats.effect.Async
import cats.implicits._
import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.http.oneframe.Protocol._
import forex.services.OneFrameService
import forex.services.oneframe.Errors.OneFrameError._
import forex.services.oneframe.Errors.{ OneFrameError, OneFrameServiceErrorResponse }
import io.circe.{ Error => CError }
import sttp.client._
import sttp.client.circe.asJson
import sttp.model.{ Header, MediaType, Uri }

import java.net.{ ConnectException, SocketTimeoutException }

/**
  * OneFrame's live implementation that fetches rates from external service
  * @param config Oneframe config
  * @param backend Sttp backend instance
  * @tparam F effect type
  * */
class OneFrameLive[F[_]: Async](config: OneFrameConfig, backend: SttpBackend[Identity, Nothing, NothingT])
    extends OneFrameService[F] {

  private def oneFrameBaseRequest: RequestT[Empty, Either[String, String], Nothing] =
    basicRequest.header("token", config.authToken).headers(Header.contentType(MediaType.ApplicationJson))
  override def fetchRatesForPairs(pairs: List[Rate.Pair]): F[Either[OneFrameError, List[Rate]]] = {
    val params: Seq[(String, String)] = pairs.map((pair: Rate.Pair) => "pair" -> s"${pair.from}${pair.to}")

    val url: Uri = uri"http://${config.http.host}:${config.http.port}/rates?$params"

    val request = oneFrameBaseRequest
      .get(url)
      .response(asJson[Either[OneFrameServiceErrorResponse, List[Rate]]])
    Async[F]
      .delay {
        backend.send(request)
      }
      .map[Either[OneFrameError, List[Rate]]] {
        resp: Response[Either[ResponseError[CError], Either[OneFrameServiceErrorResponse, List[Rate]]]] =>
          resp.body match {
            case Left(circeError)    => Left(OneFrameUnknownError(s"Circe failure ${circeError.getMessage}"))
            case Right(Left(error))  => Left(OneFrameUnknownError(error.error))
            case Right(Right(rates)) => Right(rates)
          }
      }
      .handleError {
        case _: SocketTimeoutException => Left(OneFrameTimeoutError)
        case conn: ConnectException    => Left(OneFrameUnreachableError(conn.getMessage))
        case t: Throwable              => Left(OneFrameUnknownError(t.getMessage))
      }
  }
}

object OneFrameLive {
  def apply[F[_]: Async](config: OneFrameConfig): OneFrameLive[F] = {
    val conn = HttpURLConnectionBackend()
    new OneFrameLive[F](config, conn)
  }
}
