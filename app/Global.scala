
import play.api._
import play.api.Mode._
import play.api.libs.ws._
import scala.util.Random

import db.DB
import anorm._

import play.api.libs.concurrent.Execution.Implicits._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    if (app.mode == Dev && models.Membre.findOneRandom().isEmpty) {
      MockData.execute()
    }
    loadSqlFunctions(app)
  }

  private def loadSqlFunctions(implicit app: Application) = {
    Logger.info("# Chargement des fonctions mysql")
    loadFile("evolutions/functions_mysql.sql").map(sql =>
      DB.withConnection { implicit session =>
        sql.split("#--").foreach(sqlPart => session.prepareStatement(sqlPart).execute())
      }
    ).getOrElse(Logger.error("Impossible de trouver le fichier conf/evolutions/functions_mysql.sql"))
  }

  private def loadFile(filePath: String)(implicit app: Application): Option[String] = {
    Play.resourceAsStream(filePath).map(is =>
      scala.io.Source.fromInputStream(is).getLines().mkString("\n")
    )
  }
}

object MockData {

  import play.api.Play.current
  import db.slick.DB
  import models.{Lieu, Membre}


  val url = "http://localhost:9000"

  val members = for (i <- 1 to 20) yield s"membre$i"
  //val files = (for (i <- 1 to 6) yield current.getExistingFile(s"/data/fake/$i.jpg")).flatten.toList

  val prenoms = List("Julien", "Marion", "Romain", "Jason", "Laurent", "Sebastien", "Yannick", "Clara", "Lea", "Sofia", "Marc", "Thibault", "Xavier", "Alex", "Thomas", "Valérie", "Julie")
  val noms = List("Lafont", "Felix", "Maneschi", "Durand", "Dupont", "Martin", "Loisy", "Piallat", "Simmonet", "Mailol")
  val datesN = List("1989-06-01", "1980-12-12", "1985-03-02", "1983-02-04", "1989-04-03", "1989-07-03", "1989-08-02")
  val villes = List("Montpellier", "Lunel", "Castelnau Le Lez", "Montferriez-le-lez", "Nimes", "Lattes", "Le Crès")

  def execute() {
    Logger.info("Application démarée en mode DEV, chargement des données de test...")

    createLieux()
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

  def createLieux() = {
    println("# Création des lieux")
    val lieux = List(
      Lieu(None, "Irish Corner", "147 Avenue de Boirargues", "34000", "Montpellier", 43.604698181152344, 3.899940013885498),
      Lieu(None, "La Voile Bleue", "avenue du Grand Travers", "34280", "La Grande-Motte", 43.557098388671875, 4.0447998046875),
      Lieu(None, "Effet Mer", "Zone d'aménagement concerté Grand Travers", "34280", "La Grande-Motte", 43.55870056152344, 4.047989845275879),
      Lieu(None, "The O'liver", "Place Venise", "34000", "Montpellier", 43.60369873046875, 3.9185800552368164),
      Lieu(None, "New Marine", "1 Quai Paul Cunq", "34250", "Palavas-les-Flots", 43.526100158691406, 3.9344000816345215),
      Lieu(None, "Le New", "Espace Latipolia, Allée de la Calade", "34970", "Lattes", 43.5724983215332, 3.8945300579071045),
      Lieu(None, "Bab", "Route Palavas", "34970", "Lattes", 43.564300537109375, 3.8944900035858154),
      Lieu(None, "Latipolia", "Rout De Palavas", "34970", "Lattes", 43.564300537109375, 3.8944900035858154),
      Lieu(None, "Café Joseph", "3 Place Jean Jaurès", "34000", "Montpellier", 43.61009979248047, 3.878000020980835),
      Lieu(None, "Restaurant LE MOA", "1006 Rue de la Croix Verte", "34090", "Montpellier", 43.643001556396484, 3.8429698944091797),
      Lieu(None, "La Villa Rouge", "Route de Palavas", "34970", "Lattes", 43.57889938354492, 3.897599935531616),
      Lieu(None, "Le chalet suisse", "Avenue Marcel Pagnol", "34970", "Lattes", 43.58440017700195, 3.9340500831604004),
      Lieu(None, "La Chichoumeille", "390 Chemin Cauquilloux", "34170", "Castelnau-le-Lez", 43.62300109863281, 3.9177498817443848),
      Lieu(None, "La Pépètte", "16 Rue du Cours Complémentaire", "34160", "Castries", 43.67829895019531, 3.9823999404907227),
      Lieu(None, "Les Artistes", "ZAC Des Commandeurs", "34970", "Lattes", 43.58539962768555, 3.9331300258636475),
      Lieu(None, "Circus", "3 Rue Collot", "34000", "Montpellier", 43.61000061035156, 3.8779799938201904),
      Lieu(None, "Aqualand", "2 Allée de la Découverte", "34300", "Cap d'Agde", 43.27980041503906, 3.4970600605010986),
      Lieu(None, "Gaumont Multiplexe", "235 Rue Georges Méliès", "34000", "Montpellier", 43.60419845581055, 3.915519952774048),
      Lieu(None, "La Banane", "37 Boulevard Sarrail", "34250", "Palavas-les-Flots", 43.53030014038086, 3.9402101039886475)
    )
    lieux.foreach(lieu => Lieu.insert(lieu))
    println("Lieux ajoutés: "+lieux.map(_.nom).mkString(", "))
  }

  def rand[T](list: List[T]): T = {
    val index = scala.util.Random.nextInt(list.size)
    list(index)
  }

  def randS[T](list: List[T]): Seq[T] = {
    Seq(rand(list))
  }
}