package models

import org.joda.time.DateTime
import play.api.db.slick.Config.driver.simple._

/**
 * Search members
 * @param login
 * @param firstName
 * @param lastName
 * @param sex (h/f)
 * @param city
 * @param age
 */
case class MemberSearch(
  login: OptString = None,
  firstName: OptString = None,
  lastName: OptString = None,
  sex: OptString = None,
  city: OptString = None,
  age: OptInt = None
)

object MemberSearch {

  def apply(filters: MemberSearch) = {

    val slickFilters = List[Condition[_]](
      Condition[String](filters.firstName, (t, v) => t.firstName.toLowerCase like '%' + v.toLowerCase + '%'),
      Condition[String](filters.lastName, (t, v) => t.lastName.toLowerCase like  '%' + v.toLowerCase + '%'),
      Condition[String](filters.sex, (t, v) => t.sex === v),
      Condition[String](filters.city, (t, v) => t.city === v),
      Condition[Int](filters.age, (t, v) =>
        t.birthDate >= DateTime.now.minusYears(v+1).getMillis &&
        t.birthDate <= DateTime.now.minusYears(v).getMillis)
    )

    val baseQuery = Query(Member).filter(_.isActive === true)
    slickFilters
      .filter(_.value.isDefined)
      .foldLeft(baseQuery)((query, condition) => query.filter(condition.toFilter))
  }

  private case class Condition[T](value: Option[T], condition: (Member.type, T) => Column[Boolean]) {
    def toFilter(t: Member.type) = condition(t, value.get)
  }
}
