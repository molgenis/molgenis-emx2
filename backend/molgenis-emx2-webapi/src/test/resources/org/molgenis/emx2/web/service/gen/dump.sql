/*
 THIS IS A GENERATED SQL DUMP FROM A MARIADB CONTAINER THAT IS CONNECTED TO A MATOMO CONTAINER.

 INSTRUCTIONS FOR RECREATING THIS CAN BE FOUND IN THE README IN THE PARENT OF THIS DIRECTORY.
 */

/*M!999999\- enable the sandbox mode */
-- MariaDB dump 10.20-12.3.3-MariaDB, for debian-linux-gnu (aarch64)
--
-- Host: localhost    Database: matomo
-- ------------------------------------------------------
-- Server version	12.3.3-MariaDB-ubu2404

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_access` WRITE;
/*!40000 ALTER TABLE `matomo_access` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_access` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_annotations` WRITE;
/*!40000 ALTER TABLE `matomo_annotations` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_annotations` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `matomo_archive_blob_2026_10`
--

DROP TABLE IF EXISTS `matomo_archive_blob_2026_10`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_archive_blob_2026_10` (
  `idarchive` int(10) unsigned NOT NULL,
  `name` varchar(190) NOT NULL,
  `idsite` int(10) unsigned DEFAULT NULL,
  `date1` date DEFAULT NULL,
  `date2` date DEFAULT NULL,
  `period` tinyint(3) unsigned DEFAULT NULL,
  `ts_archived` datetime DEFAULT NULL,
  `value` longblob DEFAULT NULL,
  PRIMARY KEY (`idarchive`,`name`),
  KEY `index_period_archived` (`period`,`ts_archived`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_archive_blob_2026_10`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_archive_blob_2026_10` WRITE;
/*!40000 ALTER TABLE `matomo_archive_blob_2026_10` DISABLE KEYS */;
INSERT INTO `matomo_archive_blob_2026_10` VALUES
(27,'Goals_ItemsCategory',1,'2026-10-12','2026-10-12',1,'2026-10-12 11:52:26','x�K�2���\0O�'),
(27,'Goals_ItemsCategory_Cart',1,'2026-10-12','2026-10-12',1,'2026-10-12 11:52:26','x�K�2���\0O�'),
(27,'Goals_ItemsName',1,'2026-10-12','2026-10-12',1,'2026-10-12 11:52:26','x�K�2���\0O�'),
(27,'Goals_ItemsName_Cart',1,'2026-10-12','2026-10-12',1,'2026-10-12 11:52:26','x�K�2���\0O�'),
(27,'Goals_ItemsSku',1,'2026-10-12','2026-10-12',1,'2026-10-12 11:52:26','x�K�2���\0O�'),
(27,'Goals_ItemsSku_Cart',1,'2026-10-12','2026-10-12',1,'2026-10-12 11:52:26','x�K�2���\0O�'),
(35,'Actions_actions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x���\�n\�0�_\�н-uK\�C\�!/ 06\�b�\�Ry\���\�\���E\rH\Zk~?\�3?%�|p\nJ�\rb@>IHT\�GVG� �hwss�p\�\�\�\�\�\�\�;\�\�Qv���\��\�\���a�:\�\�1(N��\n�\��(\��� 0%�xi*\�V�L�u�s��\�A�\�F�X��\��\�\�l8Tix;I\n�^�B\�Y}_�|%�`�+\��~�C8Vgi�\�\�r�8��\�\r�\�c�=m\�OK@�i\�͗\�\�d\�Wvz<�GPA�B\�c:uMu\�#��O#\�A�0\n�M\�#�.\�\�\Z�=v\�;\�%-\�a\�c\�\\j&y\�j\�\�<^�\������\0�i\�Z�\�;N\� N]w:ڮ�ꎞ\��/��xW�\��k�0\�U\�\�q��c��k�\�i���~h��\rR|\�%P{�\��}R�\�eͪ�gsP�DawQ��O�5VJ\�;Ʉ��\�#h��ُ�xX��rz\�י\�\�`8`\�olݙɔ�β\�U���\�\�\')7\�\�_x��u��	VLb\�\��@�$4,���5�\�\�\Z\�\�L�\�\�:4\��שׂ~\�Ԏ\�\�o��Ğ'),
(35,'Actions_actions_url',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�}���0@\�\���\�\�\n��\�5m]wc��N�\�ʢ&�\�yb�-P�J u<,\n�\Z\� \�۝\�\�@I\'�\�[��Z�/:�\��!�#\'uӂ\�\�`\���q��9$\�<4V���(���G6�(0ϰ\�k�Қ\�\"\�qb�t�2\�q�4�_c!�$j^@��CY\�c����\�\�G�N��\�9o�E6>dX\�d�LYܙ\"r��\��\��9�|\�\�Az�\"\�yd�����V(:Q�GN�\Z��V{a�?g�)\�\�Ė��\�J�%\�\�\�>H�b)_Wݫ\�\�+\�\�T3�)�\��l����Wdcf\�y�.�\�\��N\�\rI\�;��\Z�^��������Y\�iB\�\� ��\�\�r�m\�ǚ\�i�]dl�f�M\�\rWxXPwV�\�\�wB7g\�7ƶں\�Br�\�\�m�\�mT\�BIJ���,\�Z\�n\nEt�'),
(35,'Actions_actions_url_chunk_0_99',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x������ �߅H�\�\��Wp\�ĮNURa�\�\�\�\�b�E׶��\��\�\�9Gp\�?ZN\n\�)Ŕ#����\�\�\��\�u\�\�h	\�\�|m\���\��-��\�ߩ�̅�\�\��f��B\'(�d*hQ���K8Ro}�۾��Z�\�\�;2��p�\Z4X{x��V�\�\�\�B8-x\�ҷ\�bRq�F\�AV$~m�h~\���ǟ\�x\�a\�\0\'\n\�X���=\Z;\�`�[\��,5Yzפ�Š�Y\Z�cC��hJ˓z<�\�\�\��Q@E&e\�U\�\�m��Z�vx�\�2P1\Z[\�g<\�\�\�-5ny\�w�[Z\�¨\�ߺZ�r0�1>�y�\�?0�\�\0���X��a���\�Qr\�Q\�N���C\�Iq�\�:\�4�\�xOFp�{FX�\��v�lk���)q��tv3����-2[$S\�i.\rl5Go\�#\�\��\�\�\�N^D\�H�y�3�\�?e��ɋ�\�N<e�N���5?�U6�h\"\�?GӑX�T.\��\�\�F�\��=�\�*o\�ݐ90x;_l\�@���\�/b�\"eȍ!�N^\�=GEȗ���\�H8�\�\�VЛY�\�ٞL\��m{�m{g_k�O(�\0\�6ӿ'),
(35,'Actions_downloads',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Actions_outlink',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Actions_sitesearch',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Actions_SiteSearchCategories',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'BotTracking_AIChatbotsAIFavouredPages',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45','x�K�2���\0O�'),
(35,'BotTracking_AIChatbotsBrokenContent',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45','x�K�2���\0O�'),
(35,'BotTracking_AIChatbotsDocuments',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45','x�K�2���\0O�'),
(35,'BotTracking_AIChatbotsHumanFavouredPages',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45','x�K�2���\0O�'),
(35,'BotTracking_AIChatbotsPages',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45','x�K�2���\0O�'),
(35,'BotTracking_AIChatbotsRequestedDocuments',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45','x�K�2���\0O�'),
(35,'BotTracking_AIChatbotsRequestedPages',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45','x�K�2���\0O�'),
(35,'Contents_name_piece',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Contents_piece_name',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'DevicePlugins_plugin',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�}\�A�0л\��\"\�\��C�\"�\"H\�a���`�Y$\�\'�7I\',\�v)��(\�\�r9�\�F<9A�U\�\�oc,[�\��#[�\�z7\�O#U�t\�uª�\�e���\�\�|Iь\�z}��\�$4x}\�t)ץ%L~r�G�:\�T\n�\�Qvsl})\�\�\\�n'),
(35,'DevicesDetection_brands',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x���K\n� D\��\���^�\�\n�ܹ\�\�X*�B7]<2/@P\"��}q(#(��H\�3�|�(�Xex0�����o\r\�=�\��\\Im��}����\�j-\�[\�\�\�]/N~\�\�	d\�F�'),
(35,'DevicesDetection_browserEngines',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x��NA\n�0�\�^�N�,{�7�Pe��\��\�\�\�\�d�xH���a�\"�gt�8�j\�%\�ʟ\�P\�G	:A��-:9��/c\�D\��׷�\�\�\�r���\�-|\n\�v�\�Sk`��\� _l\�G\�'),
(35,'DevicesDetection_browsers',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2��δ2�N�2�1,����L��r�Rs����J�\�ܔ�3���\���A�	�0�1LA�� y3k�)�V\�P\�\��\�\�\0,R\�%ZXUׂ\�	\�\�\�J)\�\�\�~$l\n�ǒ,\�k0�G�'),
(35,'DevicesDetection_browserVersions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x��N1\n\�0��p$�\�6\�\�[�B�@��!��$Pn�A�lcIr�����|��θ\'Ʌ�X7�I\�Pa+F\�\\w\�\�\�\�>�j%̭[۞\�ܛ�S�A.*�\�\�\�\�ַ|\�\�m\���/\�H�'),
(35,'DevicesDetection_models',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2��δ2�N�2�1,����L��r�Rs���������3���\����\�	��\�\r-���5ȈL+s(mh\�bb\0�\����kA\���\�\�ma�\�`����Z�����Z�]�_�\�\ZC�Sa\�1�4\"\�=�\01<M;'),
(35,'DevicesDetection_os',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2��δ2�N�2�1,����L��r�Rs����2J��\�J֙VF@l\�\� \�H��� YK���5ȘL+s(mh\�db\0�\����kA��\�\�\�\�n�	6\�cI�\�\0xlG\�'),
(35,'DevicesDetection_osVersions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2��δ2�N�2�1,����L��r�Rs����J��\�ֆz��J֙VF@l\�\� \�H��� YK���5ȰL+s(mh\�db\0�\����kA��p�\�\�J)\�\�\�\Z�\�\�`��M�vY�\�Z\0�J+'),
(35,'DevicesDetection_types',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x����\r� Dwa\�\'\�فH)�\�(�\�$X�I�\�9\�\�%��,�\�C�t^E\�\n\r�b�\�\�r\�?z\n�nq2\�9�\�\�O\�\�/1�\��9��R��\�K\"%1�`~��\r>E{'),
(35,'Events_action_category',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Events_action_name',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Events_category_action',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Events_category_name',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Events_name_action',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Events_name_category',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Goals_ItemsCategory',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Goals_ItemsCategory_Cart',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Goals_ItemsName',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Goals_ItemsName_Cart',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Goals_ItemsSku',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Goals_ItemsSku_Cart',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Referrers_entryTitleByAIAssistant',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Referrers_entryUrlByAIAssistant',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Referrers_keywordByCampaign',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Referrers_keywordBySearchEngine',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Referrers_searchEngineByKeyword',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Referrers_type',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�5�\�\r� CwaR\�\�١;�RH\�zDٽ���;J�A\�\n�\Z\�\'ڋ�\��Ԡҵ%\��[f��,M	(\�ǔ��\�q�\�	�\��\�\�#�'),
(35,'Referrers_urlBySocialNetwork',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Referrers_urlByWebsite',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2���\0O�'),
(35,'Resolution_configuration',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2��δ2�N�2�1,����L��r�Rs����ͬ�|��\�ܬ�-L+\�,��3���\���A�	�0�1LA�� y3k���V\�P\�\��\�\�\0,R\�%ZXUׂ\�	\�q\r�\�\�m\r¥y\�y�\�y\�1A¦`K-\�rJ-\0X\�O�'),
(35,'Resolution_resolution',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x��NA\n\�0��_7N\�(\�R\�!���26��\�\��첃,[¶\"�\"lo<\�\�\�r\�(M ;�x43�������\"�q\�z\�Ǡ\�����%1�Ru�0(Uݺ\�;\�z\�5\�\���A>\�\�_��\�\ri/K:'),
(35,'UserCountry_city',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�5�\�\r� Cw\��@?\�݁J= q㈲{D~q,\�),\�7FG�򼍤�A$\�R4�Ӑ\�8�/!w\�c\��Q♨E\��1ԋnQ�\0J$1'),
(35,'UserCountry_country',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�K�2��δ2�N�2�1,����L��r�Rs����J�J���3���\���A�	�0�1LA�� y3k�)�V\�P\�\��\�\�\0,R\�%ZXUׂ\�	ൾ�\�z$l\n�ƒ,\�kPrG\�'),
(35,'UserCountry_region',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x�5�\�\r� Cw\��@?\�݁J= q㈲{D~q,\�),\�7FG�򼍤�A$\�R4�Ӑ\�8�/!w\�c\��Q♨E\��1ԋnQ�\0J$1'),
(35,'UserId_users',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45','x�K�2���\0O�'),
(35,'UserLanguage_language',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x��NA\n� ��\��U+�\�<\�EA�{k$DХ\�\�\��3\�P\"�\�НX��b\�k؄K�\"r.B1��恱\�d�N�yD\�w\'�O���\�&�R�\�҅\�p\�a�\�ΧN�eկ\�	TCH\�'),
(35,'VisitorInterest_daysSinceLastVisit',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x��\�=1\��Rp4M?4���\�\�*:+8\�\�\r\�m���BJxS���T�\�x�\�-\�\�7\r\�\�\�\�\�Y�\�\�\�\�>�k��s���3\�3ϫ$�\��\�?H\�<�]\�?�`	�d	�.DK  ]H�!\�B��I�%dȺp��EN�\��A�^Rɘ��V�^LɄ�-D\�f��J9��\����À@\�\Z�^R�\�tgM\�>ڕ$�'),
(35,'VisitorInterest_pageGap',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x��\�1\�0л uD2\�$)l9@\�\�J,e\�\��\n�/�Ow��\�\�b]\��ש�p\�\�\�\0;��6�\�7r%�󗞪�f!98*\�X��аM��\n-%\�A2a�5�Ph\nR��5$<��gr�-59\�\�L\�#\��\��G'),
(35,'VisitorInterest_timeGap',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x��\�1\n\�0л:\n$\�q�\�\�\\\�Ȗ1\�\�U�tj�\�c\�Ǘ:Tp��֑�`8vL��?_�$�Bm��\�̹B}пzJ\�q=�\'<�r\n���\\#\r�*��}��\�!2�2&l9�2E���.%R\\\�]\�Hq��H�C��\\`���筢7���\�\r�D\�`'),
(35,'VisitorInterest_visitsByVisitCount',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x��\�=\n\�0\�:\n$�/���wp�C [Ɛ�W����\�͇\�4)vn\�cZ��\�yX��\�\�gO\�\�$$il��qm&��\�\��\�란��\��42e,\�H(T�P\"�R\�B��F\r-:u,\�HXh�\��bi%	�r��g���c\n\�gmT9@p5����#׳�}XHCw\��,}\"\���\�d'),
(35,'VisitTime_localTime',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x��\�A\n�0�\�\��1Q3ޡw�Ѕ\�K\�\�U\�@7>�\�2B~��\�\�|��\'o\���\�\�ez��\�x,\�nu�u�:s�]�}�@llΟm_\�\�n|ݎC��p]��j�j�jK����h5���V��\�\�j\'�\��ڋ���j�\�\"��\��N7<�O�\�\�\�\�sC�*\�̉�\�(p�Jō\�E\�ي�{gNTV�c�V\�\\A\�8XP�NTf���Z\�\�2�Z\�\�2٫\�\�Y�2˸Y�yYm_\nD�'),
(35,'VisitTime_serverTime',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44','x��\�;�0�\�pfm\�X\�;���.%\�\���j-\�\�\�}\�\�-��z;͞\�a\��\�śm^\�[3]��[\�3\�,1��}L�>l\�\�q^���\�\'�z\�\nQ\�h\�D\�D�ITʹ�E\�B�ET\�h�U{Z\�EՁVQu�\�QT\�	2�xR�TB��j�EC����\�\�϶=�9TPINTV�c�V\�\\A\�8XP�NTf7\�Tf7\�d;\�\�R�2˸Y�٫�/[�\r'),
(36,'Actions_actions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x���\�n\�0�_\�н-uK\�C\�!/ 06\�b�\�Ry\���\�\���E\rH\Zk~?\�3?%�|p\nJ�\rb@>IHT\�GVG� �hwss�p\�\�\�\�\�\�\�;\�\�Qv���\��\�\���a�:\�\�1(N��\n�\��(\��� 0%�xi*\�V�L�u�s��\�A�\�F�X��\��\�\�l8Tix;I\n�^�B\�Y}_�|%�`�+\��~�C8Vgi�\�\�r�8��\�\r�\�c�=m\�OK@�i\�͗\�\�d\�Wvz<�GPA�B\�c:uMu\�#��O#\�A�0\n�M\�#�.\�\�\Z�=v\�;\�%-\�a\�c\�\\j&y\�j\�\�<^�\������\0�i\�Z�\�;N\� N]w:ڮ�ꎞ\��/��xW�\��k�0\�U\�\�q��c��k�\�i���~h��\rR|\�%P{�\��}R�\�eͪ�gsP�DawQ��O�5VJ\�;Ʉ��\�#h��ُ�xX��rz\�י\�\�`8`\�olݙɔ�β\�U���\�\�\')7\�\�_x��u��	VLb\�\��@�$4,���5�\�\�\Z\�\�L�\�\�:4\��שׂ~\�Ԏ\�\�o��Ğ'),
(36,'Actions_actions_url',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�}���0@\�\���\�\�\n��\�5m]wc��N�\�ʢ&�\�yb�-P�J u<,\n�\Z\� \�۝\�\�@I\'�\�[��Z�/:�\��!�#\'uӂ\�\�`\���q��9$\�<4V���(���G6�(0ϰ\�k�Қ\�\"\�qb�t�2\�q�4�_c!�$j^@��CY\�c����\�\�G�N��\�9o�E6>dX\�d�LYܙ\"r��\��\��9�|\�\�Az�\"\�yd�����V(:Q�GN�\Z��V{a�?g�)\�\�Ė��\�J�%\�\�\�>H�b)_Wݫ\�\�+\�\�T3�)�\��l����Wdcf\�y�.�\�\��N\�\rI\�;��\Z�^��������Y\�iB\�\� ��\�\�r�m\�ǚ\�i�]dl�f�M\�\rWxXPwV�\�\�wB7g\�7ƶں\�Br�\�\�m�\�mT\�BIJ���,\�Z\�n\nEt�'),
(36,'Actions_actions_url_chunk_0_99',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x������ �߅H�\�\��Wp\�ĮNURa�\�\�\�\�b�E׶��\��\�\�9Gp\�?ZN\n\�)Ŕ#����\�\�\��\�u\�\�h	\�\�|m\���\��-��\�ߩ�̅�\�\��f��B\'(�d*hQ���K8Ro}�۾��Z�\�\�;2��p�\Z4X{x��V�\�\�\�B8-x\�ҷ\�bRq�F\�AV$~m�h~\���ǟ\�x\�a\�\0\'\n\�X���=\Z;\�`�[\��,5Yzפ�Š�Y\Z�cC��hJ˓z<�\�\�\��Q@E&e\�U\�\�m��Z�vx�\�2P1\Z[\�g<\�\�\�-5ny\�w�[Z\�¨\�ߺZ�r0�1>�y�\�?0�\�\0���X��a���\�Qr\�Q\�N���C\�Iq�\�:\�4�\�xOFp�{FX�\��v�lk���)q��tv3����-2[$S\�i.\rl5Go\�#\�\��\�\�\�N^D\�H�y�3�\�?e��ɋ�\�N<e�N���5?�U6�h\"\�?GӑX�T.\��\�\�F�\��=�\�*o\�ݐ90x;_l\�@���\�/b�\"eȍ!�N^\�=GEȗ���\�H8�\�\�VЛY�\�ٞL\��m{�m{g_k�O(�\0\�6ӿ'),
(36,'Actions_downloads',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Actions_outlink',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Actions_sitesearch',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Actions_SiteSearchCategories',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'BotTracking_AIChatbotsAIFavouredPages',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'BotTracking_AIChatbotsBrokenContent',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'BotTracking_AIChatbotsDocuments',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'BotTracking_AIChatbotsHumanFavouredPages',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'BotTracking_AIChatbotsPages',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'BotTracking_AIChatbotsRequestedDocuments',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'BotTracking_AIChatbotsRequestedPages',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Contents_name_piece',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Contents_piece_name',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'DevicePlugins_plugin',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�}\�A�0л\��\"\�\��C�\"�\"H\�a���`�Y$\�\'�7I\',\�v)��(\�\�r9�\�F<9A�U\�\�oc,[�\��#[�\�z7\�O#U�t\�uª�\�e���\�\�|Iь\�z}��\�$4x}\�t)ץ%L~r�G�:\�T\n�\�Qvsl})\�\�\\�n'),
(36,'DevicesDetection_brands',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x���K\n� D\��\���^�\�\n�ܹ\�\�X*�B7]<2/@P\"��}q(#(��H\�3�|�(�Xex0�����o\r\�=�\��\\Im��}����\�j-\�[\�\�\�]/N~\�\�	d\�F�'),
(36,'DevicesDetection_browserEngines',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x��NA\n�0�\�^�N�,{�7�Pe��\��\�\�\�\�d�xH���a�\"�gt�8�j\�%\�ʟ\�P\�G	:A��-:9��/c\�D\��׷�\�\�\�r���\�-|\n\�v�\�Sk`��\� _l\�G\�'),
(36,'DevicesDetection_browsers',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2��δ2�N�2�1,����L��r�Rs����J�\�ܔ�3���\���A�	�0�1LA�� y3k�)�V\�P\�\��\�\�\0,R\�%ZXUׂ\�	\�\�\�J)\�\�\�~$l\n�ǒ,\�k0�G�'),
(36,'DevicesDetection_browserVersions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x��N1\n\�0��p$�\�6\�\�[�B�@��!��$Pn�A�lcIr�����|��θ\'Ʌ�X7�I\�Pa+F\�\\w\�\�\�\�>�j%̭[۞\�ܛ�S�A.*�\�\�\�\�ַ|\�\�m\���/\�H�'),
(36,'DevicesDetection_models',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2��δ2�N�2�1,����L��r�Rs���������3���\����\�	��\�\r-���5ȈL+s(mh\�bb\0�\����kA\���\�\�ma�\�`����Z�����Z�]�_�\�\ZC�Sa\�1�4\"\�=�\01<M;'),
(36,'DevicesDetection_os',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2��δ2�N�2�1,����L��r�Rs����2J��\�J֙VF@l\�\� \�H��� YK���5ȘL+s(mh\�db\0�\����kA��\�\�\�\�n�	6\�cI�\�\0xlG\�'),
(36,'DevicesDetection_osVersions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2��δ2�N�2�1,����L��r�Rs����J��\�ֆz��J֙VF@l\�\� \�H��� YK���5ȰL+s(mh\�db\0�\����kA��p�\�\�J)\�\�\�\Z�\�\�`��M�vY�\�Z\0�J+'),
(36,'DevicesDetection_types',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x����\r� Dwa\�\'\�فH)�\�(�\�$X�I�\�9\�\�%��,�\�C�t^E\�\n\r�b�\�\�r\�?z\n�nq2\�9�\�\�O\�\�/1�\��9��R��\�K\"%1�`~��\r>E{'),
(36,'Events_action_category',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Events_action_name',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Events_category_action',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Events_category_name',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Events_name_action',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Events_name_category',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Goals_ItemsCategory',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Goals_ItemsCategory_Cart',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Goals_ItemsName',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Goals_ItemsName_Cart',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Goals_ItemsSku',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Goals_ItemsSku_Cart',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Referrers_entryTitleByAIAssistant',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Referrers_entryUrlByAIAssistant',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Referrers_keywordByCampaign',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Referrers_keywordBySearchEngine',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Referrers_searchEngineByKeyword',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Referrers_type',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�5�\�\r� CwaR\�\�١;�RH\�zDٽ���;J�A\�\n�\Z\�\'ڋ�\��Ԡҵ%\��[f��,M	(\�ǔ��\�q�\�	�\��\�\�#�'),
(36,'Referrers_urlBySocialNetwork',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Referrers_urlByWebsite',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'Resolution_configuration',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2��δ2�N�2�1,����L��r�Rs����ͬ�|��\�ܬ�-L+\�,��3���\���A�	�0�1LA�� y3k���V\�P\�\��\�\�\0,R\�%ZXUׂ\�	\�q\r�\�\�m\r¥y\�y�\�y\�1A¦`K-\�rJ-\0X\�O�'),
(36,'Resolution_resolution',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x��NA\n\�0��_7N\�(\�R\�!���26��\�\��첃,[¶\"�\"lo<\�\�\�r\�(M ;�x43�������\"�q\�z\�Ǡ\�����%1�Ru�0(Uݺ\�;\�z\�5\�\���A>\�\�_��\�\ri/K:'),
(36,'UserCountry_city',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�5�\�\r� Cw\��@?\�݁J= q㈲{D~q,\�),\�7FG�򼍤�A$\�R4�Ӑ\�8�/!w\�c\��Q♨E\��1ԋnQ�\0J$1'),
(36,'UserCountry_country',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2��δ2�N�2�1,����L��r�Rs����J�J���3���\���A�	�0�1LA�� y3k�)�V\�P\�\��\�\�\0,R\�%ZXUׂ\�	ൾ�\�z$l\n�ƒ,\�kPrG\�'),
(36,'UserCountry_region',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�5�\�\r� Cw\��@?\�݁J= q㈲{D~q,\�),\�7FG�򼍤�A$\�R4�Ӑ\�8�/!w\�c\��Q♨E\��1ԋnQ�\0J$1'),
(36,'UserId_users',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x�K�2���\0O�'),
(36,'UserLanguage_language',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x��NA\n� ��\��U+�\�<\�EA�{k$DХ\�\�\��3\�P\"�\�НX��b\�k؄K�\"r.B1��恱\�d�N�yD\�w\'�O���\�&�R�\�҅\�p\�a�\�ΧN�eկ\�	TCH\�'),
(36,'VisitorInterest_daysSinceLastVisit',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x��\�=1\��Rp4M?4���\�\�*:+8\�\�\r\�m���BJxS���T�\�x�\�-\�\�7\r\�\�\�\�\�Y�\�\�\�\�>�k��s���3\�3ϫ$�\��\�?H\�<�]\�?�`	�d	�.DK  ]H�!\�B��I�%dȺp��EN�\��A�^Rɘ��V�^LɄ�-D\�f��J9��\����À@\�\Z�^R�\�tgM\�>ڕ$�'),
(36,'VisitorInterest_pageGap',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x��\�1\�0л uD2\�$)l9@\�\�J,e\�\��\n�/�Ow��\�\�b]\��ש�p\�\�\�\0;��6�\�7r%�󗞪�f!98*\�X��аM��\n-%\�A2a�5�Ph\nR��5$<��gr�-59\�\�L\�#\��\��G'),
(36,'VisitorInterest_timeGap',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x��\�1\n\�0л:\n$\�q�\�\�\\\�Ȗ1\�\�U�tj�\�c\�Ǘ:Tp��֑�`8vL��?_�$�Bm��\�̹B}пzJ\�q=�\'<�r\n���\\#\r�*��}��\�!2�2&l9�2E���.%R\\\�]\�Hq��H�C��\\`���筢7���\�\r�D\�`'),
(36,'VisitorInterest_visitsByVisitCount',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x��\�=\n\�0\�:\n$�/���wp�C [Ɛ�W����\�͇\�4)vn\�cZ��\�yX��\�\�gO\�\�$$il��qm&��\�\��\�란��\��42e,\�H(T�P\"�R\�B��F\r-:u,\�HXh�\��bi%	�r��g���c\n\�gmT9@p5����#׳�}XHCw\��,}\"\���\�d'),
(36,'VisitTime_localTime',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x��\�A\n�0�\�\��1Q3ޡw�Ѕ\�K\�\�U\�@7>�\�2B~��\�\�|��\'o\���\�\�ez��\�x,\�nu�u�:s�]�}�@llΟm_\�\�n|ݎC��p]��j�j�jK����h5���V��\�\�j\'�\��ڋ���j�\�\"��\��N7<�O�\�\�\�\�sC�*\�̉�\�(p�Jō\�E\�ي�{gNTV�c�V\�\\A\�8XP�NTf���Z\�\�2�Z\�\�2٫\�\�Y�2˸Y�yYm_\nD�'),
(36,'VisitTime_serverTime',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42','x��\�;�0�\�pfm\�X\�;���.%\�\���j-\�\�\�}\�\�-��z;͞\�a\��\�śm^\�[3]��[\�3\�,1��}L�>l\�\�q^���\�\'�z\�\nQ\�h\�D\�D�ITʹ�E\�B�ET\�h�U{Z\�EՁVQu�\�QT\�	2�xR�TB��j�EC����\�\�϶=�9TPINTV�c�V\�\\A\�8XP�NTf7\�Tf7\�d;\�\�R�2˸Y�٫�/[�\r'),
(37,'Actions_actions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���ˎ�0@%b\�\��\�M\�\�,�\��\0bwb�Gn�U4�\�knL0��&\�\�\��}\n�\�Y	��!\�ND\"(�YY\'�\�\�\�\�\�ӗ\�\�O_7\�T+\��i��.\�^}Gh��NM��\�\0\�\�B>HG\�Tβ��q�Ao,�\�w�kUɼ���i�\�\�M�$IH�\\\�K~T�\�ߔ>N��h\\\�U��I?*�\��\�EX\'\�?�\�N�\�\n}\\\�\�+V\�`\�$#�aA���y�/�&�n���9\�Y�؇\�_�̓�`YD\�DQ[�;p��\�Om��]�\�Wg!6#g>\�\�$5\�t.9\���s����\�Mu*�VMm�a\�\�&i�B=�e���zg7S��\'$a�%a�[��:/�\�༇t\�\�8\�\�9:6\�t�c3\rb�\�\�`�Ԟy\�\�\\�%��`�`��\�;X��+\��獵�\�ٞ�S\�\n\�P\�R\�B���]��i.9|\�-;=�7���b�\�{]��\�=\'�\�\��,hV4�\�}�LԵ�\�W\�N_!�_WC)��v\���|LLѢ��\�2\�\�y�N7�4K\�y\�nVD����h\�\�\�\�vF���P(M?K(p$���y\\\�'),
(37,'Actions_actions_url',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�}���0@\�\���\�\�\n��\�5m]wc��N�\�ʢ&�\�yb�-P�J u<,\n�\Z\� \�۝\�\�@I\'�\�[��Z�/:�\��!�#\'uӂ\�\�`\���q��9$\�<4V���(���G6�(0ϰ\�k�Қ\�\"\�qb�t�2\�q�4�_c!�$j^@��CY\�c����\�\�G�N��\�9o�E6>dX\�d�LYܙ\"r��\��\��9�|\�\�Az�\"\�yd�����V(:Q�GN�\Z��V{a�?g�)\�\�Ė��\�J�%\�\�\�>H�b)_Wݫ\�\�+\�\�T3�)�\��l����Wdcf\�y�.�\�\��N\�\rI\�;��\Z�^��������Y\�iB\�\� ��\�\�r�m\�ǚ\�i�]dl�f�M\�\rWxXPwV�\�\�wB7g\�7ƶں\�Br�\�\�m�\�mT\�BIJ���,\�Z\�n\nEt�'),
(37,'Actions_actions_url_chunk_0_99',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���r�0�\�\�l=x�;�\n�b\��#���L\�2�0\�i3\��_�X���\�4\�\�)�\�\� �|���\�E$�F��M\0YJDp�\�]y֪�\�Z�\�Tq>Í\��\�f:3cN\�T�\������d�8|�\�X�۵\�\��,\�R��\�i	�Ɂ$	10\�KQպ/\�k]-\�L4\�\�u�R�0$���XfC�֗\�\�\��\�r�>�)<aE\�C1B6D�\�Lj_M(\�ɶ�1�Y�؇\�\�\'�\�\�$\�\�<)j\�rŭS\�\�\�\�\�y[�3\�r��\��V�pAܵ,\�~賺ޚRת5\�\�\�9$Y�C=_e���z���)��\��0>�0\�-�ت-\Z%/\�{\�6���F\�ٱY�\��e�\r��&�2{\�\�2��02:��KC\�\��:\�|\"\\�e\�H��a\�[7H)�\�J\�8u�M�z-R\�`��\\gAc�{�\r٥\�\�Nު_ͪ\�Pt���Et\�W�+:[{\�s�\�_i>�a4\�1\Z\�\�	ml��\�\�	u�\'Յ~s��<�:=\�(\�w�\Z�656_\���Σ�}���U:7\�{\�8gk;i\�\��\�vf6\���[(\���-<\�w:b4\�\\~9�\�\�'),
(37,'Actions_downloads',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Actions_outlink',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Actions_sitesearch',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Actions_SiteSearchCategories',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'BotTracking_AIChatbotsAIFavouredPages',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'BotTracking_AIChatbotsBrokenContent',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'BotTracking_AIChatbotsDocuments',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'BotTracking_AIChatbotsHumanFavouredPages',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'BotTracking_AIChatbotsPages',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'BotTracking_AIChatbotsRequestedDocuments',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'BotTracking_AIChatbotsRequestedPages',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Contents_name_piece',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Contents_piece_name',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'DevicePlugins_plugin',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x��\�Q�0\�\�cC\�w\��T� C�!\�ݽ���\�۟��\�\�\���!��ڃ\�\�q\09s���G*��eK�!C!e�7F\�֐��\��Pȵ3��r\�oGF�t7�I3*\�(\�P\�\�\�r=_\\�w6�\�!je4Z\�g\�vLkE\\��\�lGǷfJs\�8\'A?�~\�8��\�\r.�6'),
(37,'DevicesDetection_brands',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���K\n�0D\�4�:]y\�\n\\�sYzw�\"��C_2�\0A�0>�^�!\�\�A)�[\"�À\�Gpi���A�2\�Gtm�\n-�\�)\�U�y�sўP\�sѶ���6д|�T\�o~؈�?6\�\0\�L'),
(37,'DevicesDetection_browserEngines',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���A\n�0E\�4�:\�\�\0\�CgpQFp\�\�\�&X�q\�\"䓟��A\�p1��\"�l\�AKz\�\�\rD1�[I\�^-u\��\�8�kի\�A\��\�\�Xu�_?]-\'i|��6��{\�\����w8��<�$A�\0\��Ml'),
(37,'DevicesDetection_browsers',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���;\n� D\�\'p5�4�t9���]J\�\�\�%!E\nu\��Ë�(	&D�+x�\r-(\�e\�6�a���\�\�\�ȍB\��Oh�\�\�\nv \�\�Nn2��;Wm�0(U��\�\�\�F\Z\�i�ґַ\�C�\����;�#L�'),
(37,'DevicesDetection_browserVersions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���1\n\�0E\�K���k\�R\�\��\���-�\�\�k�$B����\�\�ej�\����� �\���̤�qT�\�͑\�:�n߿���>�Q\n��v\�^{�$��\�o���=7#e8\�f�i{8w\n�\�\�\�3��=\��\�0�+6\��\�Nu'),
(37,'DevicesDetection_models',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���Q\n1D�\"9A\�|y\�\nU�\�_\�\�ݰ]D���!�L^�EI0\�f\�Q2z\�\�q \�`��$p+ۺ��N=\�E��V�h:�W{9�\Zի���`P��m\���5h��S�\�{:,�1�\�\�78;���\�7<\�\�?x\�	;�R�'),
(37,'DevicesDetection_os',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2��δ2�N�2�1,����L��r�Rs����2J��\�J֙V�Pl����FVJ�& IcKk��V& Ya\n\�ldi�7\���@ls8�dR���Uu-\�<?�\0^\��y\�r\�TL\� 9\�\�В\�\�\0MC'),
(37,'DevicesDetection_osVersions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���1\n\�0E\�K��5���\�r\0:�e4�{%HB�d\� \�K_|^��U-��\�h+h)�\�B�ځ���r�p\"�\�d�\�~ă�1�\�T\�zKfG�dq�j��\�\�\�ݓ\nZ\��i?�C��yz�\��%�}1\���ӹO�'),
(37,'DevicesDetection_types',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���K\n�0D\�4�:��w�\�\�\�e\�\�mh+��p2d�\��\�l\��\�ú\�\��n%�\�b1hP\�:�\�C���\�\�Ԟ\�Q=_:kN(\�)k\�\�_$\�\���OO����? �s�J\�'),
(37,'Goals_ItemsCategory',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Goals_ItemsCategory_Cart',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Goals_ItemsName',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Goals_ItemsName_Cart',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Goals_ItemsSku',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Goals_ItemsSku_Cart',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Referrers_entryTitleByAIAssistant',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Referrers_entryUrlByAIAssistant',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Referrers_keywordByCampaign',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Referrers_keywordBySearchEngine',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Referrers_searchEngineByKeyword',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Referrers_type',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2��δ2�N�2�1,����L��r�Rs��3��\�	�\�,@Kk��L+��0qA\Z̠R\�`�dJ���Uu-H��um-\0\�\�#�'),
(37,'Referrers_urlBySocialNetwork',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Referrers_urlByWebsite',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'Resolution_configuration',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x����\n\�0D�e� ����=�Ћ؛�C�\�C�\�7��\�a�If�}	S�����iD\Z\�\�<��\�\0:lw\�u\�Z\��%\�\�Ev�]�� \�\Z�(Z�\�5գ��\�h5Z2\�7/?kS��4k_�~����c�խ\\\�\�^��\��hod\�\���	�U7'),
(37,'Resolution_resolution',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���K\n\�0D\�X�\�\����]�wJ\�w�I)�,��4҈\�ex��2\�m\�\0Z\�r])mz ��xt3�\�\�\�S\�ǃ8�)1YLA0\�ڠ��|\�揺1ș�>�[R�C\�w9\�P\�Qo\���gH��K\�\��\�oQ�P�'),
(37,'UserCountry_city',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�M�\�\r�0Cw\�-\�Wgv�J�\�Xewj\'��gCD/jH�\�\�\r�\�yUц\0-�C\�O�\�N��d�̄c\�	�\�k{��\�Faw\�\�+g$'),
(37,'UserCountry_country',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���A\n� E\�2\'p\�\n�w\�-�v\�\�s\�\"��\�\�7~^�F�P>���!\'t�%L\�B>\�Z�nK�\���@\����Be\�*6 \�\�\��$W.\����\�\�çͶ�\�\�ҧ\�ͅ\��q);\��MZ'),
(37,'UserCountry_region',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�M�\�\r�0Cw\�-\�Wgv�J�\�Xewj\'��gCD/jH�\�\�\r�\�yUц\0-�C\�O�\�N��d�̄c\�	�\�k{��\�Faw\�\�+g$'),
(37,'UserId_users',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x�K�2���\0O�'),
(37,'UserLanguage_language',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���A\n\�0��{�����\�?���!\�b\n\�W�NrZ��e\",j�	\�5cm\�n�D�\n	\�\�\�\�\�dФ�\�A\��>��\�\�\�^~Q��y\�=�i\�\�8�YD��\�G8\�\��<|�\�\�j_\�N]'),
(37,'VisitorInterest_daysSinceLastVisit',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x��\�=1\��Rp4I?\�tsq�\�U*\�P8<�\��n�\�\�Ғ�\�fA/k��\�H\�E��9ߦ٤E0�9O\�\�\�\�8�.e)\�Ť\"\��C&mE��ZYk\�4\��Qc�~)\��B����B@�\�4��u\�k��+AS<x]�� \�ʠ)����3\0v,\�e�=pG\�]\��!\�@\���\�Zb\�\�=��	�CϚ\�e��\�\�tҤ\�\r�\�/�'),
(37,'VisitorInterest_pageGap',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x��\�1\n\�0л:\n,\�NRi\�z:�e�{�:�l_��v3IvtK\�,_A\�ح\Zm\��\�\�\�萰�wӨ\�(��\�$f\�v�\�\�`�(+V)�3V2R\n�E�+)�+ޥ\"e\�+\�P)FK\�\�<d\"K�\���Y�SYo\�\�D�\�CW\�_\�ZŁ'),
(37,'VisitorInterest_timeGap',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x��\�1\n\�0л:\n,\�qy\�z:�e�{�\�\�\�\�$c=�\�L����\�,ޅ\�y\�d��\�{�zX2\n,�\�f�V\�R�6|\�8�X�\�\��G��#\'\ng��N6\�\�K�!�\r�.4cNkrD�FN�Ly$A���\���T�R1�\�R<\�2<y	\�\�u$]�\�ʚ'),
(37,'VisitorInterest_visitsByVisitCount',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x��\�;\n\�0\�:\n$��\�[\�;�\�!�-c\�ݫ%СH\�~?��ey(g=7�>4\�A\�<�(\�\��١��}S�+\�kS�����\�_\���ʏB����)VR�ḏ�#�`��)k�\�Hi\�b�EʂK�,��V\�\�.c�\�D�\�\�_\�R�\�\�p���@3�\�\�\�\�F�2E��l�\�<#\��^�'),
(37,'VisitTime_localTime',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x���M�0�\�\�\�\�pw,	wW��ƟG\\�՛4Я�4_ftK>_�F��\�>O\�z���\�5\�\�\�Q\�Jƚ��d[�+\�o��;�\�����\�\�o(4P�P\�@#�F\r4Qh\�@[\nm5ЎB;\r��\�^=P\�A\��5Pp%A\�)U+ax\�\"�\�C\n\�贡��~ȏ��}\�<�H����\n\"S��\n\�\��\�~\�\�\��\�\�p�Ad0p�A\�0p�Ad1p�A\�1p�Ad2p�A\�2\�.3�ˌ�\�T\�Έ%2�qs�d\�Zn��x'),
(37,'VisitTime_serverTime',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42','x��\�K\n�0�\�d����\�{�Ё\�,Cq\�6�R\�\�\�䆀�N\�\�-��x3\�\�b\�-{\�a�\�5L\�\�\�(\�ʌu�e�\�l\�\�\�\�Ϲ_��\�m?޽Ս�Qh�F���F\Z��h�Ѥ��4\�j��v�hO��&:\�蠉�4:j�\�$Ae\�J\"��Y�\�%p�Pe��\'N\�\��\�\�F:\��\�Ő�\�\�\�\��H\�\�\�_\�\\A\�8X�NDf���Z\�lA\�8\\\�e\\.\�e\\.Sݧ..T\"��\�e�;\��W\�');
/*!40000 ALTER TABLE `matomo_archive_blob_2026_10` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_archive_invalidations` WRITE;
/*!40000 ALTER TABLE `matomo_archive_invalidations` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_archive_invalidations` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `matomo_archive_numeric_2026_10`
--

DROP TABLE IF EXISTS `matomo_archive_numeric_2026_10`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `matomo_archive_numeric_2026_10` (
  `idarchive` int(10) unsigned NOT NULL,
  `name` varchar(190) NOT NULL,
  `idsite` int(10) unsigned DEFAULT NULL,
  `date1` date DEFAULT NULL,
  `date2` date DEFAULT NULL,
  `period` tinyint(3) unsigned DEFAULT NULL,
  `ts_archived` datetime DEFAULT NULL,
  `value` double DEFAULT NULL,
  PRIMARY KEY (`idarchive`,`name`),
  KEY `index_idsite_dates_period` (`idsite`,`date1`,`date2`,`period`,`name`(6)),
  KEY `index_period_archived` (`period`,`ts_archived`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_archive_numeric_2026_10`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_archive_numeric_2026_10` WRITE;
/*!40000 ALTER TABLE `matomo_archive_numeric_2026_10` DISABLE KEYS */;
INSERT INTO `matomo_archive_numeric_2026_10` VALUES
(27,'done90a5a511e1974bca37613b6daec137ba.Goals',1,'2026-10-12','2026-10-12',1,'2026-10-12 11:52:26',1),
(30,'donefea44bece172bc9696ae57c26888bf8a.Goals',1,'2026-10-12','2026-10-12',1,'2026-10-12 11:52:26',1),
(35,'Actions_hits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',18),
(35,'Actions_nb_downloads',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Actions_nb_hits_with_time_generation',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Actions_nb_keywords',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Actions_nb_outlinks',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Actions_nb_pageviews',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',18),
(35,'Actions_nb_searches',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Actions_nb_uniq_downloads',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Actions_nb_uniq_outlinks',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Actions_nb_uniq_pageviews',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',2),
(35,'Actions_sum_time_generation',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'BotTracking_AIChatbotsAcquiredVisits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'BotTracking_AIChatbotsNotFoundRequests',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'BotTracking_AIChatbotsRequests',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'BotTracking_AIChatbotsServerErrorRequests',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'BotTracking_AIChatbotsUniqueChatbots',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'BotTracking_AIChatbotsUniqueDocumentUrls',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'BotTracking_AIChatbotsUniquePageUrls',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'bounce_count',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'done',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',1),
(35,'max_actions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',14),
(35,'nb_actions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',18),
(35,'nb_uniq_visitors',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',2),
(35,'nb_users',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'nb_visits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',2),
(35,'nb_visits_converted',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'PagePerformance_domcompletion_hits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',13),
(35,'PagePerformance_domcompletion_time',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',50794),
(35,'PagePerformance_domprocessing_hits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',14),
(35,'PagePerformance_domprocessing_time',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',80),
(35,'PagePerformance_network_hits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',14),
(35,'PagePerformance_network_time',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',77),
(35,'PagePerformance_onload_hits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',13),
(35,'PagePerformance_onload_time',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',9),
(35,'PagePerformance_pageload_hits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',14),
(35,'PagePerformance_pageload_time',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',51278),
(35,'PagePerformance_server_hits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',14),
(35,'PagePerformance_servery_time',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',313),
(35,'PagePerformance_transfer_hits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',14),
(35,'PagePerformance_transfer_time',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',5),
(35,'Referrers_distinctAIAssistants',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Referrers_distinctCampaigns',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Referrers_distinctKeywords',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Referrers_distinctSearchEngines',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Referrers_distinctSocialNetworks',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Referrers_distinctWebsites',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'Referrers_distinctWebsitesUrls',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(35,'sum_visit_length',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',411),
(35,'UserCountry_distinctCountries',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',2),
(36,'Actions_hits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',18),
(36,'Actions_nb_downloads',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Actions_nb_hits_with_time_generation',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Actions_nb_keywords',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Actions_nb_outlinks',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Actions_nb_pageviews',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',18),
(36,'Actions_nb_searches',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Actions_nb_uniq_downloads',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Actions_nb_uniq_outlinks',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Actions_nb_uniq_pageviews',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',2),
(36,'Actions_sum_time_generation',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'BotTracking_AIChatbotsAcquiredVisits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'BotTracking_AIChatbotsNotFoundRequests',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'BotTracking_AIChatbotsRequests',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'BotTracking_AIChatbotsServerErrorRequests',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'BotTracking_AIChatbotsUniqueChatbots',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'BotTracking_AIChatbotsUniqueDocumentUrls',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'BotTracking_AIChatbotsUniquePageUrls',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'bounce_count',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'done',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',1),
(36,'max_actions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',14),
(36,'nb_actions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',18),
(36,'nb_uniq_visitors',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',2),
(36,'nb_users',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'nb_visits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',2),
(36,'nb_visits_converted',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'PagePerformance_domcompletion_hits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',13),
(36,'PagePerformance_domcompletion_time',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',50794),
(36,'PagePerformance_domprocessing_hits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',14),
(36,'PagePerformance_domprocessing_time',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',80),
(36,'PagePerformance_network_hits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',14),
(36,'PagePerformance_network_time',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',77),
(36,'PagePerformance_onload_hits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',13),
(36,'PagePerformance_onload_time',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',9),
(36,'PagePerformance_pageload_hits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',14),
(36,'PagePerformance_pageload_time',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',51278),
(36,'PagePerformance_server_hits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',14),
(36,'PagePerformance_servery_time',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',313),
(36,'PagePerformance_transfer_hits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',14),
(36,'PagePerformance_transfer_time',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',5),
(36,'Referrers_distinctAIAssistants',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Referrers_distinctCampaigns',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Referrers_distinctKeywords',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Referrers_distinctSearchEngines',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Referrers_distinctSocialNetworks',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Referrers_distinctWebsites',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'Referrers_distinctWebsitesUrls',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(36,'sum_visit_length',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',411),
(36,'UserCountry_distinctCountries',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',2),
(37,'Actions_hits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',18),
(37,'Actions_nb_downloads',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Actions_nb_hits_with_time_generation',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Actions_nb_keywords',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Actions_nb_outlinks',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Actions_nb_pageviews',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',18),
(37,'Actions_nb_searches',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Actions_nb_uniq_downloads',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Actions_nb_uniq_outlinks',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Actions_nb_uniq_pageviews',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',2),
(37,'Actions_sum_time_generation',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'BotTracking_AIChatbotsAcquiredVisits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'BotTracking_AIChatbotsNotFoundRequests',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'BotTracking_AIChatbotsRequests',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'BotTracking_AIChatbotsServerErrorRequests',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'BotTracking_AIChatbotsUniqueChatbots',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'BotTracking_AIChatbotsUniqueDocumentUrls',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'BotTracking_AIChatbotsUniquePageUrls',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'bounce_count',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'done',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',1),
(37,'max_actions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',14),
(37,'nb_actions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',18),
(37,'nb_uniq_visitors',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',2),
(37,'nb_users',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'nb_visits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',2),
(37,'nb_visits_converted',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'PagePerformance_domcompletion_hits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',13),
(37,'PagePerformance_domcompletion_time',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',50794),
(37,'PagePerformance_domprocessing_hits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',14),
(37,'PagePerformance_domprocessing_time',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',80),
(37,'PagePerformance_network_hits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',14),
(37,'PagePerformance_network_time',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',77),
(37,'PagePerformance_onload_hits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',13),
(37,'PagePerformance_onload_time',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',9),
(37,'PagePerformance_pageload_hits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',14),
(37,'PagePerformance_pageload_time',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',51278),
(37,'PagePerformance_server_hits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',14),
(37,'PagePerformance_servery_time',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',313),
(37,'PagePerformance_transfer_hits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',14),
(37,'PagePerformance_transfer_time',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',5),
(37,'Referrers_distinctAIAssistants',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Referrers_distinctCampaigns',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Referrers_distinctKeywords',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Referrers_distinctSearchEngines',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Referrers_distinctSocialNetworks',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Referrers_distinctWebsites',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'Referrers_distinctWebsitesUrls',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(37,'sum_visit_length',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',411),
(37,'UserCountry_distinctCountries',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',2),
(38,'bounce_count',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(38,'done90a5a511e1974bca37613b6daec137ba.VisitsSummary',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',1),
(38,'max_actions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',14),
(38,'nb_actions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',18),
(38,'nb_uniq_visitors',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',2),
(38,'nb_users',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(38,'nb_visits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',2),
(38,'nb_visits_converted',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(38,'sum_visit_length',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',411),
(39,'bounce_count',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(39,'donefea44bece172bc9696ae57c26888bf8a.VisitsSummary',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',1),
(39,'max_actions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(39,'nb_actions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(39,'nb_uniq_visitors',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(39,'nb_users',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(39,'nb_visits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(39,'nb_visits_converted',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(39,'sum_visit_length',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(40,'bounce_count',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(40,'done6e9408519429f50e0541146e88fd262d.VisitsSummary',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',1),
(40,'max_actions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(40,'nb_actions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(40,'nb_uniq_visitors',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(40,'nb_users',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(40,'nb_visits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(40,'nb_visits_converted',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(40,'sum_visit_length',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(41,'bounce_count',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(41,'done69c0d64c636303b5773a21832cf7e301.VisitsSummary',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',1),
(41,'max_actions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',14),
(41,'nb_actions',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',18),
(41,'nb_uniq_visitors',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',2),
(41,'nb_users',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(41,'nb_visits',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',2),
(41,'nb_visits_converted',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',0),
(41,'sum_visit_length',1,'2026-10-12','2026-10-12',1,'2026-10-12 12:49:42',411),
(42,'bounce_count',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(42,'done90a5a511e1974bca37613b6daec137ba.VisitsSummary',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',1),
(42,'max_actions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',14),
(42,'nb_actions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',18),
(42,'nb_uniq_visitors',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',2),
(42,'nb_users',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(42,'nb_visits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',2),
(42,'nb_visits_converted',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(42,'sum_visit_length',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',411),
(43,'bounce_count',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(43,'donefea44bece172bc9696ae57c26888bf8a.VisitsSummary',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',1),
(43,'max_actions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(43,'nb_actions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(43,'nb_uniq_visitors',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(43,'nb_users',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(43,'nb_visits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(43,'nb_visits_converted',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(43,'sum_visit_length',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(44,'bounce_count',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(44,'done6e9408519429f50e0541146e88fd262d.VisitsSummary',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',1),
(44,'max_actions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(44,'nb_actions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(44,'nb_uniq_visitors',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(44,'nb_users',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(44,'nb_visits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(44,'nb_visits_converted',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(44,'sum_visit_length',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(45,'bounce_count',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(45,'done69c0d64c636303b5773a21832cf7e301.VisitsSummary',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',1),
(45,'max_actions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',14),
(45,'nb_actions',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',18),
(45,'nb_uniq_visitors',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',2),
(45,'nb_users',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(45,'nb_visits',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',2),
(45,'nb_visits_converted',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',0),
(45,'sum_visit_length',1,'2026-10-12','2026-10-18',2,'2026-10-12 12:49:42',411),
(46,'bounce_count',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',0),
(46,'done90a5a511e1974bca37613b6daec137ba.VisitsSummary',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',1),
(46,'max_actions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',14),
(46,'nb_actions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',18),
(46,'nb_uniq_visitors',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',2),
(46,'nb_users',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',0),
(46,'nb_visits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',2),
(46,'nb_visits_converted',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',0),
(46,'sum_visit_length',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',411),
(47,'done90a5a511e1974bca37613b6daec137ba.VisitsSummary',1,'2026-10-05','2026-10-11',2,'2026-10-12 12:49:44',1),
(48,'bounce_count',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',0),
(48,'donefea44bece172bc9696ae57c26888bf8a.VisitsSummary',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',1),
(48,'max_actions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',0),
(48,'nb_actions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',0),
(48,'nb_uniq_visitors',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',0),
(48,'nb_users',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',0),
(48,'nb_visits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',0),
(48,'nb_visits_converted',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',0),
(48,'sum_visit_length',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:44',0),
(49,'donefea44bece172bc9696ae57c26888bf8a.VisitsSummary',1,'2026-10-05','2026-10-11',2,'2026-10-12 12:49:44',1),
(50,'bounce_count',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(50,'done6e9408519429f50e0541146e88fd262d.VisitsSummary',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',1),
(50,'max_actions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(50,'nb_actions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(50,'nb_uniq_visitors',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(50,'nb_users',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(50,'nb_visits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(50,'nb_visits_converted',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(50,'sum_visit_length',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(51,'done6e9408519429f50e0541146e88fd262d.VisitsSummary',1,'2026-10-05','2026-10-11',2,'2026-10-12 12:49:45',1),
(52,'bounce_count',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(52,'done69c0d64c636303b5773a21832cf7e301.VisitsSummary',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',1),
(52,'max_actions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',14),
(52,'nb_actions',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',18),
(52,'nb_uniq_visitors',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',2),
(52,'nb_users',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(52,'nb_visits',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',2),
(52,'nb_visits_converted',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',0),
(52,'sum_visit_length',1,'2026-10-01','2026-10-31',3,'2026-10-12 12:49:45',411),
(53,'done69c0d64c636303b5773a21832cf7e301.VisitsSummary',1,'2026-10-05','2026-10-11',2,'2026-10-12 12:49:45',1);
/*!40000 ALTER TABLE `matomo_archive_numeric_2026_10` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_archiving_metrics` WRITE;
/*!40000 ALTER TABLE `matomo_archiving_metrics` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_archiving_metrics` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_brute_force_log`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_brute_force_log` WRITE;
/*!40000 ALTER TABLE `matomo_brute_force_log` DISABLE KEYS */;
INSERT INTO `matomo_brute_force_log` VALUES
(1,'192.168.65.1','2026-10-12 12:48:44',''),
(2,'192.168.65.1','2026-10-12 12:48:44',''),
(3,'192.168.65.1','2026-10-12 12:53:31','admin');
/*!40000 ALTER TABLE `matomo_brute_force_log` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_changes` WRITE;
/*!40000 ALTER TABLE `matomo_changes` DISABLE KEYS */;
INSERT INTO `matomo_changes` VALUES
(1,'2026-10-12 10:59:45','UsersManager','5.9.0','New: Dark Mode is here','Switch between Light and Dark themes from your Personal Settings to suit your workspace. Your preference is saved to your profile and follows you across devices for a more comfortable, accessible Matomo experience.','Switch to Dark Mode','index.php?module=UsersManager&action=userSettings&idSite=1');
/*!40000 ALTER TABLE `matomo_changes` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_custom_dimensions` WRITE;
/*!40000 ALTER TABLE `matomo_custom_dimensions` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_custom_dimensions` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_goal` WRITE;
/*!40000 ALTER TABLE `matomo_goal` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_goal` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_locks` WRITE;
/*!40000 ALTER TABLE `matomo_locks` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_locks` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_log_action`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_log_action` WRITE;
/*!40000 ALTER TABLE `matomo_log_action` DISABLE KEYS */;
INSERT INTO `matomo_log_action` VALUES
(1,'BBMRI-ERIC Directory',3136157843,4,NULL),
(2,'localhost:8080/directory/directory',3807406397,1,0),
(3,'test',3632233996,4,NULL),
(4,'localhost:8080/directory/graphql',883519902,1,0);
/*!40000 ALTER TABLE `matomo_log_action` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_log_bot_request` WRITE;
/*!40000 ALTER TABLE `matomo_log_bot_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_log_bot_request` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_log_conversion` WRITE;
/*!40000 ALTER TABLE `matomo_log_conversion` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_log_conversion` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_log_conversion_item` WRITE;
/*!40000 ALTER TABLE `matomo_log_conversion_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_log_conversion_item` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_log_link_visit_action`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_log_link_visit_action` WRITE;
/*!40000 ALTER TABLE `matomo_log_link_visit_action` DISABLE KEYS */;
INSERT INTO `matomo_log_link_visit_action` VALUES
(1,1,'	\�i8�HY',1,0,0,NULL,1,'2026-10-12 11:07:37','1T9eyM',1,2,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,370,3,0,0,5,1,NULL,NULL,NULL,NULL,NULL,NULL),
(2,1,'	\�i8�HY',1,2,1,NULL,2,'2026-10-12 11:07:38','KhQOJ4',1,2,NULL,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,370,3,0,0,5,1,NULL,NULL,NULL,NULL,NULL,NULL),
(3,1,'	\�i8�HY',1,2,1,NULL,2,'2026-10-12 11:07:38','jfSyQ1',1,2,NULL,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,370,3,0,0,5,1,NULL,NULL,NULL,NULL,NULL,NULL),
(4,1,'	\�i8�HY',1,2,1,NULL,4,'2026-10-12 11:07:38','ehMsfp',1,2,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,370,3,0,0,5,1,NULL,NULL,NULL,NULL,NULL,NULL),
(5,1,'	\�i8�HY',1,2,1,NULL,4,'2026-10-12 11:07:38','N12bm6',1,2,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,370,3,0,0,5,1,NULL,NULL,NULL,NULL,NULL,NULL),
(6,1,'	\�i8�HY',1,2,1,NULL,6,'2026-10-12 11:07:45','9OEiYh',1,2,NULL,NULL,7,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,441,9,0,1,8,0,NULL,NULL,NULL,NULL,NULL,NULL),
(7,1,'	\�i8�HY',1,2,1,NULL,7,'2026-10-12 11:07:46','oKVq55',1,2,NULL,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,441,9,0,1,8,0,NULL,NULL,NULL,NULL,NULL,NULL),
(8,1,'	\�i8�HY',1,2,1,NULL,8,'2026-10-12 11:07:58','ses1b2',1,2,NULL,NULL,12,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,13,29,NULL,9,0,NULL,NULL,NULL,NULL,NULL,NULL),
(9,1,'	\�i8�HY',1,2,1,NULL,9,'2026-10-12 11:08:21','V1m0Sn',1,2,NULL,NULL,23,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,378,5,0,1,2,0,NULL,NULL,NULL,NULL,NULL,NULL),
(10,1,'	\�i8�HY',1,2,1,NULL,10,'2026-10-12 11:08:21','2uIuU0',1,2,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,378,5,0,1,2,0,NULL,NULL,NULL,NULL,NULL,NULL),
(11,1,'�eVq\\�',2,0,0,NULL,1,'2026-10-12 11:10:07','',3,4,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),
(12,1,'	\�i8�HY',1,2,1,NULL,11,'2026-10-12 11:10:41','ppHuPg',1,2,NULL,NULL,140,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,447,6,19,1,87,0,NULL,NULL,NULL,NULL,NULL,NULL),
(13,1,'	\�i8�HY',1,2,1,NULL,12,'2026-10-12 11:10:41','UkaVV6',1,2,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,447,6,19,1,87,0,NULL,NULL,NULL,NULL,NULL,NULL),
(14,1,'	\�i8�HY',1,2,1,NULL,13,'2026-10-12 11:10:49','9E98v5',1,2,NULL,NULL,8,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,366,4,0,1,8,0,NULL,NULL,NULL,NULL,NULL,NULL),
(15,1,'�eVq\\�',2,4,3,NULL,2,'2026-10-12 11:11:48','',3,4,NULL,NULL,101,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),
(16,1,'�eVq\\�',2,4,3,NULL,3,'2026-10-12 11:11:57','',3,4,NULL,NULL,9,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),
(17,1,'�eVq\\�',2,4,3,NULL,4,'2026-10-12 11:12:05','',3,4,NULL,NULL,8,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),
(18,1,'	\�i8�HY',1,2,1,NULL,14,'2026-10-12 11:12:28','Uq39cY',1,2,NULL,NULL,99,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,46046,8,10,2,77,0,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `matomo_log_link_visit_action` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_log_profiling` WRITE;
/*!40000 ALTER TABLE `matomo_log_profiling` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_log_profiling` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `matomo_log_visit`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_log_visit` WRITE;
/*!40000 ALTER TABLE `matomo_log_visit` DISABLE KEYS */;
INSERT INTO `matomo_log_visit` VALUES
(1,1,'	\�i8�HY','2026-10-12 11:12:28','\�gj�Mg�','��\0\0',1,NULL,'2026-10-12 11:07:37',0,0,0,0,NULL,1,1,2,1,2,14,14,0,NULL,NULL,1,'http://localhost:8080/apps/central/','en-us','Gecko','FF','157.0',1,'AP','generic desktop',0,'MAC','10.15',0,'13:07:37',0,'3841x1608',1,0,0,1,0,0,0,0,NULL,292,NULL,'us',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),
(2,1,'�eVq\\�','2026-10-12 11:12:05','HǻU�uP*','��\0\0',1,NULL,'2026-10-12 11:10:07',0,0,0,0,NULL,1,3,4,3,4,4,4,0,NULL,NULL,1,'','','','UNK','',NULL,'','',NULL,'UNK','UNK',0,'11:10:07',0,'unknown',0,0,0,0,0,0,0,0,NULL,119,NULL,'xx',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `matomo_log_visit` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_logger_message` WRITE;
/*!40000 ALTER TABLE `matomo_logger_message` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_logger_message` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_option` WRITE;
/*!40000 ALTER TABLE `matomo_option` DISABLE KEYS */;
INSERT INTO `matomo_option` VALUES
('AIAssistantDefinitions','YToyNTp7czoxNToieWl5YW4uYmFpZHUuY29tIjtzOjg6IkJhaWR1IEFJIjtzOjEyOiJhaS5iYWlkdS5jb20iO3M6ODoiQmFpZHUgQUkiO3M6MTI6ImNoYXRwbHVzLmNvbSI7czo1OiJDaGF0KyI7czoxMjoiY2hhdC1ncHQub3JnIjtzOjU6IkNoYXQrIjtzOjExOiJjaGF0Z3B0LmNvbSI7czo3OiJDaGF0R1BUIjtzOjE1OiJjaGF0Lm9wZW5haS5jb20iO3M6NzoiQ2hhdEdQVCI7czoxNToibGFicy5vcGVuYWkuY29tIjtzOjc6IkNoYXRHUFQiO3M6OToiY2xhdWRlLmFpIjtzOjY6IkNsYXVkZSI7czoyMToiY29waWxvdC5taWNyb3NvZnQuY29tIjtzOjc6IkNvcGlsb3QiO3M6MTA6ImNoYXRnbG0uY24iO3M6NzoiQ2hhdEdMTSI7czoxNzoiY2hhdC5kZWVwc2Vlay5jb20iO3M6ODoiRGVlcHNlZWsiO3M6NzoiZHVjay5haSI7czo3OiJEdWNrLmFpIjtzOjE3OiJnZW1pbmkuZ29vZ2xlLmNvbSI7czo2OiJHZW1pbmkiO3M6MTU6ImJhcmQuZ29vZ2xlLmNvbSI7czo2OiJHZW1pbmkiO3M6ODoiZ3Jvay5jb20iO3M6NDoiR3JvayI7czoxMjoieC5jb20vaS9ncm9rIjtzOjQ6Ikdyb2siO3M6NzoiaWFzay5haSI7czo0OiJpQXNrIjtzOjEzOiJhcHAuamFzcGVyLmFpIjtzOjY6Ikphc3BlciI7czoxNToiY2hhdC5taXN0cmFsLmFpIjtzOjc6IkxlIENoYXQiO3M6NzoibWV0YS5haSI7czo3OiJNZXRhIEFJIjtzOjIxOiJub3RlYm9va2xtLmdvb2dsZS5jb20iO3M6MTA6Ik5vdGVib29rTE0iO3M6MTM6InBlcnBsZXhpdHkuYWkiO3M6MTA6IlBlcnBsZXhpdHkiO3M6MTI6ImNoYXQucXdlbi5haSI7czo0OiJRd2VuIjtzOjE4OiJhcHAud3JpdGVzb25pYy5jb20iO3M6MTA6IldyaXRlc29uaWMiO3M6NzoieW91LmNvbSI7czozOiJZb3UiO30=',0),
('fingerprint_salt_1_2026-10-10','{\"value\":\"pj79gdnjn89p207a536hjysyvtlidb8x\",\"time\":1791802849}',0),
('fingerprint_salt_1_2026-10-11','{\"value\":\"s7jwk3zb6wbye9b5izaa59gcau1jstei\",\"time\":1791802849}',0),
('fingerprint_salt_1_2026-10-12','{\"value\":\"3n30hsgkdrsa2niyfgonwb3914dpixsm\",\"time\":1791802849}',0),
('fingerprint_salt_1_2026-10-13','{\"value\":\"ml9acsjev9mloga0z3d97f1vxier9ez3\",\"time\":1791802849}',0),
('geoip2.loc_db_url','https://download.db-ip.com/free/dbip-city-lite-2026-10.mmdb.gz',0),
('geoip2.updater_last_run_time','1791763200',0),
('geoip2.updater_period','month',0),
('install_mail_sent','1',0),
('install_version','5.14.0',0),
('InvalidatedOldReports_DatesWebsiteIds','a:2:{i:0;s:7:\"2026_10\";i:1;s:7:\"2026_01\";}',0),
('JsTrackerInstallCheck_1','{\"641a5db4dcd53b5b4652b67f21bed058\":{\"time\":1791802961,\"url\":\"http:\\/\\/localhost:8080\",\"isSuccessful\":false}}',0),
('lastTrackerCronRun','1791803258',0),
('LoginFromDifferentCountry.lastCountry.admin','',0),
('Marketplace.unique_id','97f4958c1ae33ccf1fdaa74e6289888f19f61379d0898fc9c051bedf5f1f3aa4',0),
('Marketplace.web_php_version','8.4.26',0),
('MatomoUpdateHistory','5.14.0,',0),
('MobileMessaging_DelegatedManagement','false',0),
('PrivacyManager.ipAnonymizerEnabled','1',0),
('SitesManager_DefaultTimezone','Europe/Amsterdam',0),
('SitesManagerHadTrafficInPast_1','1',0),
('SocialDefinitions','YToyMTg6e3M6OToiYmFkb28uY29tIjtzOjU6IkJhZG9vIjtzOjg6ImJlYm8uY29tIjtzOjQ6IkJlYm8iO3M6MTI6ImJpbGliaWxpLmNvbSI7czo4OiJiaWxpYmlsaSI7czoxNToiYmxhY2twbGFuZXQuY29tIjtzOjExOiJCbGFja1BsYW5ldCI7czo4OiJic2t5LmFwcCI7czo3OiJCbHVlc2t5IjtzOjExOiJza3lmZWVkLmFwcCI7czo3OiJCbHVlc2t5IjtzOjExOiJidXp6bmV0LmNvbSI7czo3OiJCdXp6bmV0IjtzOjE0OiJjbGFzc21hdGVzLmNvbSI7czoxNDoiQ2xhc3NtYXRlcy5jb20iO3M6MTg6Imdsb2JhbC5jeXdvcmxkLmNvbSI7czo3OiJDeXdvcmxkIjtzOjExOiJkaXNjb3JkLmNvbSI7czo3OiJEaXNjb3JkIjtzOjEwOiJkaXNjb3JkLmdnIjtzOjc6IkRpc2NvcmQiO3M6MTQ6ImRpc2NvcmRhcHAuY29tIjtzOjc6IkRpc2NvcmQiO3M6MTA6ImRvdXlpbi5jb20iO3M6NjoiRG91eWluIjtzOjk6ImRvdXl1LmNvbSI7czo1OiJEb3V5dSI7czoxNDoiZ2FpYW9ubGluZS5jb20iO3M6MTE6IkdhaWEgT25saW5lIjtzOjg6ImdlbmkuY29tIjtzOjg6IkdlbmkuY29tIjtzOjEwOiJnaXRodWIuY29tIjtzOjY6IkdpdEh1YiI7czoxNToicGx1cy5nb29nbGUuY29tIjtzOjk6Ikdvb2dsZSUyQiI7czoxNDoidXJsLmdvb2dsZS5jb20iO3M6OToiR29vZ2xlJTJCIjtzOjI4OiJjb20uZ29vZ2xlLmFuZHJvaWQuYXBwcy5wbHVzIjtzOjk6Ikdvb2dsZSUyQiI7czoxMDoiZG91YmFuLmNvbSI7czo2OiJEb3ViYW4iO3M6MTI6ImRyaWJiYmxlLmNvbSI7czo4OiJEcmliYmJsZSI7czoxMjoiZmFjZWJvb2suY29tIjtzOjg6IkZhY2Vib29rIjtzOjU6ImZiLm1lIjtzOjg6IkZhY2Vib29rIjtzOjE0OiJtLmZhY2Vib29rLmNvbSI7czo4OiJGYWNlYm9vayI7czoxNDoibC5mYWNlYm9vay5jb20iO3M6ODoiRmFjZWJvb2siO3M6MTE6ImZldGxpZmUuY29tIjtzOjc6IkZldGxpZmUiO3M6MTA6ImZsaWNrci5jb20iO3M6NjoiRmxpY2tyIjtzOjEyOiJmbGl4c3Rlci5jb20iO3M6ODoiRmxpeHN0ZXIiO3M6MTE6ImZvdG9sb2cuY29tIjtzOjc6IkZvdG9sb2ciO3M6MTQ6ImZvdXJzcXVhcmUuY29tIjtzOjEwOiJGb3Vyc3F1YXJlIjtzOjE5OiJmcmllbmRzcmV1bml0ZWQuY29tIjtzOjE2OiJGcmllbmRzIFJldW5pdGVkIjtzOjE0OiJmcmllbmRzdGVyLmNvbSI7czoxMDoiRnJpZW5kc3RlciI7czo3OiJncmVlLmpwIjtzOjQ6ImdyZWUiO3M6OToiaGFiYm8uY29tIjtzOjU6IkhhYm9vIjtzOjIwOiJuZXdzLnljb21iaW5hdG9yLmNvbSI7czoxMToiSGFja2VyIE5ld3MiO3M6NzoiaGk1LmNvbSI7czozOiJoaTUiO3M6ODoiaHV5YS5jb20iO3M6NDoiSHV5YSI7czo4OiJoeXZlcy5ubCI7czo1OiJIeXZlcyI7czo5OiJpZGVudGkuY2EiO3M6OToiaWRlbnRpLmNhIjtzOjEzOiJpbnN0YWdyYW0uY29tIjtzOjk6Ikluc3RhZ3JhbSI7czoxNToibC5pbnN0YWdyYW0uY29tIjtzOjk6Ikluc3RhZ3JhbSI7czoxMDoibGFuZy04LmNvbSI7czo2OiJsYW5nLTgiO3M6NzoibGFzdC5mbSI7czo3OiJMYXN0LmZtIjtzOjk6Imxhc3RmbS5ydSI7czo3OiJMYXN0LmZtIjtzOjk6Imxhc3RmbS5kZSI7czo3OiJMYXN0LmZtIjtzOjk6Imxhc3RmbS5lcyI7czo3OiJMYXN0LmZtIjtzOjk6Imxhc3RmbS5mciI7czo3OiJMYXN0LmZtIjtzOjk6Imxhc3RmbS5pdCI7czo3OiJMYXN0LmZtIjtzOjk6Imxhc3RmbS5qcCI7czo3OiJMYXN0LmZtIjtzOjk6Imxhc3RmbS5wbCI7czo3OiJMYXN0LmZtIjtzOjEzOiJsYXN0Zm0uY29tLmJyIjtzOjc6Ikxhc3QuZm0iO3M6OToibGFzdGZtLnNlIjtzOjc6Ikxhc3QuZm0iO3M6MTM6Imxhc3RmbS5jb20udHIiO3M6NzoiTGFzdC5mbSI7czoxMjoibGlua2VkaW4uY29tIjtzOjg6IkxpbmtlZEluIjtzOjc6Imxua2QuaW4iO3M6ODoiTGlua2VkSW4iO3M6MTY6ImxpbmtlZGluLmFuZHJvaWQiO3M6ODoiTGlua2VkSW4iO3M6MTQ6ImxpdmVqb3VybmFsLnJ1IjtzOjExOiJMaXZlSm91cm5hbCI7czoxNToibGl2ZWpvdXJuYWwuY29tIjtzOjExOiJMaXZlSm91cm5hbCI7czoxNToibWFzdG9kb24uc29jaWFsIjtzOjg6Ik1hc3RvZG9uIjtzOjE0OiJtYXN0b2Rvbi5jbG91ZCI7czo4OiJNYXN0b2RvbiI7czoxOToibWFzdG9kb24udGVjaG5vbG9neSI7czo4OiJNYXN0b2RvbiI7czoxMjoibWFzdG9kb24ueHl6IjtzOjg6Ik1hc3RvZG9uIjtzOjExOiJtYXN0b2Rvbi5hdCI7czo4OiJNYXN0b2RvbiI7czoxMjoibWFzdG9kb24uYXJ0IjtzOjg6Ik1hc3RvZG9uIjtzOjg6Im1hbW90LmZyIjtzOjg6Ik1hc3RvZG9uIjtzOjk6InBhd29vLm5ldCI7czo4OiJNYXN0b2RvbiI7czo4OiJtc3Rkbi5pbyI7czo4OiJNYXN0b2RvbiI7czo4OiJtc3Rkbi5qcCI7czo4OiJNYXN0b2RvbiI7czoxMjoiZnJpZW5kcy5uaWNvIjtzOjg6Ik1hc3RvZG9uIjtzOjE5OiJyby1tYXN0b2Rvbi5wdXlvLmpwIjtzOjg6Ik1hc3RvZG9uIjtzOjg6InF1ZXkub3JnIjtzOjg6Ik1hc3RvZG9uIjtzOjEyOiJib3RzaW4uc3BhY2UiO3M6ODoiTWFzdG9kb24iO3M6MTY6InNvY2lhbC50Y2huY3MuZGUiO3M6ODoiTWFzdG9kb24iO3M6Nzoia256ay5tZSI7czo4OiJNYXN0b2RvbiI7czoxMzoibWFzdG9kb250LmNhdCI7czo4OiJNYXN0b2RvbiI7czoxODoiYml0Y29pbmhhY2tlcnMub3JnIjtzOjg6Ik1hc3RvZG9uIjtzOjEzOiJmb3NzdG9kb24ub3JnIjtzOjg6Ik1hc3RvZG9uIjtzOjEyOiJjaGFvcy5zb2NpYWwiO3M6ODoiTWFzdG9kb24iO3M6MTE6ImN5YnJlLnNwYWNlIjtzOjg6Ik1hc3RvZG9uIjtzOjEwOiJ2aXMuc29jaWFsIjtzOjg6Ik1hc3RvZG9uIjtzOjExOiJ0eXBvLnNvY2lhbCI7czo4OiJNYXN0b2RvbiI7czoxNjoiZnJvbnQtZW5kLnNvY2lhbCI7czo4OiJNYXN0b2RvbiI7czoxMjoiaGFjaHlkZXJtLmlvIjtzOjg6Ik1hc3RvZG9uIjtzOjE1OiJtYXN0b2Rvbi5vbmxpbmUiO3M6ODoiTWFzdG9kb24iO3M6MTM6Im5ld3NpZS5zb2NpYWwiO3M6ODoiTWFzdG9kb24iO3M6MTI6Im1zdGRuLnNvY2lhbCI7czo4OiJNYXN0b2RvbiI7czoxNToiaW5kaWV3ZWIuc29jaWFsIjtzOjg6Ik1hc3RvZG9uIjtzOjExOiJzZmJhLnNvY2lhbCI7czo4OiJNYXN0b2RvbiI7czo2OiJtYXMudG8iO3M6ODoiTWFzdG9kb24iO3M6MTE6Im1zdGRuLnBhcnR5IjtzOjg6Ik1hc3RvZG9uIjtzOjQ6ImMuaW0iO3M6ODoiTWFzdG9kb24iO3M6MTQ6Im1hc3RvZG9uYXBwLnVrIjtzOjg6Ik1hc3RvZG9uIjtzOjE2OiJ1bml2ZXJzZW9kb24uY29tIjtzOjg6Ik1hc3RvZG9uIjtzOjE4OiJzb2NpYWwudml2YWxkaS5uZXQiO3M6ODoiTWFzdG9kb24iO3M6MTE6Im9oYWkuc29jaWFsIjtzOjg6Ik1hc3RvZG9uIjtzOjE0OiJ0b290LmNvbW11bml0eSI7czo4OiJNYXN0b2RvbiI7czo4OiJtYXN0by5haSI7czo4OiJNYXN0b2RvbiI7czoxMzoibWFzdG9kb24uc2NvdCI7czo4OiJNYXN0b2RvbiI7czoxNDoibWFzdG9kb24ud29ybGQiO3M6ODoiTWFzdG9kb24iO3M6MTE6Im1hc3RvZG9uLm56IjtzOjg6Ik1hc3RvZG9uIjtzOjE1OiJncmFwaGljcy5zb2NpYWwiO3M6ODoiTWFzdG9kb24iO3M6MjQ6Im9yZy5qb2lubWFzdG9kb24uYW5kcm9pZCI7czo4OiJNYXN0b2RvbiI7czoxMjoibWFzdG9kb24uZXVzIjtzOjg6Ik1hc3RvZG9uIjtzOjE4OiJtYXN0b2Rvbi5qYWxnaS5ldXMiO3M6ODoiTWFzdG9kb24iO3M6NzoidGttLmV1cyI7czo4OiJNYXN0b2RvbiI7czoxMDoibWVpbnZ6Lm5ldCI7czo2OiJNZWluVloiO3M6MTI6Im1pc3NldmFuLmNvbSI7czo4OiJNaXNzRXZhbiI7czo3OiJtaXhpLmpwIjtzOjQ6Ik1peGkiO3M6MTA6Im1vaWtydWcucnUiO3M6MTA6Ik1vaUtydWcucnUiO3M6MTI6Im11bHRpcGx5LmNvbSI7czo4OiJNdWx0aXBseSI7czoxMDoibXkubWFpbC5ydSI7czoxMDoibXkubWFpbC5ydSI7czoxNDoibXloZXJpdGFnZS5jb20iO3M6MTA6Ik15SGVyaXRhZ2UiO3M6OToibXlsaWZlLnJ1IjtzOjY6Ik15TGlmZSI7czoxMToibXlzcGFjZS5jb20iO3M6NzoiTXlzcGFjZSI7czoxNDoibXl5ZWFyYm9vay5jb20iO3M6MTA6Im15WWVhcmJvb2siO3M6NToibmsucGwiO3M6MTQ6Ik5hc3phLWtsYXNhLnBsIjtzOjEwOiJuZXRsb2cuY29tIjtzOjY6Ik5ldGxvZyI7czoxMjoibmljb3ZpZGVvLmpwIjtzOjg6Ik5pY29uaWNvIjtzOjE2OiJvZG5va2xhc3NuaWtpLnJ1IjtzOjEzOiJPZG5va2xhc3NuaWtpIjtzOjk6Im9ya3V0LmNvbSI7czo1OiJPcmt1dCI7czoxMjoicXpvbmUucXEuY29tIjtzOjU6Ik96b25lIjtzOjExOiJwZWVwZXRoLmNvbSI7czo3OiJQZWVwZXRoIjtzOjEzOiJwaW50ZXJlc3QuY29tIjtzOjk6IlBpbnRlcmVzdCI7czoxMjoicGludGVyZXN0LmNhIjtzOjk6IlBpbnRlcmVzdCI7czoxMjoicGludGVyZXN0LmNoIjtzOjk6IlBpbnRlcmVzdCI7czoxNToicGludGVyZXN0LmNvLnVrIjtzOjk6IlBpbnRlcmVzdCI7czoxNjoicGludGVyZXN0LmNvbS5hdSI7czo5OiJQaW50ZXJlc3QiO3M6MTI6InBpbnRlcmVzdC5kZSI7czo5OiJQaW50ZXJlc3QiO3M6MTI6InBpbnRlcmVzdC5kayI7czo5OiJQaW50ZXJlc3QiO3M6MTI6InBpbnRlcmVzdC5lcyI7czo5OiJQaW50ZXJlc3QiO3M6MTI6InBpbnRlcmVzdC5mciI7czo5OiJQaW50ZXJlc3QiO3M6MTI6InBpbnRlcmVzdC5pZSI7czo5OiJQaW50ZXJlc3QiO3M6MTQ6InBpbnRlcmVzdC5pbmZvIjtzOjk6IlBpbnRlcmVzdCI7czoxMjoicGludGVyZXN0LmpwIjtzOjk6IlBpbnRlcmVzdCI7czoxMjoicGludGVyZXN0Lm56IjtzOjk6IlBpbnRlcmVzdCI7czoxMjoicGludGVyZXN0LnB0IjtzOjk6IlBpbnRlcmVzdCI7czoxMjoicGludGVyZXN0LnNlIjtzOjk6IlBpbnRlcmVzdCI7czoxNjoiYXQucGludGVyZXN0LmNvbSI7czo5OiJQaW50ZXJlc3QiO3M6MTY6ImNoLnBpbnRlcmVzdC5jb20iO3M6OToiUGludGVyZXN0IjtzOjE2OiJjbC5waW50ZXJlc3QuY29tIjtzOjk6IlBpbnRlcmVzdCI7czoxNjoiY28ucGludGVyZXN0LmNvbSI7czo5OiJQaW50ZXJlc3QiO3M6MTY6ImRrLnBpbnRlcmVzdC5jb20iO3M6OToiUGludGVyZXN0IjtzOjE2OiJlcy5waW50ZXJlc3QuY29tIjtzOjk6IlBpbnRlcmVzdCI7czoxNjoiaHUucGludGVyZXN0LmNvbSI7czo5OiJQaW50ZXJlc3QiO3M6MTY6ImlkLnBpbnRlcmVzdC5jb20iO3M6OToiUGludGVyZXN0IjtzOjE2OiJpZS5waW50ZXJlc3QuY29tIjtzOjk6IlBpbnRlcmVzdCI7czoxNjoiaW4ucGludGVyZXN0LmNvbSI7czo5OiJQaW50ZXJlc3QiO3M6MTY6Iml0LnBpbnRlcmVzdC5jb20iO3M6OToiUGludGVyZXN0IjtzOjE2OiJrci5waW50ZXJlc3QuY29tIjtzOjk6IlBpbnRlcmVzdCI7czoxNjoibXgucGludGVyZXN0LmNvbSI7czo5OiJQaW50ZXJlc3QiO3M6MTY6Im5sLnBpbnRlcmVzdC5jb20iO3M6OToiUGludGVyZXN0IjtzOjE2OiJuei5waW50ZXJlc3QuY29tIjtzOjk6IlBpbnRlcmVzdCI7czoxNjoicGgucGludGVyZXN0LmNvbSI7czo5OiJQaW50ZXJlc3QiO3M6MTY6InB0LnBpbnRlcmVzdC5jb20iO3M6OToiUGludGVyZXN0IjtzOjE2OiJydS5waW50ZXJlc3QuY29tIjtzOjk6IlBpbnRlcmVzdCI7czoxNjoic2UucGludGVyZXN0LmNvbSI7czo5OiJQaW50ZXJlc3QiO3M6MTU6InBpeGVsZmVkLnNvY2lhbCI7czo4OiJQaXhlbGZlZCI7czoxMToicGl4ZWxmZWQuZGUiO3M6ODoiUGl4ZWxmZWQiO3M6OToicHhsbW8uY29tIjtzOjg6IlBpeGVsZmVkIjtzOjEyOiJtZXRhcGl4bC5jb20iO3M6ODoiUGl4ZWxmZWQiO3M6MTQ6InBpeC50b290LndhbGVzIjtzOjg6IlBpeGVsZmVkIjtzOjEyOiJwaXhlbGZlZC51bm8iO3M6ODoiUGl4ZWxmZWQiO3M6OToicGl4ZXkub3JnIjtzOjg6IlBpeGVsZmVkIjtzOjk6InBsYXhvLmNvbSI7czo1OiJQbGF4byI7czoxMDoicmVkZGl0LmNvbSI7czo2OiJyZWRkaXQiO3M6MTM6Im5wLnJlZGRpdC5jb20iO3M6NjoicmVkZGl0IjtzOjE0OiJwYXkucmVkZGl0LmNvbSI7czo2OiJyZWRkaXQiO3M6MjA6ImNvbS5yZWRkaXQuZnJvbnRwYWdlIjtzOjY6InJlZGRpdCI7czoxMDoicmVucmVuLmNvbSI7czo2OiJSZW5yZW4iO3M6MTE6InNreXJvY2suY29tIjtzOjc6IlNreXJvY2siO3M6MTI6InNuYXBjaGF0LmNvbSI7czo4OiJTbmFwY2hhdCI7czoyMDoiY29tLnNuYXBjaGF0LmFuZHJvaWQiO3M6ODoiU25hcGNoYXQiO3M6MTA6InNvbmljby5jb20iO3M6MTA6IlNvbmljby5jb20iO3M6MTQ6InNvdW5kY2xvdWQuY29tIjtzOjEwOiJTb3VuZENsb3VkIjtzOjc6ImdhdGUuc2MiO3M6MTA6IlNvdW5kQ2xvdWQiO3M6MTc6InN0YWNrb3ZlcmZsb3cuY29tIjtzOjEzOiJTdGFja092ZXJmbG93IjtzOjExOiJzdHVkaXZ6Lm5ldCI7czo3OiJTdHVkaVZaIjtzOjE2OiJsb2dpbi50YWdnZWQuY29tIjtzOjY6IlRhZ2dlZCI7czoxMToidGFyaW5nYS5uZXQiO3M6ODoiVGFyaW5nYSEiO3M6MTY6IndlYi50ZWxlZ3JhbS5vcmciO3M6ODoiVGVsZWdyYW0iO3M6MjI6Im9yZy50ZWxlZ3JhbS5tZXNzZW5nZXIiO3M6ODoiVGVsZWdyYW0iO3M6MTE6InRocmVhZHMubmV0IjtzOjc6IlRocmVhZHMiO3M6MTM6ImwudGhyZWFkcy5uZXQiO3M6NzoiVGhyZWFkcyI7czoxMToidGhyZWFkcy5jb20iO3M6NzoiVGhyZWFkcyI7czoxMzoibC50aHJlYWRzLmNvbSI7czo3OiJUaHJlYWRzIjtzOjEwOiJ0aWt0b2suY29tIjtzOjY6IlRpa1RvayI7czoxMDoidHVlbnRpLmNvbSI7czo2OiJUdWVudGkiO3M6MTA6InR1bWJsci5jb20iO3M6NjoidHVtYmxyIjtzOjExOiJ0LnVtYmxyLmNvbSI7czo2OiJ0dW1ibHIiO3M6MTQ6InR3aXRjYXN0aW5nLnR2IjtzOjg6IlR3aXRjYXN0IjtzOjExOiJ0d2l0dGVyLmNvbSI7czo3OiJUd2l0dGVyIjtzOjQ6InQuY28iO3M6NzoiVHdpdHRlciI7czo1OiJ4LmNvbSI7czo3OiJUd2l0dGVyIjtzOjE1OiJzb3VyY2Vmb3JnZS5uZXQiO3M6MTE6IlNvdXJjZWZvcmdlIjtzOjE1OiJzdHVtYmxldXBvbi5jb20iO3M6MTE6IlN0dW1ibGVVcG9uIjtzOjY6InZrLmNvbSI7czo5OiJWa29udGFrdGUiO3M6MTI6InZrb250YWt0ZS5ydSI7czo5OiJWa29udGFrdGUiO3M6MTE6InlvdXR1YmUuY29tIjtzOjc6IllvdVR1YmUiO3M6ODoieW91dHUuYmUiO3M6NzoiWW91VHViZSI7czo4OiJ2MmV4LmNvbSI7czo0OiJWMkVYIjtzOjEwOiJ2aWFkZW8uY29tIjtzOjY6IlZpYWRlbyI7czo5OiJ2aW1lby5jb20iO3M6NToiVmltZW8iO3M6MTU6InZrcnVndWRydXplaS5ydSI7czoxNToidmtydWd1ZHJ1emVpLnJ1IjtzOjg6IndheW4uY29tIjtzOjQ6IldBWU4iO3M6MTA6IndlY2hhdC5jb20iO3M6NjoiV2VDaGF0IjtzOjE2OiJtcC53ZWl4aW4ucXEuY29tIjtzOjY6IldlQ2hhdCI7czoxODoib3Blbi53ZWl4aW4ucXEuY29tIjtzOjY6IldlQ2hhdCI7czoyMjoiY2hhbm5lbHMud2VpeGluLnFxLmNvbSI7czo2OiJXZUNoYXQiO3M6OToid2VpYm8uY29tIjtzOjU6IldlaWJvIjtzOjQ6InQuY24iO3M6NToiV2VpYm8iO3M6MTI6IndlZXdvcmxkLmNvbSI7czo4OiJXZWVXb3JsZCI7czoxNDoibG9naW4ubGl2ZS5jb20iO3M6MTk6IldpbmRvd3MgTGl2ZSBTcGFjZXMiO3M6MTM6IndvcmtwbGFjZS5jb20iO3M6OToiV29ya3BsYWNlIjtzOjE1OiJsLndvcmtwbGFjZS5jb20iO3M6OToiV29ya3BsYWNlIjtzOjE2OiJsbS53b3JrcGxhY2UuY29tIjtzOjk6IldvcmtwbGFjZSI7czo5OiJ4YW5nYS5jb20iO3M6NToiWGFuZ2EiO3M6ODoieGluZy5jb20iO3M6NDoiWElORyI7fQ==',0),
('TaskScheduler.timetable','a:32:{s:45:\"Piwik\\Plugins\\GeoIp2\\GeoIP2AutoUpdater.update\";i:1793664031;s:60:\"Piwik\\Plugins\\CoreAdminHome\\Tasks.invalidateOutdatedArchives\";i:1791849638;s:59:\"Piwik\\Plugins\\CoreAdminHome\\Tasks.deleteOldFingerprintSalts\";i:1791849638;s:55:\"Piwik\\Plugins\\CoreAdminHome\\Tasks.purgeOutdatedArchives\";i:1791849638;s:55:\"Piwik\\Plugins\\CoreAdminHome\\Tasks.purgeOrphanedArchives\";i:1792368038;s:51:\"Piwik\\Plugins\\CoreAdminHome\\Tasks.updateSpammerList\";i:1792368038;s:61:\"Piwik\\Plugins\\CoreAdminHome\\Tasks.checkSiteHasTrackedVisits_1\";i:1792116000;s:49:\"Piwik\\Plugins\\Referrers\\Tasks.updateSearchEngines\";i:1792368038;s:43:\"Piwik\\Plugins\\Referrers\\Tasks.updateSocials\";i:1792368038;s:48:\"Piwik\\Plugins\\Referrers\\Tasks.updateAIAssistants\";i:1792368038;s:47:\"Piwik\\Plugins\\Login\\Tasks.cleanupBruteForceLogs\";i:1791849638;s:63:\"Piwik\\Plugins\\TwoFactorAuth\\Tasks.cleanupTwoFaCodesUsedRecently\";i:1791849638;s:53:\"Piwik\\Plugins\\UsersManager\\Tasks.cleanupExpiredTokens\";i:1791849638;s:63:\"Piwik\\Plugins\\UsersManager\\Tasks.setUserDefaultReportPreference\";i:1791849638;s:54:\"Piwik\\Plugins\\UsersManager\\Tasks.cleanUpExpiredInvites\";i:1791849638;s:85:\"Piwik\\Plugins\\UsersManager\\TokenNotifications\\TokenNotifierTask.dispatchNotifications\";i:1791849638;s:83:\"Piwik\\Plugins\\UsersManager\\UserNotifications\\UserNotifierTask.dispatchNotifications\";i:1793491238;s:49:\"Piwik\\Plugins\\CustomJsTracker\\Tasks.updateTracker\";i:1791806438;s:58:\"Piwik\\Plugins\\CoreAdminHome\\Tasks.purgeInvalidatedArchives\";i:1791849638;s:65:\"Piwik\\Plugins\\CoreAdminHome\\Tasks.purgeBrokenArchivesCurrentMonth\";i:1791849638;s:67:\"Piwik\\Plugins\\CoreAdminHome\\Tasks.purgeInvalidationsForDeletedSites\";i:1791849638;s:51:\"Piwik\\Plugins\\PrivacyManager\\Tasks.deleteReportData\";i:1791849638;s:48:\"Piwik\\Plugins\\PrivacyManager\\Tasks.deleteLogData\";i:1791806438;s:52:\"Piwik\\Plugins\\PrivacyManager\\Tasks.anonymizePastData\";i:1791806438;s:63:\"Piwik\\Plugins\\PrivacyManager\\Tasks.deleteLogDataForDeletedSites\";i:1792368038;s:54:\"Piwik\\Plugins\\CoreAdminHome\\Tasks.optimizeArchiveTable\";i:1793491238;s:57:\"Piwik\\Plugins\\CoreAdminHome\\Tasks.cleanupTrackingFailures\";i:1791849638;s:56:\"Piwik\\Plugins\\CoreAdminHome\\Tasks.notifyTrackingFailures\";i:1792368038;s:65:\"Piwik\\Plugins\\CoreUpdater\\Tasks.sendNotificationIfUpdateAvailable\";i:1791849638;s:52:\"Piwik\\Plugins\\Marketplace\\Tasks.clearAllCacheEntries\";i:1791849638;s:48:\"Piwik\\Plugins\\Marketplace\\Tasks.warmCacheEntries\";i:1791806438;s:66:\"Piwik\\Plugins\\Marketplace\\Tasks.sendNotificationIfUpdatesAvailable\";i:1791849638;}',0),
('TransactionLevel.testOption','1',0),
('UpdateCheck_LastCheckFailed','',0),
('UpdateCheck_LastTimeChecked','1791802793',1),
('UpdateCheck_LatestVersion','5.14.1',0),
('usercountry.location_provider','geoip2php',0),
('usercountry.switchtoisoregions','1791802891',0),
('useridsalt','Sx7brPgaW1WMn1VX4Jpiicg7WCSwUkTa$yILdAa7',1),
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
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_plugin_setting` WRITE;
/*!40000 ALTER TABLE `matomo_plugin_setting` DISABLE KEYS */;
INSERT INTO `matomo_plugin_setting` VALUES
('Feedback','nextFeedbackReminder','2027-04-12',0,'admin',1);
/*!40000 ALTER TABLE `matomo_plugin_setting` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_privacy_logdata_anonymizations` WRITE;
/*!40000 ALTER TABLE `matomo_privacy_logdata_anonymizations` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_privacy_logdata_anonymizations` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_report` WRITE;
/*!40000 ALTER TABLE `matomo_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_report` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_report_subscriptions` WRITE;
/*!40000 ALTER TABLE `matomo_report_subscriptions` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_report_subscriptions` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_segment` WRITE;
/*!40000 ALTER TABLE `matomo_segment` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_segment` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_sequence` WRITE;
/*!40000 ALTER TABLE `matomo_sequence` DISABLE KEYS */;
INSERT INTO `matomo_sequence` VALUES
('matomo_archive_numeric_2026_10',53);
/*!40000 ALTER TABLE `matomo_sequence` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_session` WRITE;
/*!40000 ALTER TABLE `matomo_session` DISABLE KEYS */;
INSERT INTO `matomo_session` VALUES
('7136fbfe619df415c6cfe168333fc1d90f0393d874c9486bd040d53438fa7b839451abc5b9be5a59fc7227b1ff70e1b8882ccfc5bc14891dec0be58636150431',1791809695,1209600,'a:1:{s:4:\"data\";s:1940:\"YToxMTp7czo0OiJfX1pGIjthOjc6e3M6MTE6IkxvZ2luLmxvZ2luIjthOjE6e3M6NDoiRU5WVCI7YToxOntzOjU6Im5vbmNlIjtpOjE3OTE4MDk5MjY7fX1zOjE5OiJjaGFuZ2VQYXNzd29yZE5vbmNlIjthOjE6e3M6NDoiRU5WVCI7YToxOntzOjU6Im5vbmNlIjtpOjE3OTE4MTAyOTU7fX1zOjIwOiJkZWxldGVBdXRoVG9rZW5Ob25jZSI7YToxOntzOjQ6IkVOVlQiO2E6MTp7czo1OiJub25jZSI7aToxNzkxODEwMjk1O319czoyOToiVHdvRmFjdG9yQXV0aC5kaXNhYmxlQXV0aENvZGUiO2E6MTp7czo0OiJFTlZUIjthOjE6e3M6NToibm9uY2UiO2k6MTc5MTgxMDI5NTt9fXM6NToiTG9naW4iO2E6MTp7czo0OiJFTlZUIjthOjM6e3M6MTQ6InJlZGlyZWN0UGFyYW1zIjtpOjE3OTE4MTE0MTM7czoxNjoibGFzdFBhc3N3b3JkQXV0aCI7aToxNzkxODExNDEzO3M6MjE6InBhc3N3b3JkVmVyaWZpZWRMb2dpbiI7aToxNzkxODExNDEzO319czoxNToiY29uZmlybVBhc3N3b3JkIjthOjE6e3M6NDoiRU5WVCI7YToxOntzOjU6Im5vbmNlIjtpOjE3OTE4MTAyMTE7fX1zOjE3OiJhZGRBdXRoVG9rZW5Ob25jZSI7YToxOntzOjQ6IkVOVlQiO2E6MTp7czo1OiJub25jZSI7aToxNzkxODEwMjEzO319fXM6OToidXNlci5uYW1lIjtzOjU6ImFkbWluIjtzOjIyOiJ0d29mYWN0b3JhdXRoLnZlcmlmaWVkIjtpOjA7czoyMDoidXNlci50b2tlbl9hdXRoX3RlbXAiO3M6MzI6IjljN2Q1NjMxODZiNGRhYWNjYWI4NzJiNzg3MTY4MGNiIjtzOjEyOiJzZXNzaW9uLmluZm8iO2E6Mzp7czoyOiJ0cyI7aToxNzkxODA5MzMxO3M6MTA6InJlbWVtYmVyZWQiO2I6MDtzOjEwOiJleHBpcmF0aW9uIjtpOjE3OTE4MTMyOTU7fXM6MTI6Im5vdGlmaWNhdGlvbiI7YToxOntzOjEzOiJub3RpZmljYXRpb25zIjthOjA6e319czoxOToiY2hhbmdlUGFzc3dvcmROb25jZSI7YToxOntzOjU6Im5vbmNlIjtzOjMyOiJjZTNkOWRiNGVlNWMzNDIwYjg4NTllMTVjMjA5YTVhNiI7fXM6MjA6ImRlbGV0ZUF1dGhUb2tlbk5vbmNlIjthOjE6e3M6NToibm9uY2UiO3M6MzI6ImJkNmJjNTJkZDBmYjZkMjI1MjIyZGQ4Yjg5MDQ3NzVhIjt9czoyOToiVHdvRmFjdG9yQXV0aC5kaXNhYmxlQXV0aENvZGUiO2E6MTp7czo1OiJub25jZSI7czozMjoiOTFiMTg5OWY4NmRjNDU0YjlkMDA4ZmJhNDE3Njk5NmYiO31zOjU6IkxvZ2luIjthOjM6e3M6MTQ6InJlZGlyZWN0UGFyYW1zIjthOjI6e3M6NjoibW9kdWxlIjtzOjEyOiJVc2Vyc01hbmFnZXIiO3M6NjoiYWN0aW9uIjtzOjExOiJhZGROZXdUb2tlbiI7fXM6MTY6Imxhc3RQYXNzd29yZEF1dGgiO3M6MTk6IjIwMjYtMTAtMTIgMTI6NTM6MzMiO3M6MjE6InBhc3N3b3JkVmVyaWZpZWRMb2dpbiI7czo1OiJhZG1pbiI7fXM6MTU6ImNvbmZpcm1QYXNzd29yZCI7YToxOntzOjU6Im5vbmNlIjtzOjMyOiIzZTM0ZjdkMmU5MjQ4NmZjMzNiMDEyZTExMjExYzRlYiI7fX0=\";}'),
('8b4dfdc2988b0a8c81162379fd389782e25388c071efc83adad9221b064353383b17ddaada508158d63079c4503aec6972b00fba7a4f959dc3ecb64b14a8a058',1791806476,1209600,'a:1:{s:4:\"data\";s:552:\"YTo3OntzOjQ6Il9fWkYiO2E6MTp7czoxNToic2l0ZVdpdGhvdXREYXRhIjthOjE6e3M6MzoiRU5UIjtpOjE3OTE4MDY2MjE7fX1zOjk6InVzZXIubmFtZSI7czo1OiJhZG1pbiI7czoyMjoidHdvZmFjdG9yYXV0aC52ZXJpZmllZCI7aTowO3M6MjA6InVzZXIudG9rZW5fYXV0aF90ZW1wIjtzOjMyOiJmNjg1NDBiMTQyNTc2NmMyMzc4MjgyMDIwZDJhZjY0OCI7czoxMjoic2Vzc2lvbi5pbmZvIjthOjM6e3M6MjoidHMiO2k6MTc5MTgwMjg4MjtzOjEwOiJyZW1lbWJlcmVkIjtiOjA7czoxMDoiZXhwaXJhdGlvbiI7aToxNzkxODEwMDc2O31zOjEyOiJub3RpZmljYXRpb24iO2E6MTp7czoxMzoibm90aWZpY2F0aW9ucyI7YTowOnt9fXM6MTU6InNpdGVXaXRob3V0RGF0YSI7YToxOntzOjEzOiJpZ25vcmVNZXNzYWdlIjtiOjE7fX0=\";}');
/*!40000 ALTER TABLE `matomo_session` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_site` WRITE;
/*!40000 ALTER TABLE `matomo_site` DISABLE KEYS */;
INSERT INTO `matomo_site` VALUES
(1,'emx2','','http://localhost:8080','2026-10-11 00:00:00',0,1,'','','Europe/Amsterdam','USD',0,'','','','','','website',0,'anonymous');
/*!40000 ALTER TABLE `matomo_site` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_site_setting` WRITE;
/*!40000 ALTER TABLE `matomo_site_setting` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_site_setting` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_site_url` WRITE;
/*!40000 ALTER TABLE `matomo_site_url` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_site_url` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_tracking_failure` WRITE;
/*!40000 ALTER TABLE `matomo_tracking_failure` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_tracking_failure` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_twofactor_recovery_code` WRITE;
/*!40000 ALTER TABLE `matomo_twofactor_recovery_code` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_twofactor_recovery_code` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_user` WRITE;
/*!40000 ALTER TABLE `matomo_user` DISABLE KEYS */;
INSERT INTO `matomo_user` VALUES
('admin','$2y$12$0bWkgOSqwO1yANcO4e2O2.GIUxMRDWbbVg7lDr8d94RepMqueIgaa','foo@example.org','',1,'2026-10-12 11:00:26','2026-10-12 11:00:26',1,NULL,NULL,NULL,NULL,NULL,'2026-10-12 11:01:32','2026-10-12 12:54:28',NULL),
('anonymous','','anonymous@example.org','',0,'2026-10-12 10:59:45','2026-10-12 10:59:45',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `matomo_user` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_user_dashboard` WRITE;
/*!40000 ALTER TABLE `matomo_user_dashboard` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_user_dashboard` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_user_language` WRITE;
/*!40000 ALTER TABLE `matomo_user_language` DISABLE KEYS */;
/*!40000 ALTER TABLE `matomo_user_language` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

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

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `matomo_user_token_auth` WRITE;
/*!40000 ALTER TABLE `matomo_user_token_auth` DISABLE KEYS */;
INSERT INTO `matomo_user_token_auth` VALUES
(1,'anonymous','anonymous default token','f99ce675f7d1caaa4b4d6fb9438a34fed5d2bbb8edaf00ca18ed818f7a63f5067ee615640098ac71220d5876ce60cd91359b400052c4c938641b597666ccf51e','sha512',0,'2026-10-12 12:47:43','2026-10-12 10:59:45',NULL,0,NULL,NULL),
(2,'admin','integration-test','eeba94698a9d860d276d048b245d27e0b88ee977b34659d652641cce9e145055155f9c7fdfbd01ad0b7b2f66e8f642e46ba5a38aaf4181dfd64ddcbf0103fde8','sha512',0,NULL,'2026-10-12 12:54:28',NULL,1,NULL,NULL);
/*!40000 ALTER TABLE `matomo_user_token_auth` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2026-10-12 12:55:55
