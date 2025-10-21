drop database if exists aggregator;
create database if not exists aggregator;
GRANT ALL PRIVILEGES ON aggregator.* TO 'cache'@'localhost' identified by 'plenty';
use aggregator;
delimiter |

ALTER DATABASE aggregator  DEFAULT COLLATE utf8mb4_unicode_ci|

CREATE TABLE if not exists `archive_register` (
  `id` int(11)  NOT NULL AUTO_INCREMENT,
  `hostname` varchar(150) DEFAULT NULL,
  `timegate` varchar(255) DEFAULT NULL,
  `timemap` varchar(255) DEFAULT NULL,
   PRIMARY KEY (`id`)
) ENGINE=MyISAM|
ALTER TABLE archive_register
 ADD CONSTRAINT  pk_host UNIQUE (hostname)|

CREATE TABLE if not exists `jobs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `url` varchar(100) DEFAULT NULL,
  `reqtime` datetime DEFAULT NULL,
  `process_id` int(11) DEFAULT NULL,
  `compltime` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM|
CREATE INDEX url_index ON jobs (url)|
CREATE INDEX p_index ON jobs (process_id)|

 CREATE TABLE if not exists `links` (
  `id` varchar(32) NOT NULL,
  `mdate` datetime,
  `archive_id` int(11) DEFAULT NULL,
  `href` varchar(2050) DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,
  `rel` varchar(50) DEFAULT NULL,
  `part` int(11) NOT NULL,
   CONSTRAINT pk_link PRIMARY KEY (id,mdate,archive_id,part)
) ENGINE=MyISAM
/*!50100 PARTITION BY LIST (part)
(PARTITION p0 VALUES IN (48) ENGINE = MyISAM,
 PARTITION p1 VALUES IN (49) ENGINE = MyISAM,
 PARTITION p2 VALUES IN (50) ENGINE = MyISAM,
 PARTITION p3 VALUES IN (51) ENGINE = MyISAM,
 PARTITION p4 VALUES IN (52) ENGINE = MyISAM,
 PARTITION p5 VALUES IN (53) ENGINE = MyISAM,
 PARTITION p6 VALUES IN (54) ENGINE = MyISAM,
 PARTITION p7 VALUES IN (55) ENGINE = MyISAM,
 PARTITION p8 VALUES IN (56) ENGINE = MyISAM,
 PARTITION p9 VALUES IN (57) ENGINE = MyISAM,  
 PARTITION pa VALUES IN (97) ENGINE = MyISAM,
 PARTITION pb VALUES IN (98) ENGINE = MyISAM,
 PARTITION pc VALUES IN (99) ENGINE = MyISAM,
 PARTITION pd VALUES IN (100) ENGINE = MyISAM,
 PARTITION pe VALUES IN (101) ENGINE = MyISAM,
 PARTITION pf VALUES IN (102) ENGINE = MyISAM) */ |
 
 CREATE TABLE if not exists `linkmaster` (
  `url` varchar(100) DEFAULT NULL,
  `id` char(32) DEFAULT NULL,
  `reqtime` datetime DEFAULT NULL,
  `numreq` int(11) DEFAULT NULL,
  `updtime` datetime DEFAULT NULL
) ENGINE=MyISAM|

ALTER TABLE linkmaster
 ADD CONSTRAINT  pk_id PRIMARY KEY (id) |

 CREATE INDEX url_index_m ON linkmaster (url)|
 CREATE INDEX update_index_m ON linkmaster (updtime)|

CREATE DEFINER=`cache`@`localhost` TRIGGER part_update BEFORE UPDATE ON `links`
 FOR EACH ROW
  BEGIN 
  SET NEW.part = ASCII(SUBSTR(NEW.id,1,1));
  END |


CREATE DEFINER=`cache`@`localhost` TRIGGER part_insert BEFORE INSERT ON `links`
FOR EACH ROW
BEGIN
SET NEW.part = ASCII(SUBSTR(NEW.id,1,1));
END |

delimiter ;

