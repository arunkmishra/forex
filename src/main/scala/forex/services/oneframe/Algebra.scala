package forex.services.oneframe

import forex.domain.Rate
import forex.services.oneframe.Errors.OneFrameError

trait Algebra[F[_]] {

  def fetchRatesForPairs(pairs: List[Rate.Pair]): F[Either[OneFrameError, List[Rate]]]
}
