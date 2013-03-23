package models

import org.joda.time.{LocalDate, DateTime}

object Zodiac {

  private val signs = Map(
    "0120" -> "Capricorne",
    "0218" -> "Verseau",
    "0320" -> "Poisson",
    "0420" -> "Bélier",
    "0521" -> "Taureau",
    "0621" -> "Gémeaux",
    "0722" -> "Cancer",
    "0822" -> "Lion",
    "0922" -> "Vierge",
    "1022" -> "Balance",
    "1122" -> "Scorpion",
    "1221" -> "Sagittaire",
    "1300" -> "Capricorne")

  def sign(date: LocalDate) = {
    val dateStr = "%02d%02d".format(date.getMonthOfYear, date.getDayOfMonth)
    signs.dropWhile(e => e._1 < dateStr).head._2
  }

}