package forex.services.rates

import cats.Applicative
import forex.services.RateStoreService
import forex.services.rates.interpreters._

import scala.concurrent.duration.FiniteDuration

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def live[F[_]: Applicative](rateStore: RateStoreService[F], rateValidityPeriod: FiniteDuration): Algebra[F] =
    OneFrameLiveRates[F](rateStore, rateValidityPeriod)
}
