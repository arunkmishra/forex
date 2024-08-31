package forex.services.rates.interpreters

import cats._
import cats.implicits._
import forex.domain.{ Rate, Timestamp }
import forex.services.rates.errors
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.{ RateStoreService, RatesService }

import java.time.{ OffsetDateTime, ZoneOffset }
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.FiniteDuration

class OneFrameLiveRates[F[_]: Applicative](rateStore: RateStoreService[F], rateValidityPeriod: FiniteDuration)
    extends RatesService[F] {

  private def isValidRate(rate: Rate): Boolean =
    rate.timestamp.value
      .atZoneSameInstant(ZoneOffset.UTC)
      .plus(rateValidityPeriod.toMillis, ChronoUnit.MILLIS)
      .isBefore(Timestamp(OffsetDateTime.now).value.atZoneSameInstant(ZoneOffset.UTC))

  override def get(pair: Rate.Pair): F[Either[errors.Error, Rate]] =
    rateStore.getRates.map { rates =>
      val res: Either[errors.Error, Rate] = rates
        .get(pair)
        .toRight(OneFrameLookupFailed(s"Rate is missing for pair $pair"))
        .flatMap { rate =>
          if (isValidRate(rate)) Left(OneFrameLookupFailed("Expired rate")) else Right(rate)
        }

      res
    }
}
object OneFrameLiveRates {
  def apply[F[_]: Applicative](rateStoreService: RateStoreService[F], rateValidityPeriod: FiniteDuration) =
    new OneFrameLiveRates[F](rateStoreService, rateValidityPeriod)
}
