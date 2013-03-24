# Installation API TwagIn

## Pré-requis

 * [Play 2.1.0](http://downloads.typesafe.com/play/2.1.0/play-2.1.0.zip) installé (cf [Installig Play](http://www.playframework.com/documentation/2.1.0/Installing))
 * Base de données MYSQL installée
 * Pour faciliter les tests, [HTTPie](https://github.com/jkbr/httpie) est recommandé

## Lancer l'API
 * Cloner le dépot git : `git clone TODO`
 * Configurer les accès à la base de données dans le fichier `conf/applications.conf`

        db.default.url="jdbc:mysql://HOTE/BASE"
        db.default.user="LOGIN"
        db.default.password="PASSWORD"
 * Lancer l'application `play run`
 
 L'application est disponible sur le port 9000 par défaut : [http://localhost:9000](http://localhost:9000)
 
 Des utilisateurs sont créés automatiquement au lancement de l'application. Utilisez (login:membre1, password:membre1) par exemple.

## Développer pour l'API

 * Suivre la procédure "Lancer l'API"
 * Rentrer dans la console play en exécutant `play` dans le répertoire du projet (cf [Using Play Console](http://www.playframework.com/documentation/2.1.0/PlayConsole))
 * *Facultatif* : Intégrer le projet à votre IDE en exécutant : `eclipse` ou `idea` (cf [Integrating IDE](http://www.playframework.com/documentation/2.1.0/IDE))
 * Exécuter Play en mode développement avec rechargement automatique en exécutant `play ~run`
 
## Règles de développement et utilisation de GIT

* Ne poussez que si le code marche. Par contre vous pouvez commiter des choses invalides ou non finies sur votre dépôt local. Il est conseillé de squasher les dits commits avant de les envoyer.
* Si vous implémentez un WS, n'oubliez pas de le documenter
* Si vous ajoutez une entité, créez des données de test qui vont avec (si possible en appelant votre WS, ça permet de tester que tout va bien)
* Les entités (= tables en base) doivent être en français.

Interdiction formelle d'exécuter `git pull`, faites obligatoirement `git pull --rebase` ;)
S'il refuse, c'est que vous n'avez pas stashé (`git stash`) votre wip.
 