package forex.services.rates.interpreters

import cats._
import cats.implicits._
import forex.domain.Rate
import forex.services.rates.errors
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.{ RateStoreService, RatesService }

class OneFrameLiveRates[F[_]: Applicative](rateStore: RateStoreService[F]) extends RatesService[F] {

  private def isValidRate(rate: Rate): Boolean = ???
  override def get(pair: Rate.Pair): F[Either[errors.Error, Rate]] =
    rateStore.getRates.map { rates =>
      println("rate in cache: " + rates)
      val res: Either[errors.Error, Rate] = rates.get(pair).toRight {
        println(">> rate not found")
        OneFrameLookupFailed(s"Rate is missing for pair $pair")
      }

      res
    }
}
object OneFrameLiveRates {
  def apply[F[_]: Applicative](rateStoreService: RateStoreService[F]) = new OneFrameLiveRates[F](rateStoreService)
}
