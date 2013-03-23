# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table `members` (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`login` varchar(50) NOT NULL,`email` VARCHAR(254) NOT NULL,`password` VARCHAR(254) NOT NULL,`uid` VARCHAR(254) NOT NULL,`suscribeDate` BIGINT NOT NULL,`lastConnectDate` BIGINT NOT NULL,`isActive` BOOLEAN NOT NULL,`sex` varchar(1),`firstName` VARCHAR(254),`lastName` VARCHAR(254),`birthDate` BIGINT,`city` varchar(100),`description` varchar(255),`mainPicture` BIGINT,`securityKey` VARCHAR(254),`token` VARCHAR(254));
create table `photos` (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`login` VARCHAR(254) NOT NULL,`contentType` VARCHAR(254) NOT NULL,`email` INTEGER NOT NULL,`password` BOOLEAN NOT NULL);

# --- !Downs

drop table `members`;
drop table `photos`;

