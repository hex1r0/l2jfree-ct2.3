-- ---------------------------
-- Table structure for raidboss_points
-- ---------------------------

CREATE TABLE IF NOT EXISTS `raidboss_points` (
  `owner_id` int(11) unsigned NOT NULL,
  `boss_id` int(11) unsigned NOT NULL,
  `points` int(11) NOT NULL default '0',
  PRIMARY KEY(`owner_id`, `boss_id`)
);
