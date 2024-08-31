package forex.services.rates.interpreters

import cats.effect.IO
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.RateStoreService
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.utils.AsyncHelperSpec
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers

import java.time.temporal.ChronoUnit
import java.time.{ OffsetDateTime, ZoneOffset }
import scala.concurrent.duration.DurationInt

class OneFrameLiveRatesSpec extends AsyncFlatSpecLike with Matchers with AsyncHelperSpec {
  import OneFrameLiveRatesSpec._

  it should "return a valid rate" in {
    oneFrameLiveRates.get(validRate.pair).map {
      case Right(rate) => rate shouldEqual validRate
      case Left(error) => fail(s"Expected valid rate but got error: $error")
    }
  }

  it should "return an error for expired rate" in {
    oneFrameLiveRates.get(expiredRate.pair).map {
      case Left(error) => error shouldEqual OneFrameLookupFailed("Expired rate")
      case _           => fail("Expected expired rate")
    }
  }

  it should "return an error for missing rates" in {
    oneFrameLiveRates.get(missingPair).map {
      case Left(error) => error shouldEqual OneFrameLookupFailed(s"Rate is missing for pair $missingPair")
      case _           => fail("Expected missing rate")
    }
  }

}

object OneFrameLiveRatesSpec {
  private val validTimestamp   = Timestamp(OffsetDateTime.now(ZoneOffset.UTC).minus(4, ChronoUnit.MINUTES))
  private val expiredTimestamp = Timestamp(OffsetDateTime.now(ZoneOffset.UTC).minus(1, ChronoUnit.HOURS))
  private val validRate        = Rate(Rate.Pair(Currency.CAD, Currency.CHF), Price(BigDecimal(100.0)), validTimestamp)
  private val expiredRate      = Rate(Rate.Pair(Currency.CAD, Currency.EUR), Price(BigDecimal(100.0)), expiredTimestamp)
  private val testRateStore = new RateStoreService[IO] {
    override def getRates: IO[Map[Rate.Pair, Rate]] =
      IO.pure(Map(validRate.pair -> validRate, expiredRate.pair -> expiredRate))
  }
  private val rateValidityPeriod = 5.minutes
  private val missingPair        = Rate.Pair(Currency.CHF, Currency.USD)
  private val oneFrameLiveRates  = OneFrameLiveRates[IO](testRateStore, rateValidityPeriod)
}
