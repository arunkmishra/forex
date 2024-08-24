package forex.services.rates

import cats.Applicative
import forex.services.RateStoreService
import forex.services.rates.interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def live[F[_]: Applicative](rateStore: RateStoreService[F]): Algebra[F] = OneFrameLiveRates[F](rateStore)
}
