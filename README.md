# Documentation API TwagIn

## Banalités sur les échanges avec le WS

* Toutes les réponses de l'API sont en JSON
* Les données des requêtes PUT ou POST doivent être envoyées avec le header `Content-Type: application/x-www-form-urlencoded`
* Le status HTTP de la réponse est primordial !
  * `200` OK
  * `404` Ressource introuvable
  * `403` Accès interdit (vérifier le token !)
  * `400` Erreur de requête (mauvais paramètres) ou impossible d'effectuer la requête
  * `500` Erreur 
* Le flux json des réponses est normalisé :

	    {
            meta: {
                code: "Code retour HTTP",
                error: "Description de l'erreur ou null si pas d'erreur",
            },
            result: "réponse, ou null en cas d'erreur"
        }
* Une réponse `200` sans contenu n'est pas un bug, juste qu'il n'y avait rien de spécial à dire !
* Les dates doivent être transmises au format ISO 8601 (ex: `2013-04-22T14:00:00+01:00` pour les DateTime, et `2013-04-22` pour les Date)
* Les API paginées sont normalisées via 2 paramètres GET
  * `?offset=X`: Index du premier élément à afficher (default: 0)
  * `?limit=X`: Nombre d'éléments par page (default: 20, max: 100)
* Pourquoi utiliser tous ces verbes HTTP : Post, Put, Get, Delete ?
  * En 1 mot : [RESTful](http://en.wikipedia.org/wiki/Representational_state_transfer)
  * GET : Opération sans effet de bord - Réprésentation d'une ressource
  * PUT : Opération avec effet de bord - [Idempotente](http://fr.wikipedia.org/wiki/Idempotence#En_informatique)
  * POST : Opération avec effet de bord - Non idempotente
  * DELETE : Opération de suppression d'une ressource


## Sécurisation de l'API

Les api sécurisées nécessitent un TOKEN pour être autorisées. Dans le cas contraire, une erreur 403 (Forbidden) est renvoyée.

Le token se récupère en effectuant une tentative d'authentification sur `/api/v1/members/authenticate`.

Le token doit ensuite être transmis sur chaque requête :

* **Méthode recommandée** : Dans les headers de la requête, en ajoutant un header `Token: xxxxxxxxxx` 
* Dans les paramètres d'url : `?token=xxxxxxxxxx`

*Tips* : en mode `DEV` , il est possible de se passer du token. Dans ce cas, le premier compte enregistré est utilisé.

# API Membres : *Gestion des membres*

Pour exécuter ces commandes, utilisez le client HTTP [HTTPie](https://github.com/jkbr/httpie).

### Voir le profil d'un membre / Voir mon profil `sécurisée`

    http GET http://localhost:9000/api/v1/members/1
    http GET http://localhost:9000/api/v1/members/me

Réponse
    
>     TODO

### Vérifier les données avant inscription

    http PUT http://localhost:9000/api/v1/members/checkSubscription login=julien email=a.b@c.fr -f

>    * 200 si ok
>    * 400 en cas d'erreur

### Création d'un nouveau compte

    http POST http://localhost:9000/api/v1/members/subscribe login=titi email=a.a@aa.ab password=a -f

>     { id: 12 } // Identifiant unique assigné

### Authentification

    http POST http://localhost:9000/api/v1/members/authenticate login=titi password=a -f

>     200 / 400

### Uploader ma photo principale `sécurisée`

    http POST http://localhost:9000/api/v1/members/uploadMainPicture picture@~/Pictures/IMG_0010.jpg -f --follow

>     Idem "My profile"

### Mettre à jour mes informations `sécurisée`

Paramètres : prenom, nom, description, sexe (h/f), dateNaissance, ville (facultatifs)

    http PUT http://localhost:9000/api/v1/members/update firstName=Julien -f

>     Idem "My profile"

### Rechercher des membres `sécurisée` `paginée`

Paramètres : login, prenom, nom, sexe (h/f), ville, age (facultatifs)

    http GET http://localhost:9000/api/v1/members/search?login=titi
    
>     [ List "MyProfile" ]

# Assets

### Afficher une photo

    http GET http://localhost:9000/api/v1/assets/[path]

# API Lieux

### Lister les lieux proches de ma position `paginée` `sécurisée`

TODO : Rajouter plus de filtres

    http GET http://localhost:9000/api/v1/lieux/search lt==43.6 lg==3.9
