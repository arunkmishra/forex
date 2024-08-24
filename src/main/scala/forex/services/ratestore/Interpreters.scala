package forex.services.ratestore

import cats.effect.{ Async, Timer }
import forex.services.OneFrameService
import forex.services.ratestore.interpreter.RateStoreLive

import scala.concurrent.duration.FiniteDuration

object Interpreters {
  def live[F[_]: Async: Timer](oneFrameService: OneFrameService[F], refreshInterval: FiniteDuration): RateStoreLive[F] =
    RateStoreLive[F](oneFrameService, refreshInterval)
}
