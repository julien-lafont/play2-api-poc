import controllers.api.routes
import models.{Photo, Membre}
import org.joda.time.format.ISODateTimeFormat
import play.api._
import play.api.Mode._
import play.api.libs.ws._
import scala.util.Random

import play.api.Play.current
import slick.lifted.Query

import play.api.libs.concurrent.Execution.Implicits._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    if (app.mode == Dev && Membre.findOneRandom().isEmpty) {
      MockData.execute()
    }
  }

}

object MockData {

  val url = "http://localhost:9000"

  val members = for (i <- 1 to 20) yield s"membre$i"
  //val files = (for (i <- 1 to 6) yield current.getExistingFile(s"/data/fake/$i.jpg")).flatten.toList

  val prenoms = List("Julien", "Marion", "Romain", "Jason", "Laurent", "Sebastien", "Yannick", "Clara", "Lea", "Sofia", "Marc", "Thibault", "Xavier", "Alex", "Thomas", "Valérie", "Julie")
  val noms = List("Lafont", "Felix", "Maneschi", "Durand", "Dupont", "Martin", "Loisy", "Piallat", "Simmonet", "Mailol")
  val datesN = List("1989-06-01", "1980-12-12", "1985-03-02", "1983-02-04", "1989-04-03", "1989-07-03", "1989-08-02")
  val villes = List("Montpellier", "Lunel", "Castelnau Le Lez", "Montferriez-le-lez", "Nimes", "Lattes", "Le Crès")

  def execute() {
    Logger.info("Application démarée en mode DEV, chargement des données de test...")

    createMembers()
  }

  def createMembers() {
    Logger.info("# Création des membres")

    members.foreach { login =>
      WS.url(s"$url/api/v1/members/subscribe").post(Map(
        "login" -> Seq(login),
        "email" -> Seq(s"$login@twagin.com"),
        "password" -> Seq(login)
      )).map{ response =>

        val membre = Membre.findByLogin(login).get
        val token = Membre.authenticate(membre).get.token

        val (prenom, nom, sexe, ville) = (rand(prenoms), rand(noms), if (Random.nextBoolean()) "h" else "f", rand(villes))

        WS.url(s"$url/api/v1/members/update?token=$token").put(Map(
          "prenom" -> Seq(prenom),
          "nom" -> Seq(nom),
          "sexe" -> Seq(sexe),
          "dateNaissance" -> Seq(rand(datesN)),
          "ville" -> Seq(ville),
          "description" -> Seq("Lorem ipsum dolor sit amet")
        ))

        //val file = rand(files)
        //Logger.info(s"[$login] Photo de profil = ${file.getName()}")
        //WS.url(s"$url/api/v1/members/updateMainPictureMock?token=$token")

        Logger.info(s"login=$login, password=$login, token=$token")
      }
    }
  }


  def rand[T](list: List[T]): T = {
    val index = scala.util.Random.nextInt(list.size)
    list(index)
  }

  def randS[T](list: List[T]): Seq[T] = {
    Seq(rand(list))
  }
}