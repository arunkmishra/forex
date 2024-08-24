package forex

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  final val RatesServices = rates.Interpreters

  type OneFrameService[F[_]] = oneframe.Algebra[F]
  final val OneFrameServices = oneframe.Interpreters
}
