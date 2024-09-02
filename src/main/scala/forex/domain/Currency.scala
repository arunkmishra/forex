package forex.domain

import enumeratum.{ CirceEnum, Enum, EnumEntry }
import enumeratum.EnumEntry.Uppercase
sealed trait Currency extends EnumEntry with Uppercase

object Currency extends Enum[Currency] with CirceEnum[Currency] {
  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency
  override def values: IndexedSeq[Currency] = findValues

  /**
    * All possible unique currency pairs
    * */
  def allCurrencyPairs: List[(Currency, Currency)] =
    (for {
      (currencyFrom, indexFrom) <- values.zipWithIndex
      (currencyTo, indexTo) <- values.zipWithIndex if indexFrom != indexTo
    } yield (currencyFrom, currencyTo)).toList
}
