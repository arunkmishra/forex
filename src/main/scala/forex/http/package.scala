package forex

import cats.data.{ NonEmptyList, ValidatedNel }
import cats.effect.Sync
import cats.instances.string._
import cats.syntax.applicative._
import cats.syntax.foldable._
import io.circe.{ Decoder, Encoder }
import org.http4s._
import org.http4s.circe._

package object http {

  implicit def jsonDecoder[A <: Product: Decoder, F[_]: Sync]: EntityDecoder[F, A] = jsonOf[F, A]
  implicit def jsonEncoder[A <: Product: Encoder, F[_]]: EntityEncoder[F, A]       = jsonEncoderOf[F, A]

  def handleRequestWithQueryParams[P, F[_]: Sync](params: ValidatedNel[ParseFailure, P],
                                                  inner: P => F[Response[F]]): F[Response[F]] =
    params.fold(
      (errors: NonEmptyList[ParseFailure]) =>
        Response[F](Status.BadRequest)
          .withEntity(ErrorResponse(errors.map(_.sanitized).mkString_("; ")))
          .pure[F],
      (valid: P) => inner(valid)
    )

}
