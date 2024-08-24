package forex.services.rates

import cats.Applicative
import forex.services.oneframe.interpreters.OneFrameLive
import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def live[F[_]: Applicative](oneFrameLive: OneFrameLive[F]): Algebra[F] = new OneFrameLiveRates[F](oneFrameLive)
}
