package forex

import cats.effect.{ Concurrent, Timer }
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig) {

  private val oneFrameService = OneFrameServices.live(config.oneFrame)
  // private val ratesService: RatesService[F] = RatesServices.dummy[F]
  private val rateStoreCache                = RateStoreServices.live(oneFrameService, config.oneFrame.refreshRates)
  private val ratesService: RatesService[F] = RatesServices.live[F](rateStoreCache, config.oneFrame.rateExpiration)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  private type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  private type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F]                    = appMiddleware(routesMiddleware(http).orNotFound)
  val refreshRateStream: fs2.Stream[F, Unit] = rateStoreCache.backgroundRefresh()

}
