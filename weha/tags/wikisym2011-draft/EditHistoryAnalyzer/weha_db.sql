-- phpMyAdmin SQL Dump
-- version 3.3.2deb1
-- http://www.phpmyadmin.net
--
-- 主機: localhost
-- 建立日期: Mar 18, 2011, 04:15 PM
-- 伺服器版本: 5.1.41
-- PHP 版本: 5.3.2-1ubuntu4.7

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- 資料庫: `wikidb`
--

-- --------------------------------------------------------

--
-- 資料表格式： `weha_page`
--

CREATE TABLE IF NOT EXISTS `weha_page` (
  `page_id` int(10) NOT NULL,
  `page_analyzed_rev` int(10) NOT NULL,
  `page_total_significance` double DEFAULT NULL,
  `page_avg_significance` double DEFAULT NULL,
  PRIMARY KEY (`page_id`)
) ENGINE=InnoDB DEFAULT CHARSET=binary;

-- --------------------------------------------------------

--
-- 資料表格式： `weha_revision`
--

CREATE TABLE IF NOT EXISTS `weha_revision` (
  `weha_rev_id` int(10) unsigned NOT NULL,
  `weha_rev_page` int(10) unsigned NOT NULL,
  `weha_rev_user` int(10) unsigned NOT NULL DEFAULT '0',
  `weha_rev_user_text` varbinary(255) NOT NULL DEFAULT '',
  `weha_rev_md5` binary(16) DEFAULT NULL,
  `weha_rev_diff` mediumblob,
  `weha_rev_bdiff` mediumblob,
  `weha_rev_sdiff` mediumblob,
  `weha_rev_action_count` mediumblob,
  `weha_rev_token_count` mediumblob,
  `weha_rev_token_affected` int(11) DEFAULT NULL,
  `weha_rev_significance` double DEFAULT NULL,
  PRIMARY KEY (`weha_rev_id`),
  UNIQUE KEY `rev_page_id` (`weha_rev_page`,`weha_rev_id`),
  KEY `rev_page_md5` (`weha_rev_page`,`weha_rev_md5`),
  KEY `rev_usertext_page` (`weha_rev_user_text`,`weha_rev_page`),
  KEY `rev_page_usertext` (`weha_rev_page`,`weha_rev_user_text`)
) ENGINE=InnoDB DEFAULT CHARSET=binary;
