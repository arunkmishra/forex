package forex.services.oneframe

import cats.effect.Async
import forex.config.OneFrameConfig
import forex.services.oneframe.interpreters.OneFrameLive

object Interpreters {

  def live[F[_]: Async](config: OneFrameConfig): OneFrameLive[F] = OneFrameLive(config)
}
