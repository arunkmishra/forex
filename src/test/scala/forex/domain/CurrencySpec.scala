package domain

import forex.domain.Currency
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CurrencySpec extends AnyFlatSpec with Matchers {

  it should "return all possible pairs" in {
    Currency.allCurrencyPairs should have size 72 // Pre computed based on supported currency
    Currency.allCurrencyPairs.forall { case (from, to) => from != to } should be(true)
  }

}
