package forex.services.ratestore

import forex.domain.Rate

trait Algebra[F[_]] {

  def getRates: F[Map[Rate.Pair, Rate]]
}
