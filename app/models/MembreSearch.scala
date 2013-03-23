package models

import org.joda.time.DateTime
import play.api.db.slick.Config.driver.simple._

/**
 * Search members
 * @param login
 * @param prenom
 * @param nom
 * @param sexe (h/f)
 * @param ville
 * @param age
 */
case class MembreSearch(
  login: OptString = None,
  prenom: OptString = None,
  nom: OptString = None,
  sexe: OptString = None,
  ville: OptString = None,
  age: OptInt = None
)

object MembreSearch {

  def apply(filters: MembreSearch) = {

    val slickFilters = List[Condition[_]](
      Condition[String](filters.prenom, (t, v) => t.prenom.toLowerCase like '%' + v.toLowerCase + '%'),
      Condition[String](filters.nom, (t, v) => t.nom.toLowerCase like  '%' + v.toLowerCase + '%'),
      Condition[String](filters.sexe, (t, v) => t.sexe === v),
      Condition[String](filters.ville, (t, v) => t.ville === v),
      Condition[Int](filters.age, (t, v) =>
        t.dateNaissance >= DateTime.now.minusYears(v+1).getMillis &&
        t.dateNaissance <= DateTime.now.minusYears(v).getMillis)
    )

    val baseQuery = Query(Membre).filter(_.estActif === true)
    slickFilters
      .filter(_.value.isDefined)
      .foldLeft(baseQuery)((query, condition) => query.filter(condition.toFilter))
  }

  private case class Condition[T](value: Option[T], condition: (Membre.type, T) => Column[Boolean]) {
    def toFilter(t: Membre.type) = condition(t, value.get)
  }
}
