# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table `membres` (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`login` varchar(50) NOT NULL,`email` VARCHAR(254) NOT NULL,`password` VARCHAR(254) NOT NULL,`uid` VARCHAR(254) NOT NULL,`dateInscription` BIGINT NOT NULL,`dateConnexion` BIGINT NOT NULL,`estActif` BOOLEAN NOT NULL,`sexe` varchar(1),`prenom` VARCHAR(254),`nom` VARCHAR(254),`dateNaissance` BIGINT,`ville` varchar(100),`description` varchar(255),`photoProfil` BIGINT,`securityKey` VARCHAR(254),`token` VARCHAR(254));
create table `photos` (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`login` VARCHAR(254) NOT NULL,`contentType` VARCHAR(254) NOT NULL,`signalement` INTEGER NOT NULL,`estAffiche` BOOLEAN NOT NULL);

# --- !Downs

drop table `membres`;
drop table `photos`;

