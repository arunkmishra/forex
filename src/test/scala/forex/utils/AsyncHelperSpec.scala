package forex.utils

import cats.effect.IO
import org.scalatest.{Assertion, AsyncTestSuite}

import scala.concurrent.Future

/**
 * Helper to convert IO assertion to Future
 * */
trait AsyncHelperSpec extends AsyncTestSuite{
  implicit def toFutureAssertion(io: IO[Assertion]): Future[Assertion] = io.unsafeToFuture()

}
