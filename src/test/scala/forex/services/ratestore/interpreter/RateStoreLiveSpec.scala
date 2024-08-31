package forex.services.ratestore.interpreter

import cats.effect.{ ContextShift, IO, Timer }
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.OneFrameService
import forex.services.oneframe.Errors
import forex.services.oneframe.Errors.OneFrameError.OneFrameUnknownError
import forex.utils.AsyncHelperSpec
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class RateStoreLiveSpec extends AsyncFlatSpec with Matchers with AsyncHelperSpec {
  import RateStoreLiveSpec._
  implicit val timer: Timer[IO]     = IO.timer(scala.concurrent.ExecutionContext.global)
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  it should "update rates in the background " in {
    val oneframeService = new OneFrameServiceTest()
    val service         = RateStoreLive[IO](oneframeService, 1.seconds)

    for {
      _ <- service.updateRates()
      rates <- service.getRates
    } yield {
      rates.values should contain(testRate)
      rates should not be empty
    }
  }

  it should "handle errors during rate update" in {
    val oneFrameService = new OneFrameServiceTest(shouldFail = true)
    val rateStore       = RateStoreLive[IO](oneFrameService, 1.second)

    for {
      _ <- rateStore.updateRates().attempt
      rates <- rateStore.getRates
    } yield {
      rates shouldBe empty
    }
  }

  it should "update cache asynchronously using backgroundRefresh" in {
    val oneFrameService = new OneFrameServiceTest()
    val rateStore       = RateStoreLive[IO](oneFrameService, 1.second)
    val testStream      = rateStore.backgroundRefresh().interruptAfter(3.seconds)
    for {
      _ <- testStream.compile.drain.start // Start the stream asynchronously
      _ <- IO.sleep(2.seconds) // Allow some time for the stream to update the cache
      rates <- rateStore.getRates
    } yield {
      rates should not be empty
      rates.values should contain(testRate)
    }
  }
}

object RateStoreLiveSpec {
  private val testRate = Rate(Rate.Pair(Currency.CAD, Currency.CHF), Price(BigDecimal(100.0)), Timestamp.now)
  private class OneFrameServiceTest(shouldFail: Boolean = false) extends OneFrameService[IO] {
    override def fetchRatesForPairs(pairs: List[Rate.Pair]): IO[Either[Errors.OneFrameError, List[Rate]]] =
      if (shouldFail) IO.pure(Left(OneFrameUnknownError("fail request")))
      else IO.pure(Right(List(testRate)))

  }
}
