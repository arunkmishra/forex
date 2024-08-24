package forex.services.rates.interpreters

import cats._
import cats.implicits._
import cats.syntax._
import forex.domain.Rate
import forex.services.oneframe.interpreters.OneFrameLive
import forex.services.rates.{ errors, Algebra }

class OneFrameLiveRates[F[_]: Applicative](oneframeLive: OneFrameLive[F]) extends Algebra[F] {
  override def get(pair: Rate.Pair): F[Either[errors.Error, Rate]] =
    oneframeLive.fetchRatesForPairs(List(pair)).map {
      case Left(error)  => Left(errors.Error.OneFrameLookupFailed(error.toString))
      case Right(rates) => Right(rates.head)
    }

}
