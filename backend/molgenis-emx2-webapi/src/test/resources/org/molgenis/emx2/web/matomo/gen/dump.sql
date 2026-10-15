/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-11.4.13-MariaDB, for debian-linux-gnu (aarch64)
--
-- Host: localhost    Database: matomo
-- ------------------------------------------------------
-- Server version	11.4.13-MariaDB-ubu2404

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Table structure for table `matomo_access`
--

DROP TABLE IF EXISTS `matomo_access`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_access` (
  `idaccess` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `login` varchar(100) NOT NULL,
  `idsite` int(10) unsigned NOT NULL,
  `access` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`idaccess`),
  KEY `index_loginidsite` (`login`,`idsite`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_access`
--

LOCK TABLES `matomo_access` WRITE;
/*!40000 ALTER TABLE `matomo_access` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_access` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_annotations`
--

DROP TABLE IF EXISTS `matomo_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_annotations` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `idsite` int(10) unsigned NOT NULL,
  `date` datetime NOT NULL,
  `note` text NOT NULL,
  `starred` tinyint(1) NOT NULL DEFAULT 0,
  `user` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_idsite_date` (`idsite`,`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_annotations`
--

LOCK TABLES `matomo_annotations` WRITE;
/*!40000 ALTER TABLE `matomo_annotations` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_annotations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_archive_invalidations`
--

DROP TABLE IF EXISTS `matomo_archive_invalidations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_archive_invalidations` (
  `idinvalidation` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `idarchive` int(10) unsigned DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `idsite` int(10) unsigned NOT NULL,
  `date1` date NOT NULL,
  `date2` date NOT NULL,
  `period` tinyint(3) unsigned NOT NULL,
  `ts_invalidated` datetime DEFAULT NULL,
  `ts_started` datetime DEFAULT NULL,
  `status` tinyint(1) unsigned DEFAULT 0,
  `report` varchar(255) DEFAULT NULL,
  `processing_host` varchar(100) DEFAULT NULL,
  `process_id` varchar(15) DEFAULT NULL,
  PRIMARY KEY (`idinvalidation`),
  KEY `index_idsite_dates_period_name` (`idsite`,`date1`,`period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_archive_invalidations`
--

LOCK TABLES `matomo_archive_invalidations` WRITE;
/*!40000 ALTER TABLE `matomo_archive_invalidations` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_archive_invalidations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_archiving_metrics`
--

DROP TABLE IF EXISTS `matomo_archiving_metrics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_archiving_metrics` (
  `metadataid` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `idarchive` bigint(20) unsigned NOT NULL,
  `idsite` int(10) unsigned NOT NULL,
  `archive_name` varchar(255) NOT NULL,
  `date1` date NOT NULL,
  `date2` date NOT NULL,
  `period` tinyint(3) unsigned NOT NULL,
  `ts_started` datetime NOT NULL,
  `ts_finished` datetime NOT NULL,
  `total_time` bigint(20) unsigned NOT NULL,
  `total_time_exclusive` bigint(20) unsigned NOT NULL,
  `is_temporary` tinyint(1) unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`metadataid`),
  KEY `index_idarchive` (`idarchive`),
  KEY `index_idsite_archive_name` (`idsite`,`archive_name`),
  KEY `index_idsite_date1_period` (`idsite`,`date1`,`period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_archiving_metrics`
--

LOCK TABLES `matomo_archiving_metrics` WRITE;
/*!40000 ALTER TABLE `matomo_archiving_metrics` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_archiving_metrics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_brute_force_log`
--

DROP TABLE IF EXISTS `matomo_brute_force_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_brute_force_log` (
  `id_brute_force_log` bigint(11) NOT NULL AUTO_INCREMENT,
  `ip_address` varchar(60) DEFAULT NULL,
  `attempted_at` datetime NOT NULL,
  `login` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_brute_force_log`),
  KEY `index_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_brute_force_log`
--

LOCK TABLES `matomo_brute_force_log` WRITE;
/*!40000 ALTER TABLE `matomo_brute_force_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_brute_force_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_changes`
--

DROP TABLE IF EXISTS `matomo_changes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_changes` (
  `idchange` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `created_time` datetime NOT NULL,
  `plugin_name` varchar(60) NOT NULL,
  `version` varchar(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `link_name` varchar(255) DEFAULT NULL,
  `link` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`idchange`),
  UNIQUE KEY `unique_plugin_version_title` (`plugin_name`,`version`,`title`(100))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_changes`
--

LOCK TABLES `matomo_changes` WRITE;
/*!40000 ALTER TABLE `matomo_changes` DISABLE KEYS */;
INSERT INTO `matomo_changes` VALUES
(1,'2026-10-15 09:59:35','UsersManager','5.9.0','New: Dark Mode is here','Switch between Light and Dark themes from your Personal Settings to suit your workspace. Your preference is saved to your profile and follows you across devices for a more comfortable, accessible Matomo experience.','Switch to Dark Mode','index.php?module=UsersManager&action=userSettings&idSite=1');
/*!40000 ALTER TABLE `matomo_changes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_custom_dimensions`
--

DROP TABLE IF EXISTS `matomo_custom_dimensions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_custom_dimensions` (
  `idcustomdimension` bigint(20) unsigned NOT NULL,
  `idsite` bigint(20) unsigned NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(1000) NOT NULL DEFAULT '',
  `index` smallint(5) unsigned NOT NULL,
  `scope` varchar(10) NOT NULL,
  `active` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `extractions` text NOT NULL DEFAULT '',
  `case_sensitive` tinyint(3) unsigned NOT NULL DEFAULT 1,
  PRIMARY KEY (`idcustomdimension`,`idsite`),
  UNIQUE KEY `uniq_hash` (`idsite`,`scope`,`index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_custom_dimensions`
--

LOCK TABLES `matomo_custom_dimensions` WRITE;
/*!40000 ALTER TABLE `matomo_custom_dimensions` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_custom_dimensions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_goal`
--

DROP TABLE IF EXISTS `matomo_goal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_goal` (
  `idsite` int(11) NOT NULL,
  `idgoal` int(11) NOT NULL,
  `name` varchar(50) NOT NULL,
  `description` varchar(255) NOT NULL DEFAULT '',
  `match_attribute` varchar(20) NOT NULL,
  `pattern` varchar(255) NOT NULL,
  `pattern_type` varchar(25) NOT NULL,
  `case_sensitive` tinyint(4) NOT NULL,
  `allow_multiple` tinyint(4) NOT NULL,
  `revenue` double NOT NULL,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  `event_value_as_revenue` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`idsite`,`idgoal`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_goal`
--

LOCK TABLES `matomo_goal` WRITE;
/*!40000 ALTER TABLE `matomo_goal` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_goal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_locks`
--

DROP TABLE IF EXISTS `matomo_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_locks` (
  `key` varchar(70) NOT NULL,
  `value` varchar(255) DEFAULT NULL,
  `expiry_time` bigint(20) unsigned DEFAULT 9999999999,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_locks`
--

LOCK TABLES `matomo_locks` WRITE;
/*!40000 ALTER TABLE `matomo_locks` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_locks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_log_action`
--

DROP TABLE IF EXISTS `matomo_log_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_log_action` (
  `idaction` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(4096) DEFAULT NULL,
  `hash` int(10) unsigned NOT NULL,
  `type` tinyint(3) unsigned DEFAULT NULL,
  `url_prefix` tinyint(2) DEFAULT NULL,
  PRIMARY KEY (`idaction`),
  KEY `index_type_hash` (`type`,`hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_log_action`
--

LOCK TABLES `matomo_log_action` WRITE;
/*!40000 ALTER TABLE `matomo_log_action` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_log_action` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_log_bot_request`
--

DROP TABLE IF EXISTS `matomo_log_bot_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_log_bot_request` (
  `idrequest` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `idsite` int(10) unsigned NOT NULL,
  `server_time` datetime NOT NULL,
  `created_time` datetime NOT NULL DEFAULT current_timestamp(),
  `idaction_url` int(10) unsigned DEFAULT NULL,
  `bot_name` varchar(100) NOT NULL,
  `bot_type` varchar(50) NOT NULL,
  `http_status_code` smallint(5) unsigned DEFAULT NULL,
  `response_size_bytes` int(10) unsigned DEFAULT NULL,
  `response_time_ms` int(10) unsigned DEFAULT NULL,
  `source` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`idrequest`),
  KEY `index_idsite_server_time` (`idsite`,`server_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_log_bot_request`
--

LOCK TABLES `matomo_log_bot_request` WRITE;
/*!40000 ALTER TABLE `matomo_log_bot_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_log_bot_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_log_conversion`
--

DROP TABLE IF EXISTS `matomo_log_conversion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_log_conversion` (
  `idvisit` bigint(10) unsigned NOT NULL,
  `idsite` int(10) unsigned NOT NULL,
  `idvisitor` binary(8) NOT NULL,
  `server_time` datetime NOT NULL,
  `idaction_url` int(10) unsigned DEFAULT NULL,
  `idlink_va` bigint(10) unsigned DEFAULT NULL,
  `idgoal` int(10) NOT NULL,
  `buster` int(10) unsigned NOT NULL,
  `idorder` varchar(100) DEFAULT NULL,
  `items` smallint(5) unsigned DEFAULT NULL,
  `url` varchar(4096) NOT NULL,
  `revenue` float DEFAULT NULL,
  `revenue_shipping` double DEFAULT NULL,
  `revenue_subtotal` double DEFAULT NULL,
  `revenue_tax` double DEFAULT NULL,
  `revenue_discount` double DEFAULT NULL,
  `pageviews_before` smallint(5) unsigned DEFAULT NULL,
  `visitor_returning` tinyint(1) DEFAULT NULL,
  `visitor_seconds_since_first` int(11) unsigned DEFAULT NULL,
  `visitor_seconds_since_order` int(11) unsigned DEFAULT NULL,
  `visitor_count_visits` int(11) unsigned NOT NULL DEFAULT 0,
  `referer_keyword` varchar(255) DEFAULT NULL,
  `referer_name` varchar(255) DEFAULT NULL,
  `referer_type` tinyint(1) unsigned DEFAULT NULL,
  `config_browser_name` varchar(40) DEFAULT NULL,
  `config_client_type` tinyint(1) DEFAULT NULL,
  `config_device_brand` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `config_device_model` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `config_device_type` tinyint(100) DEFAULT NULL,
  `location_city` varchar(255) DEFAULT NULL,
  `location_country` char(3) DEFAULT NULL,
  `location_latitude` decimal(9,6) DEFAULT NULL,
  `location_longitude` decimal(9,6) DEFAULT NULL,
  `location_region` char(3) DEFAULT NULL,
  `custom_dimension_1` varchar(255) DEFAULT NULL,
  `custom_dimension_2` varchar(255) DEFAULT NULL,
  `custom_dimension_3` varchar(255) DEFAULT NULL,
  `custom_dimension_4` varchar(255) DEFAULT NULL,
  `custom_dimension_5` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`idvisit`,`idgoal`,`buster`),
  UNIQUE KEY `unique_idsite_idorder` (`idsite`,`idorder`),
  KEY `index_idsite_datetime` (`idsite`,`server_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_log_conversion`
--

LOCK TABLES `matomo_log_conversion` WRITE;
/*!40000 ALTER TABLE `matomo_log_conversion` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_log_conversion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_log_conversion_item`
--

DROP TABLE IF EXISTS `matomo_log_conversion_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_log_conversion_item` (
  `idsite` int(10) unsigned NOT NULL,
  `idvisitor` binary(8) NOT NULL,
  `server_time` datetime NOT NULL,
  `idvisit` bigint(10) unsigned NOT NULL,
  `idorder` varchar(100) NOT NULL,
  `idaction_sku` int(10) unsigned NOT NULL,
  `idaction_name` int(10) unsigned NOT NULL,
  `idaction_category` int(10) unsigned NOT NULL,
  `idaction_category2` int(10) unsigned NOT NULL,
  `idaction_category3` int(10) unsigned NOT NULL,
  `idaction_category4` int(10) unsigned NOT NULL,
  `idaction_category5` int(10) unsigned NOT NULL,
  `price` double NOT NULL,
  `quantity` int(10) unsigned NOT NULL,
  `deleted` tinyint(1) unsigned NOT NULL,
  PRIMARY KEY (`idvisit`,`idorder`,`idaction_sku`),
  KEY `index_idsite_servertime` (`idsite`,`server_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_log_conversion_item`
--

LOCK TABLES `matomo_log_conversion_item` WRITE;
/*!40000 ALTER TABLE `matomo_log_conversion_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_log_conversion_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_log_link_visit_action`
--

DROP TABLE IF EXISTS `matomo_log_link_visit_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_log_link_visit_action` (
  `idlink_va` bigint(10) unsigned NOT NULL AUTO_INCREMENT,
  `idsite` int(10) unsigned NOT NULL,
  `idvisitor` binary(8) NOT NULL,
  `idvisit` bigint(10) unsigned NOT NULL,
  `idaction_url_ref` int(10) unsigned DEFAULT 0,
  `idaction_name_ref` int(10) unsigned DEFAULT NULL,
  `custom_float` double DEFAULT NULL,
  `pageview_position` mediumint(8) unsigned DEFAULT NULL,
  `server_time` datetime NOT NULL,
  `idpageview` char(6) DEFAULT NULL,
  `idaction_name` int(10) unsigned DEFAULT NULL,
  `idaction_url` int(10) unsigned DEFAULT NULL,
  `search_cat` varchar(200) DEFAULT NULL,
  `search_count` int(10) unsigned DEFAULT NULL,
  `time_spent_ref_action` int(10) unsigned DEFAULT NULL,
  `idaction_product_cat` int(10) unsigned DEFAULT NULL,
  `idaction_product_cat2` int(10) unsigned DEFAULT NULL,
  `idaction_product_cat3` int(10) unsigned DEFAULT NULL,
  `idaction_product_cat4` int(10) unsigned DEFAULT NULL,
  `idaction_product_cat5` int(10) unsigned DEFAULT NULL,
  `idaction_product_name` int(10) unsigned DEFAULT NULL,
  `product_price` double DEFAULT NULL,
  `idaction_product_sku` int(10) unsigned DEFAULT NULL,
  `idaction_event_action` int(10) unsigned DEFAULT NULL,
  `idaction_event_category` int(10) unsigned DEFAULT NULL,
  `idaction_content_interaction` int(10) unsigned DEFAULT NULL,
  `idaction_content_name` int(10) unsigned DEFAULT NULL,
  `idaction_content_piece` int(10) unsigned DEFAULT NULL,
  `idaction_content_target` int(10) unsigned DEFAULT NULL,
  `time_dom_completion` mediumint(10) unsigned DEFAULT NULL,
  `time_dom_processing` mediumint(10) unsigned DEFAULT NULL,
  `time_network` mediumint(10) unsigned DEFAULT NULL,
  `time_on_load` mediumint(10) unsigned DEFAULT NULL,
  `time_server` mediumint(10) unsigned DEFAULT NULL,
  `time_transfer` mediumint(10) unsigned DEFAULT NULL,
  `time_spent` int(10) unsigned DEFAULT NULL,
  `custom_dimension_1` varchar(255) DEFAULT NULL,
  `custom_dimension_2` varchar(255) DEFAULT NULL,
  `custom_dimension_3` varchar(255) DEFAULT NULL,
  `custom_dimension_4` varchar(255) DEFAULT NULL,
  `custom_dimension_5` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`idlink_va`),
  KEY `index_idvisit` (`idvisit`),
  KEY `index_idsite_servertime` (`idsite`,`server_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_log_link_visit_action`
--

LOCK TABLES `matomo_log_link_visit_action` WRITE;
/*!40000 ALTER TABLE `matomo_log_link_visit_action` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_log_link_visit_action` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_log_profiling`
--

DROP TABLE IF EXISTS `matomo_log_profiling`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_log_profiling` (
  `query` text NOT NULL,
  `count` int(10) unsigned DEFAULT NULL,
  `sum_time_ms` float DEFAULT NULL,
  `idprofiling` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`idprofiling`),
  UNIQUE KEY `query` (`query`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_log_profiling`
--

LOCK TABLES `matomo_log_profiling` WRITE;
/*!40000 ALTER TABLE `matomo_log_profiling` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_log_profiling` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_log_visit`
--

DROP TABLE IF EXISTS `matomo_log_visit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_log_visit` (
  `idvisit` bigint(10) unsigned NOT NULL AUTO_INCREMENT,
  `idsite` int(10) unsigned NOT NULL,
  `idvisitor` binary(8) NOT NULL,
  `visit_last_action_time` datetime NOT NULL,
  `config_id` binary(8) NOT NULL,
  `location_ip` varbinary(16) NOT NULL,
  `profilable` tinyint(1) DEFAULT NULL,
  `user_id` varchar(200) DEFAULT NULL,
  `visit_first_action_time` datetime NOT NULL,
  `visit_goal_buyer` tinyint(1) DEFAULT NULL,
  `visit_goal_converted` tinyint(1) DEFAULT NULL,
  `visitor_returning` tinyint(1) DEFAULT NULL,
  `visitor_seconds_since_first` int(11) unsigned DEFAULT NULL,
  `visitor_seconds_since_order` int(11) unsigned DEFAULT NULL,
  `visitor_count_visits` int(11) unsigned NOT NULL DEFAULT 0,
  `visit_entry_idaction_name` int(10) unsigned DEFAULT NULL,
  `visit_entry_idaction_url` int(11) unsigned DEFAULT NULL,
  `visit_exit_idaction_name` int(10) unsigned DEFAULT NULL,
  `visit_exit_idaction_url` int(10) unsigned DEFAULT 0,
  `visit_total_actions` int(11) unsigned DEFAULT NULL,
  `visit_total_interactions` mediumint(8) unsigned DEFAULT 0,
  `visit_total_searches` smallint(5) unsigned DEFAULT NULL,
  `referer_keyword` varchar(255) DEFAULT NULL,
  `referer_name` varchar(255) DEFAULT NULL,
  `referer_type` tinyint(1) unsigned DEFAULT NULL,
  `referer_url` varchar(1500) DEFAULT NULL,
  `location_browser_lang` varchar(20) DEFAULT NULL,
  `config_browser_engine` varchar(10) DEFAULT NULL,
  `config_browser_name` varchar(40) DEFAULT NULL,
  `config_browser_version` varchar(20) DEFAULT NULL,
  `config_client_type` tinyint(1) DEFAULT NULL,
  `config_device_brand` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `config_device_model` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `config_device_type` tinyint(100) DEFAULT NULL,
  `config_os` char(3) DEFAULT NULL,
  `config_os_version` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `visit_total_events` int(11) unsigned DEFAULT NULL,
  `visitor_localtime` time DEFAULT NULL,
  `visitor_seconds_since_last` int(11) unsigned DEFAULT NULL,
  `config_resolution` varchar(18) DEFAULT NULL,
  `config_cookie` tinyint(1) DEFAULT NULL,
  `config_flash` tinyint(1) DEFAULT NULL,
  `config_java` tinyint(1) DEFAULT NULL,
  `config_pdf` tinyint(1) DEFAULT NULL,
  `config_quicktime` tinyint(1) DEFAULT NULL,
  `config_realplayer` tinyint(1) DEFAULT NULL,
  `config_silverlight` tinyint(1) DEFAULT NULL,
  `config_windowsmedia` tinyint(1) DEFAULT NULL,
  `ai_agent_name` varchar(40) DEFAULT NULL,
  `visit_total_time` int(11) unsigned NOT NULL,
  `location_city` varchar(255) DEFAULT NULL,
  `location_country` char(3) DEFAULT NULL,
  `location_latitude` decimal(9,6) DEFAULT NULL,
  `location_longitude` decimal(9,6) DEFAULT NULL,
  `location_region` char(3) DEFAULT NULL,
  `last_idlink_va` bigint(20) unsigned DEFAULT NULL,
  `custom_dimension_1` varchar(255) DEFAULT NULL,
  `custom_dimension_2` varchar(255) DEFAULT NULL,
  `custom_dimension_3` varchar(255) DEFAULT NULL,
  `custom_dimension_4` varchar(255) DEFAULT NULL,
  `custom_dimension_5` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`idvisit`),
  KEY `index_idsite_config_datetime` (`idsite`,`config_id`,`visit_last_action_time`),
  KEY `index_idsite_datetime` (`idsite`,`visit_last_action_time`),
  KEY `index_idsite_idvisitor_time` (`idsite`,`idvisitor`,`visit_last_action_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_log_visit`
--

LOCK TABLES `matomo_log_visit` WRITE;
/*!40000 ALTER TABLE `matomo_log_visit` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_log_visit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_logger_message`
--

DROP TABLE IF EXISTS `matomo_logger_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_logger_message` (
  `idlogger_message` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `tag` varchar(50) DEFAULT NULL,
  `timestamp` timestamp NULL DEFAULT NULL,
  `level` varchar(16) DEFAULT NULL,
  `message` text DEFAULT NULL,
  PRIMARY KEY (`idlogger_message`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_logger_message`
--

LOCK TABLES `matomo_logger_message` WRITE;
/*!40000 ALTER TABLE `matomo_logger_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_logger_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_option`
--

DROP TABLE IF EXISTS `matomo_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_option` (
  `option_name` varchar(191) NOT NULL,
  `option_value` longtext NOT NULL,
  `autoload` tinyint(4) NOT NULL DEFAULT 1,
  PRIMARY KEY (`option_name`),
  KEY `autoload` (`autoload`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_option`
--

LOCK TABLES `matomo_option` WRITE;
/*!40000 ALTER TABLE `matomo_option` DISABLE KEYS */;
INSERT INTO `matomo_option` VALUES
('fingerprint_salt_1_2026-10-13','{\"value\":\"ktka6nu90dk8ov3ge0c9m2l3r3lyymlu\",\"time\":1792058651}',0),
('fingerprint_salt_1_2026-10-14','{\"value\":\"epf1kxu0oza741out9ci0o403xtyh3iy\",\"time\":1792058651}',0),
('fingerprint_salt_1_2026-10-15','{\"value\":\"yc55wm3s5fz8sjf6v5ohgix670fwska7\",\"time\":1792058651}',0),
('fingerprint_salt_1_2026-10-16','{\"value\":\"mrh1tbx0dxzfvao0vl4yltl5421lmzdr\",\"time\":1792058651}',0),
('geoip2.loc_db_url','https://download.db-ip.com/free/dbip-city-lite-2026-10.mmdb.gz',0),
('geoip2.updater_last_run_time','1792022400',0),
('geoip2.updater_period','month',0),
('install_mail_sent','1',0),
('install_version','5.14.0',0),
('LoginFromDifferentCountry.lastCountry.admin','',0),
('Marketplace.unique_id','47abfce56f3e8a8da8c4d81353714525041af6917f92d5de98ac58f877754885',0),
('Marketplace.web_php_version','8.4.26',0),
('MatomoUpdateHistory','5.14.0,',0),
('MobileMessaging_DelegatedManagement','false',0),
('PrivacyManager.ipAnonymizerEnabled','1',0),
('SitesManager_DefaultTimezone','Europe/Amsterdam',0),
('TaskScheduler.timetable','a:1:{s:45:\"Piwik\\Plugins\\GeoIp2\\GeoIP2AutoUpdater.update\";i:1793664026;}',0),
('TransactionLevel.testOption','1',0),
('UpdateCheck_LastCheckFailed','',0),
('UpdateCheck_LastTimeChecked','1792058376',1),
('UpdateCheck_LatestVersion','5.14.1',0),
('usercountry.location_provider','geoip2php',0),
('usercountry.switchtoisoregions','1792058666',0),
('useridsalt','SeESTEhtPY7BjUIa0eJh7Ra$AXp5g7zlUR_hlniC',1),
('version_Actions','5.14.0',1),
('version_AIAgents','5.14.0',1),
('version_AIProviders','5.14.0',1),
('version_Annotations','5.14.0',1),
('version_API','5.14.0',1),
('version_BotTracking','5.14.0',1),
('version_BulkTracking','5.14.0',1),
('version_Contents','5.14.0',1),
('version_core','5.14.0',1),
('version_CoreAdminHome','5.14.0',1),
('version_CoreConsole','5.14.0',1),
('version_CoreHome','5.14.0',1),
('version_CorePluginsAdmin','5.14.0',1),
('version_CoreUpdater','5.14.0',1),
('version_CoreVisualizations','5.14.0',1),
('version_CoreVue','5.14.0',1),
('version_CustomDimensions','5.14.0',1),
('version_CustomJsTracker','5.14.0',1),
('version_Dashboard','5.14.0',1),
('version_DevicePlugins','5.14.0',1),
('version_DevicesDetection','5.14.0',1),
('version_Diagnostics','5.14.0',1),
('version_Ecommerce','5.14.0',1),
('version_Events','5.14.0',1),
('version_FeatureFlags','5.14.0',1),
('version_Feedback','5.14.0',1),
('version_GeoIp2','5.14.0',1),
('version_Goals','5.14.0',1),
('version_Heartbeat','5.14.0',1),
('version_ImageGraph','5.14.0',1),
('version_Insights','5.14.0',1),
('version_Installation','5.14.0',1),
('version_Intl','5.14.0',1),
('version_IntranetMeasurable','5.14.0',1),
('version_JsTrackerInstallCheck','5.14.0',1),
('version_LanguagesManager','5.14.0',1),
('version_Live','5.14.0',1),
('version_log_conversion.pageviews_before','SMALLINT UNSIGNED DEFAULT NULL',1),
('version_log_conversion.revenue','float default NULL',1),
('version_log_link_visit_action.idaction_content_interaction','INTEGER(10) UNSIGNED DEFAULT NULL',1),
('version_log_link_visit_action.idaction_content_name','INTEGER(10) UNSIGNED DEFAULT NULL',1),
('version_log_link_visit_action.idaction_content_piece','INTEGER(10) UNSIGNED DEFAULT NULL',1),
('version_log_link_visit_action.idaction_content_target','INTEGER(10) UNSIGNED DEFAULT NULL',1),
('version_log_link_visit_action.idaction_event_action','INTEGER(10) UNSIGNED DEFAULT NULL',1),
('version_log_link_visit_action.idaction_event_category','INTEGER(10) UNSIGNED DEFAULT NULL',1),
('version_log_link_visit_action.idaction_name','INTEGER(10) UNSIGNED',1),
('version_log_link_visit_action.idaction_product_cat','INT(10) UNSIGNED NULL',1),
('version_log_link_visit_action.idaction_product_cat2','INT(10) UNSIGNED NULL',1),
('version_log_link_visit_action.idaction_product_cat3','INT(10) UNSIGNED NULL',1),
('version_log_link_visit_action.idaction_product_cat4','INT(10) UNSIGNED NULL',1),
('version_log_link_visit_action.idaction_product_cat5','INT(10) UNSIGNED NULL',1),
('version_log_link_visit_action.idaction_product_name','INT(10) UNSIGNED NULL',1),
('version_log_link_visit_action.idaction_product_sku','INT(10) UNSIGNED NULL',1),
('version_log_link_visit_action.idaction_url','INTEGER(10) UNSIGNED DEFAULT NULL',1),
('version_log_link_visit_action.idpageview','CHAR(6) NULL DEFAULT NULL',1),
('version_log_link_visit_action.product_price','DOUBLE NULL',1),
('version_log_link_visit_action.search_cat','VARCHAR(200) NULL',1),
('version_log_link_visit_action.search_count','INTEGER(10) UNSIGNED NULL',1),
('version_log_link_visit_action.server_time','DATETIME NOT NULL',1),
('version_log_link_visit_action.time_dom_completion','MEDIUMINT(10) UNSIGNED NULL',1),
('version_log_link_visit_action.time_dom_processing','MEDIUMINT(10) UNSIGNED NULL',1),
('version_log_link_visit_action.time_network','MEDIUMINT(10) UNSIGNED NULL',1),
('version_log_link_visit_action.time_on_load','MEDIUMINT(10) UNSIGNED NULL',1),
('version_log_link_visit_action.time_server','MEDIUMINT(10) UNSIGNED NULL',1),
('version_log_link_visit_action.time_spent_ref_action','INTEGER(10) UNSIGNED NULL',1),
('version_log_link_visit_action.time_transfer','MEDIUMINT(10) UNSIGNED NULL',1),
('version_log_visit.ai_agent_name','VARCHAR(40) NULL',1),
('version_log_visit.config_browser_engine','VARCHAR(10) NULL',1),
('version_log_visit.config_browser_name','VARCHAR(40) NULL1',1),
('version_log_visit.config_browser_version','VARCHAR(20) NULL',1),
('version_log_visit.config_client_type','TINYINT( 1 ) NULL DEFAULT NULL1',1),
('version_log_visit.config_cookie','TINYINT(1) NULL',1),
('version_log_visit.config_device_brand','VARCHAR( 100 ) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL1',1),
('version_log_visit.config_device_model','VARCHAR( 100 ) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL1',1),
('version_log_visit.config_device_type','TINYINT( 100 ) NULL DEFAULT NULL1',1),
('version_log_visit.config_flash','TINYINT(1) NULL',1),
('version_log_visit.config_java','TINYINT(1) NULL',1),
('version_log_visit.config_os','CHAR(3) NULL',1),
('version_log_visit.config_os_version','VARCHAR( 100 ) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL',1),
('version_log_visit.config_pdf','TINYINT(1) NULL',1),
('version_log_visit.config_quicktime','TINYINT(1) NULL',1),
('version_log_visit.config_realplayer','TINYINT(1) NULL',1),
('version_log_visit.config_resolution','VARCHAR(18) NULL',1),
('version_log_visit.config_silverlight','TINYINT(1) NULL',1),
('version_log_visit.config_windowsmedia','TINYINT(1) NULL',1),
('version_log_visit.location_browser_lang','VARCHAR(20) NULL',1),
('version_log_visit.location_city','varchar(255) DEFAULT NULL1',1),
('version_log_visit.location_country','CHAR(3) NULL1',1),
('version_log_visit.location_latitude','decimal(9, 6) DEFAULT NULL1',1),
('version_log_visit.location_longitude','decimal(9, 6) DEFAULT NULL1',1),
('version_log_visit.location_region','char(3) DEFAULT NULL1',1),
('version_log_visit.profilable','TINYINT(1) NULL',1),
('version_log_visit.referer_keyword','VARCHAR(255) NULL1',1),
('version_log_visit.referer_name','VARCHAR(255) NULL1',1),
('version_log_visit.referer_type','TINYINT(1) UNSIGNED NULL1',1),
('version_log_visit.referer_url','VARCHAR(1500) NULL',1),
('version_log_visit.user_id','VARCHAR(200) NULL',1),
('version_log_visit.visit_entry_idaction_name','INTEGER(10) UNSIGNED NULL',1),
('version_log_visit.visit_entry_idaction_url','INTEGER(11) UNSIGNED NULL  DEFAULT NULL',1),
('version_log_visit.visit_exit_idaction_name','INTEGER(10) UNSIGNED NULL',1),
('version_log_visit.visit_exit_idaction_url','INTEGER(10) UNSIGNED NULL DEFAULT 0',1),
('version_log_visit.visit_first_action_time','DATETIME NOT NULL',1),
('version_log_visit.visit_goal_buyer','TINYINT(1) NULL',1),
('version_log_visit.visit_goal_converted','TINYINT(1) NULL',1),
('version_log_visit.visit_total_actions','INT(11) UNSIGNED NULL',1),
('version_log_visit.visit_total_events','INT(11) UNSIGNED NULL',1),
('version_log_visit.visit_total_interactions','MEDIUMINT UNSIGNED DEFAULT 0',1),
('version_log_visit.visit_total_searches','SMALLINT(5) UNSIGNED NULL',1),
('version_log_visit.visit_total_time','INT(11) UNSIGNED NOT NULL',1),
('version_log_visit.visitor_count_visits','INT(11) UNSIGNED NOT NULL DEFAULT 01',1),
('version_log_visit.visitor_localtime','TIME NULL',1),
('version_log_visit.visitor_returning','TINYINT(1) NULL1',1),
('version_log_visit.visitor_seconds_since_first','INT(11) UNSIGNED NULL1',1),
('version_log_visit.visitor_seconds_since_last','INT(11) UNSIGNED NULL',1),
('version_log_visit.visitor_seconds_since_order','INT(11) UNSIGNED NULL1',1),
('version_Login','5.14.0',1),
('version_Marketplace','5.14.0',1),
('version_MobileMessaging','5.14.0',1),
('version_Monolog','5.14.0',1),
('version_Morpheus','5.14.0',1),
('version_MultiSites','5.14.0',1),
('version_Overlay','5.14.0',1),
('version_PagePerformance','5.14.0',1),
('version_PrivacyManager','5.14.0',1),
('version_ProfessionalServices','5.14.0',1),
('version_Proxy','5.14.0',1),
('version_Referrers','5.14.0',1),
('version_Resolution','5.14.0',1),
('version_RssWidget','1.0',1),
('version_ScheduledReports','5.14.0',1),
('version_SegmentEditor','5.14.0',1),
('version_SEO','5.14.0',1),
('version_SitesManager','5.14.0',1),
('version_Tour','5.14.0',1),
('version_Transitions','5.14.0',1),
('version_TwoFactorAuth','5.14.0',1),
('version_UserCountry','5.14.0',1),
('version_UserCountryMap','5.14.0',1),
('version_UserId','5.14.0',1),
('version_UserLanguage','5.14.0',1),
('version_UsersManager','5.14.0',1),
('version_VisitFrequency','5.14.0',1),
('version_VisitorInterest','5.14.0',1),
('version_VisitsSummary','5.14.0',1),
('version_VisitTime','5.14.0',1),
('version_WebsiteMeasurable','5.14.0',1),
('version_Widgetize','5.14.0',1);
/*!40000 ALTER TABLE `matomo_option` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_plugin_setting`
--

DROP TABLE IF EXISTS `matomo_plugin_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_plugin_setting` (
  `plugin_name` varchar(60) NOT NULL,
  `setting_name` varchar(255) NOT NULL,
  `setting_value` longtext NOT NULL,
  `json_encoded` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `user_login` varchar(100) NOT NULL DEFAULT '',
  `idplugin_setting` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`idplugin_setting`),
  KEY `plugin_name` (`plugin_name`,`user_login`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_plugin_setting`
--

LOCK TABLES `matomo_plugin_setting` WRITE;
/*!40000 ALTER TABLE `matomo_plugin_setting` DISABLE KEYS */;
INSERT INTO `matomo_plugin_setting` VALUES
('Feedback','nextFeedbackReminder','2027-04-15',0,'admin',1);
/*!40000 ALTER TABLE `matomo_plugin_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_privacy_logdata_anonymizations`
--

DROP TABLE IF EXISTS `matomo_privacy_logdata_anonymizations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_privacy_logdata_anonymizations` (
  `idlogdata_anonymization` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `idsites` text DEFAULT NULL,
  `date_start` datetime NOT NULL,
  `date_end` datetime NOT NULL,
  `anonymize_ip` tinyint(1) unsigned NOT NULL DEFAULT 0,
  `anonymize_location` tinyint(1) unsigned NOT NULL DEFAULT 0,
  `anonymize_userid` tinyint(1) unsigned NOT NULL DEFAULT 0,
  `unset_visit_columns` text NOT NULL DEFAULT '',
  `unset_link_visit_action_columns` text NOT NULL DEFAULT '',
  `output` mediumtext DEFAULT NULL,
  `scheduled_date` datetime DEFAULT NULL,
  `job_start_date` datetime DEFAULT NULL,
  `job_finish_date` datetime DEFAULT NULL,
  `requester` varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY (`idlogdata_anonymization`),
  KEY `job_start_date` (`job_start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_privacy_logdata_anonymizations`
--

LOCK TABLES `matomo_privacy_logdata_anonymizations` WRITE;
/*!40000 ALTER TABLE `matomo_privacy_logdata_anonymizations` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_privacy_logdata_anonymizations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_report`
--

DROP TABLE IF EXISTS `matomo_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_report` (
  `idreport` int(11) NOT NULL AUTO_INCREMENT,
  `idsite` int(11) NOT NULL,
  `login` varchar(100) NOT NULL,
  `description` varchar(255) NOT NULL,
  `idsegment` int(11) DEFAULT NULL,
  `period` varchar(10) NOT NULL,
  `hour` tinyint(4) NOT NULL DEFAULT 0,
  `type` varchar(10) NOT NULL,
  `format` varchar(10) NOT NULL,
  `reports` text NOT NULL,
  `parameters` text DEFAULT NULL,
  `ts_created` timestamp NULL DEFAULT NULL,
  `ts_last_sent` timestamp NULL DEFAULT NULL,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  `evolution_graph_within_period` tinyint(4) NOT NULL DEFAULT 0,
  `evolution_graph_period_n` int(11) NOT NULL,
  `period_param` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`idreport`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_report`
--

LOCK TABLES `matomo_report` WRITE;
/*!40000 ALTER TABLE `matomo_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_report_subscriptions`
--

DROP TABLE IF EXISTS `matomo_report_subscriptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_report_subscriptions` (
  `idreport` int(11) NOT NULL,
  `token` varchar(100) DEFAULT NULL,
  `email` varchar(100) NOT NULL,
  `ts_subscribed` timestamp NULL DEFAULT current_timestamp(),
  `ts_unsubscribed` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`idreport`,`email`),
  UNIQUE KEY `unique_token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_report_subscriptions`
--

LOCK TABLES `matomo_report_subscriptions` WRITE;
/*!40000 ALTER TABLE `matomo_report_subscriptions` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_report_subscriptions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_segment`
--

DROP TABLE IF EXISTS `matomo_segment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_segment` (
  `idsegment` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `definition` text NOT NULL,
  `hash` char(32) DEFAULT NULL,
  `login` varchar(100) NOT NULL,
  `enable_all_users` tinyint(4) NOT NULL DEFAULT 0,
  `enable_only_idsite` int(11) DEFAULT NULL,
  `auto_archive` tinyint(4) NOT NULL DEFAULT 0,
  `ts_created` timestamp NULL DEFAULT NULL,
  `ts_last_edit` timestamp NULL DEFAULT NULL,
  `starred` tinyint(4) NOT NULL DEFAULT 0,
  `starred_by` varchar(100) DEFAULT NULL,
  `deleted` tinyint(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (`idsegment`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_segment`
--

LOCK TABLES `matomo_segment` WRITE;
/*!40000 ALTER TABLE `matomo_segment` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_segment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_sequence`
--

DROP TABLE IF EXISTS `matomo_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_sequence` (
  `name` varchar(120) NOT NULL,
  `value` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_sequence`
--

LOCK TABLES `matomo_sequence` WRITE;
/*!40000 ALTER TABLE `matomo_sequence` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_sequence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_session`
--

DROP TABLE IF EXISTS `matomo_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_session` (
  `id` varchar(191) NOT NULL,
  `modified` int(11) DEFAULT NULL,
  `lifetime` int(11) DEFAULT NULL,
  `data` mediumtext DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_session`
--

LOCK TABLES `matomo_session` WRITE;
/*!40000 ALTER TABLE `matomo_session` DISABLE KEYS */;
INSERT INTO `matomo_session` VALUES
('239db9acd7f4b70f7474ee47e7656997b9f2bf43a8a5840412bb0acabdee9851227d7ee51726f173523d05ab2da514f1d53326a94271f843d6ab1176d9ceb8dc',1792058804,1209600,'a:1:{s:4:\"data\";s:2080:\"YToxMjp7czo0OiJfX1pGIjthOjg6e3M6MTE6IkxvZ2luLmxvZ2luIjthOjE6e3M6NDoiRU5WVCI7YToxOntzOjU6Im5vbmNlIjtpOjE3OTIwNTkyNjI7fX1zOjE1OiJzaXRlV2l0aG91dERhdGEiO2E6MTp7czozOiJFTlQiO2k6MTc5MjA2MjMyMjt9czo1OiJMb2dpbiI7YToxOntzOjQ6IkVOVlQiO2E6Mzp7czoxNDoicmVkaXJlY3RQYXJhbXMiO2k6MTc5MjA2MDU3MjtzOjE2OiJsYXN0UGFzc3dvcmRBdXRoIjtpOjE3OTIwNjA1NzI7czoyMToicGFzc3dvcmRWZXJpZmllZExvZ2luIjtpOjE3OTIwNjA1NzI7fX1zOjE1OiJjb25maXJtUGFzc3dvcmQiO2E6MTp7czo0OiJFTlZUIjthOjE6e3M6NToibm9uY2UiO2k6MTc5MjA1OTM2OTt9fXM6MTc6ImFkZEF1dGhUb2tlbk5vbmNlIjthOjE6e3M6NDoiRU5WVCI7YToxOntzOjU6Im5vbmNlIjtpOjE3OTIwNTkzNzI7fX1zOjE5OiJjaGFuZ2VQYXNzd29yZE5vbmNlIjthOjE6e3M6NDoiRU5WVCI7YToxOntzOjU6Im5vbmNlIjtpOjE3OTIwNTk0MDM7fX1zOjIwOiJkZWxldGVBdXRoVG9rZW5Ob25jZSI7YToxOntzOjQ6IkVOVlQiO2E6MTp7czo1OiJub25jZSI7aToxNzkyMDU5NDAzO319czoyOToiVHdvRmFjdG9yQXV0aC5kaXNhYmxlQXV0aENvZGUiO2E6MTp7czo0OiJFTlZUIjthOjE6e3M6NToibm9uY2UiO2k6MTc5MjA1OTQwMzt9fX1zOjk6InVzZXIubmFtZSI7czo1OiJhZG1pbiI7czoyMjoidHdvZmFjdG9yYXV0aC52ZXJpZmllZCI7aTowO3M6MjA6InVzZXIudG9rZW5fYXV0aF90ZW1wIjtzOjMyOiJkNTk4ZGM5NDRkN2JhYzQ0YzVlMTVhN2Q2ZTEzZjdmZSI7czoxMjoic2Vzc2lvbi5pbmZvIjthOjM6e3M6MjoidHMiO2k6MTc5MjA1ODY2OTtzOjEwOiJyZW1lbWJlcmVkIjtiOjA7czoxMDoiZXhwaXJhdGlvbiI7aToxNzkyMDYyNDA0O31zOjEyOiJub3RpZmljYXRpb24iO2E6MTp7czoxMzoibm90aWZpY2F0aW9ucyI7YTowOnt9fXM6MTU6InNpdGVXaXRob3V0RGF0YSI7YToxOntzOjEzOiJpZ25vcmVNZXNzYWdlIjtiOjE7fXM6NToiTG9naW4iO2E6Mzp7czoxNDoicmVkaXJlY3RQYXJhbXMiO2E6Mjp7czo2OiJtb2R1bGUiO3M6MTI6IlVzZXJzTWFuYWdlciI7czo2OiJhY3Rpb24iO3M6MTE6ImFkZE5ld1Rva2VuIjt9czoxNjoibGFzdFBhc3N3b3JkQXV0aCI7czoxOToiMjAyNi0xMC0xNSAxMDowNjoxMiI7czoyMToicGFzc3dvcmRWZXJpZmllZExvZ2luIjtzOjU6ImFkbWluIjt9czoxNToiY29uZmlybVBhc3N3b3JkIjthOjE6e3M6NToibm9uY2UiO3M6MzI6ImUyY2YxM2RjZjA4YjAzYmRlMTU3ZDI3NDQ3MzVmNTk2Ijt9czoxOToiY2hhbmdlUGFzc3dvcmROb25jZSI7YToxOntzOjU6Im5vbmNlIjtzOjMyOiJjYWUwZTRlNzA5NmJhMjg5YjQ4OTMxMjFmNjQ0NjFjZCI7fXM6MjA6ImRlbGV0ZUF1dGhUb2tlbk5vbmNlIjthOjE6e3M6NToibm9uY2UiO3M6MzI6IjY5NjFiZWFhN2FjNzBlOWM2MDk1ZWIxNDU3ZjZmYmUzIjt9czoyOToiVHdvRmFjdG9yQXV0aC5kaXNhYmxlQXV0aENvZGUiO2E6MTp7czo1OiJub25jZSI7czozMjoiM2UyOTViZDlkNTZhYTc3MDczZjgwOGQzMzNjMzU5ZjgiO319\";}');
/*!40000 ALTER TABLE `matomo_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_site`
--

DROP TABLE IF EXISTS `matomo_site`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_site` (
  `idsite` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(90) NOT NULL,
  `description` varchar(255) NOT NULL DEFAULT '',
  `main_url` varchar(255) NOT NULL,
  `ts_created` timestamp NULL DEFAULT NULL,
  `ecommerce` tinyint(4) DEFAULT 0,
  `sitesearch` tinyint(4) DEFAULT 1,
  `sitesearch_keyword_parameters` text NOT NULL,
  `sitesearch_category_parameters` text NOT NULL,
  `timezone` varchar(50) NOT NULL,
  `currency` char(3) NOT NULL,
  `exclude_unknown_urls` tinyint(1) DEFAULT 0,
  `excluded_ips` text NOT NULL,
  `excluded_parameters` text NOT NULL,
  `excluded_user_agents` text NOT NULL,
  `excluded_referrers` text NOT NULL,
  `group` varchar(250) NOT NULL,
  `type` varchar(255) NOT NULL,
  `keep_url_fragment` tinyint(4) NOT NULL DEFAULT 0,
  `creator_login` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`idsite`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_site`
--

LOCK TABLES `matomo_site` WRITE;
/*!40000 ALTER TABLE `matomo_site` DISABLE KEYS */;
INSERT INTO `matomo_site` VALUES
(1,'emx2','','http://localhost:8080','2026-10-15 10:04:11',0,1,'','','Europe/Amsterdam','USD',0,'','','','','','website',0,'anonymous');
/*!40000 ALTER TABLE `matomo_site` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_site_setting`
--

DROP TABLE IF EXISTS `matomo_site_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_site_setting` (
  `idsite` int(10) unsigned NOT NULL,
  `plugin_name` varchar(60) NOT NULL,
  `setting_name` varchar(255) NOT NULL,
  `setting_value` longtext NOT NULL,
  `json_encoded` tinyint(3) unsigned NOT NULL DEFAULT 0,
  `idsite_setting` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`idsite_setting`),
  KEY `idsite` (`idsite`,`plugin_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_site_setting`
--

LOCK TABLES `matomo_site_setting` WRITE;
/*!40000 ALTER TABLE `matomo_site_setting` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_site_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_site_url`
--

DROP TABLE IF EXISTS `matomo_site_url`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_site_url` (
  `idsite` int(10) unsigned NOT NULL,
  `url` varchar(190) NOT NULL,
  PRIMARY KEY (`idsite`,`url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_site_url`
--

LOCK TABLES `matomo_site_url` WRITE;
/*!40000 ALTER TABLE `matomo_site_url` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_site_url` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_tracking_failure`
--

DROP TABLE IF EXISTS `matomo_tracking_failure`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_tracking_failure` (
  `idsite` bigint(20) unsigned NOT NULL,
  `idfailure` smallint(5) unsigned NOT NULL,
  `date_first_occurred` datetime NOT NULL,
  `request_url` mediumtext NOT NULL,
  PRIMARY KEY (`idsite`,`idfailure`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_tracking_failure`
--

LOCK TABLES `matomo_tracking_failure` WRITE;
/*!40000 ALTER TABLE `matomo_tracking_failure` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_tracking_failure` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_twofactor_recovery_code`
--

DROP TABLE IF EXISTS `matomo_twofactor_recovery_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_twofactor_recovery_code` (
  `idrecoverycode` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `login` varchar(100) NOT NULL,
  `recovery_code` varchar(40) NOT NULL,
  PRIMARY KEY (`idrecoverycode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_twofactor_recovery_code`
--

LOCK TABLES `matomo_twofactor_recovery_code` WRITE;
/*!40000 ALTER TABLE `matomo_twofactor_recovery_code` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_twofactor_recovery_code` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_user`
--

DROP TABLE IF EXISTS `matomo_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_user` (
  `login` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(100) NOT NULL,
  `twofactor_secret` varchar(40) NOT NULL DEFAULT '',
  `superuser_access` tinyint(2) unsigned NOT NULL DEFAULT 0,
  `date_registered` timestamp NULL DEFAULT NULL,
  `ts_password_modified` timestamp NULL DEFAULT NULL,
  `idchange_last_viewed` int(10) unsigned DEFAULT NULL,
  `invited_by` varchar(100) DEFAULT NULL,
  `invite_token` varchar(191) DEFAULT NULL,
  `invite_link_token` varchar(191) DEFAULT NULL,
  `invite_expired_at` timestamp NULL DEFAULT NULL,
  `invite_accept_at` timestamp NULL DEFAULT NULL,
  `ts_changes_shown` timestamp NULL DEFAULT NULL,
  `ts_last_seen` timestamp NULL DEFAULT NULL,
  `ts_inactivity_notified` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`login`),
  UNIQUE KEY `uniq_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_user`
--

LOCK TABLES `matomo_user` WRITE;
/*!40000 ALTER TABLE `matomo_user` DISABLE KEYS */;
INSERT INTO `matomo_user` VALUES
('admin','$2y$12$UXaLsnT5SdXKXKWV2o1WuenDOUegRIusylvR42aFUdW6gxnFgmsoW','test@foo.bar','',1,'2026-10-15 10:01:48','2026-10-15 10:01:48',NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-15 10:04:29','2026-10-15 10:04:29',NULL),
('anonymous','','anonymous@example.org','',0,'2026-10-15 09:59:35','2026-10-15 09:59:35',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `matomo_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_user_dashboard`
--

DROP TABLE IF EXISTS `matomo_user_dashboard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_user_dashboard` (
  `login` varchar(100) NOT NULL,
  `iddashboard` int(11) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `layout` text NOT NULL,
  PRIMARY KEY (`login`,`iddashboard`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_user_dashboard`
--

LOCK TABLES `matomo_user_dashboard` WRITE;
/*!40000 ALTER TABLE `matomo_user_dashboard` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_user_dashboard` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_user_language`
--

DROP TABLE IF EXISTS `matomo_user_language`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_user_language` (
  `login` varchar(100) NOT NULL,
  `language` varchar(10) NOT NULL,
  `use_12_hour_clock` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`login`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_user_language`
--

LOCK TABLES `matomo_user_language` WRITE;
/*!40000 ALTER TABLE `matomo_user_language` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_user_language` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `matomo_user_token_auth`
--

DROP TABLE IF EXISTS `matomo_user_token_auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_user_token_auth` (
  `idusertokenauth` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `login` varchar(100) NOT NULL,
  `description` varchar(100) NOT NULL,
  `password` varchar(191) NOT NULL,
  `hash_algo` varchar(30) NOT NULL,
  `system_token` tinyint(1) NOT NULL DEFAULT 0,
  `last_used` datetime DEFAULT NULL,
  `date_created` datetime NOT NULL,
  `date_expired` datetime DEFAULT NULL,
  `secure_only` tinyint(2) unsigned NOT NULL DEFAULT 0,
  `ts_rotation_notified` datetime DEFAULT NULL,
  `ts_expiration_warning_notified` datetime DEFAULT NULL,
  PRIMARY KEY (`idusertokenauth`),
  UNIQUE KEY `uniq_password` (`password`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_user_token_auth`
--

LOCK TABLES `matomo_user_token_auth` WRITE;
/*!40000 ALTER TABLE `matomo_user_token_auth` DISABLE KEYS */;
INSERT INTO `matomo_user_token_auth` VALUES
(1,'anonymous','anonymous default token','a9d54eeac74f2d10bebea8778fa63368929ff177c94a1e3f4adf55d40060c667d34d6554c28316ff6490f53341530a61c41578232a84bc9d63605923fc3df764','sha512',0,'2026-10-15 09:59:36','2026-10-15 09:59:35',NULL,0,NULL,NULL),
(2,'admin','emx2','85c1e4596056702d9afc4f3025e239e82d97e6d400518b6685c89d9ad38c0f34654778b3d80e09102368d42fc12b3ec723d54780e6f6f17db2cf41af548d5c6b','sha512',0,NULL,'2026-10-15 10:06:23',NULL,1,NULL,NULL);
/*!40000 ALTER TABLE `matomo_user_token_auth` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2026-10-15 10:12:46
