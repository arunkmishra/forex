package forex.services.ratestore.interpreter

import cats.effect.concurrent.Ref
import cats.effect.{ Async, Timer }
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import com.typesafe.scalalogging.LazyLogging
import forex.domain.{ Currency, Rate }
import forex.services.oneframe.Errors.OneFrameError
import forex.services.{ OneFrameService, RateStoreService }
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

class RateStoreLive[F[_]: Async: Timer](oneFrameService: OneFrameService[F], refreshInterval: FiniteDuration)
    extends RateStoreService[F]
    with LazyLogging {
  private val cache: Ref[F, Map[Rate.Pair, Rate]] = Ref.unsafe(Map.empty[Rate.Pair, Rate])
  override def getRates: F[Map[Rate.Pair, Rate]]  = cache.get

  def updateRates(): F[Unit] =
    oneFrameService
      .fetchRatesForPairs(Currency.allCurrencyPairs.map(Rate.Pair.tupled))
      .flatMap[List[Rate]] {
        case Left(err: OneFrameError) => Async[F].raiseError(new Exception(err.message))
        case Right(value)             => Async[F].pure(value)
      }
      .flatMap { rates =>
        val cachedRate = rates.map(rate => rate.pair -> rate).toMap
        logger.debug("Updating rate store cache")
        cache.set(cachedRate)
      }
      .handleError { t: Throwable =>
        logger.error("An error occurred while updating rate store cache", t)
      }

  def backgroundRefresh(): Stream[F, Unit] =
    Stream.eval(updateRates()) ++ Stream.awakeEvery[F](refreshInterval).evalMap(_ => updateRates())
}

object RateStoreLive {
  def apply[F[_]: Async: Timer](oneFrameService: OneFrameService[F], refreshInterval: FiniteDuration) =
    new RateStoreLive[F](oneFrameService, refreshInterval)
}
