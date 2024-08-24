package forex.config

import scala.concurrent.duration.FiniteDuration

case class OneFrameConfig(http: OneFrameHttpConfig, authToken: String, refreshRates: FiniteDuration)

case class OneFrameHttpConfig(host: String, port: Int, timeout: FiniteDuration)
