CREATE DATABASE  IF NOT EXISTS `mydb` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `mydb`;
-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: mydb
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `actividad`
--

DROP TABLE IF EXISTS `actividad`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `actividad` (
  `idActividad` int NOT NULL AUTO_INCREMENT,
  `idUsuario` int NOT NULL,
  `descripcion` varchar(100) NOT NULL,
  `detalle` text,
  `fecha` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`idActividad`),
  KEY `fk_Actividad_Usuarios_idx` (`idUsuario`),
  CONSTRAINT `fk_Actividad_Usuarios` FOREIGN KEY (`idUsuario`) REFERENCES `usuarios` (`idUsuarios`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=137 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `actividad`
--

LOCK TABLES `actividad` WRITE;
/*!40000 ALTER TABLE `actividad` DISABLE KEYS */;
INSERT INTO `actividad` VALUES (26,4,'Cambio de estado','Cambi√≥ el estado de \"Bal√≥n de oro\" a Ocupado','2025-06-14 13:01:05'),(27,4,'Cambio de estado','Cambi√≥ el estado de \"Bal√≥n de oro\" a Disponible','2025-06-14 13:01:15'),(28,4,'Actualizaci√≥n de Espacio','Edit√≥ la informaci√≥n del espacio \"Nuevo Espacio\" a las 13:16:24','2025-06-14 13:16:24'),(29,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Complejo Deportivo Santos','2025-06-14 15:31:47'),(30,4,'Cambio de estado','Cambi√≥ el estado de \"Complejo Deportivo Santos\" a Ocupado','2025-06-14 15:32:09'),(31,4,'Cambio de estado','Cambi√≥ el estado de \"Complejo Deportivo Santos\" a Disponible','2025-06-14 15:32:16'),(32,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de platino','2025-06-14 16:04:04'),(33,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de platino','2025-06-14 16:18:11'),(34,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de platino','2025-06-14 16:18:20'),(35,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 16:22:27'),(36,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 16:24:11'),(37,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 16:25:18'),(38,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 16:26:56'),(39,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 16:46:15'),(40,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 16:51:11'),(41,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 16:53:04'),(42,4,'Cambio de estado','Cambi√≥ el estado de \"Bal√≥n de oro\" a Disponible','2025-06-14 16:53:11'),(43,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 16:53:50'),(44,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 16:55:51'),(45,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 16:56:13'),(46,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 16:58:20'),(47,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 17:07:22'),(48,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro','2025-06-14 17:12:58'),(49,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oroo','2025-06-14 17:13:26'),(50,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oroo','2025-06-14 17:13:43'),(51,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oroo','2025-06-14 17:14:04'),(52,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oroo','2025-06-14 17:14:24'),(53,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oroo','2025-06-14 17:17:55'),(54,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oroo','2025-06-14 17:21:12'),(55,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de platino','2025-06-14 17:21:29'),(56,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de platino','2025-06-14 17:21:37'),(57,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de platino','2025-06-14 17:28:23'),(58,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo celeste','2025-06-14 17:29:45'),(59,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo celeste','2025-06-14 17:30:02'),(60,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo celeste','2025-06-14 17:33:08'),(61,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo celeste','2025-06-14 17:33:49'),(62,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo celeste','2025-06-14 17:37:02'),(63,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo celeste','2025-06-14 17:37:24'),(64,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo celeste','2025-06-14 17:37:40'),(65,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo celeste','2025-06-14 17:38:19'),(66,4,'Cambio de estado','Cambi√≥ el estado de \"Bal√≥n de oroo\" a Disponible','2025-06-14 17:38:58'),(67,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oroo','2025-06-14 17:39:15'),(68,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"prueba\" a las 17:40:22','2025-06-14 17:40:22'),(69,4,'Edici√≥n de espacio','Se edit√≥ el espacio: prueba','2025-06-14 17:40:36'),(70,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de platino','2025-06-14 22:35:24'),(71,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de platino','2025-06-14 23:08:43'),(72,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"prueba\" a las 23:17:20','2025-06-14 23:17:21'),(73,4,'Edici√≥n de espacio','Se edit√≥ el espacio: prueba','2025-06-14 23:18:07'),(74,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"afsdf\" a las 23:31:16','2025-06-14 23:31:16'),(75,4,'Edici√≥n de espacio','Se edit√≥ el espacio: afsdf','2025-06-14 23:32:01'),(76,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oroo','2025-06-19 20:06:31'),(77,2,'Cambio de estado','El espacio \"Bal√≥n de oroo\" fue marcado como \"Ocupado\".','2025-06-28 14:36:38'),(78,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oroo','2025-07-02 14:04:41'),(79,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"Prueba\" a las 14:12:33','2025-07-02 14:12:33'),(80,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"Prueba\" a las 14:12:34','2025-07-02 14:12:34'),(81,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Nuevo Espacio','2025-07-02 14:25:33'),(82,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"Prueba Espacio Nuevo\" a las 14:26:29','2025-07-02 14:26:30'),(83,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"Otra prueba\" a las 14:40:02','2025-07-02 14:40:02'),(84,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"dhfgh\" a las 15:00:34','2025-07-02 15:00:34'),(85,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"fgdfhn\" a las 15:05:34','2025-07-02 15:05:35'),(86,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"otra\" a las 15:08:10','2025-07-02 15:08:10'),(87,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"last one\" a las 15:09:08','2025-07-02 15:09:08'),(88,4,'Edici√≥n de espacio','Se edit√≥ el espacio: last one','2025-07-02 15:32:51'),(89,4,'Edici√≥n de espacio','Se edit√≥ el espacio: last one','2025-07-02 15:41:25'),(90,4,'Edici√≥n de espacio','Se edit√≥ el espacio: last one','2025-07-02 15:41:51'),(91,4,'Edici√≥n de espacio','Se edit√≥ el espacio: last one','2025-07-02 15:54:18'),(92,4,'Edici√≥n de espacio','Se edit√≥ el espacio: last one','2025-07-02 21:27:48'),(93,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"gdfg23\" a las 21:28:45','2025-07-02 21:28:46'),(94,2,'Cambio de estado de espacio','Se cambi√≥ el estado del espacio \"Bal√≥n de oroo\" a \"Mantenimiento\".','2025-07-18 19:36:54'),(95,2,'Cambio de estado de espacio','Se cambi√≥ el estado del espacio \"gdfg23\" a \"Mantenimiento\".','2025-07-18 19:38:50'),(96,2,'Cambio de estado de espacio','Se cambi√≥ el estado del espacio \"Bal√≥n de oroo\" a \"Disponible\".','2025-07-18 19:38:55'),(97,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Bal√≥n de oroo\".','2025-07-18 21:31:31'),(98,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Bal√≥n de oroo\".','2025-07-18 21:32:10'),(99,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Campo celeste\".','2025-07-18 22:42:51'),(100,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Complejo Deportivo Santos\".','2025-07-18 22:45:50'),(101,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Bal√≥n de platino\".','2025-07-18 22:48:17'),(102,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Bal√≥n de platino\".','2025-07-18 22:58:21'),(103,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Bal√≥n de oroo\".','2025-07-18 23:19:16'),(104,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Bal√≥n de platino\".','2025-07-18 23:20:14'),(105,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Complejo Deportivo Santos\".','2025-07-18 23:20:24'),(106,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Bal√≥n de platino\".','2025-07-18 23:30:52'),(107,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Campo naranja\".','2025-07-18 23:37:42'),(108,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Complejo Deportivo Santos\".','2025-07-18 23:39:40'),(109,2,'Cambio de estado de espacio','Se cambi√≥ el estado del espacio \"Bal√≥n de oroo\" a \"Mantenimiento\".','2025-07-18 23:40:26'),(110,2,'Cambio de estado de espacio','Se cambi√≥ el estado del espacio \"Bal√≥n de oroo\" a \"Disponible\".','2025-07-18 23:40:28'),(111,2,'Agreg√≥ una observaci√≥n','Se a√±adi√≥ una observaci√≥n al espacio \"Bal√≥n de oroo\".','2025-07-19 14:18:56'),(112,2,'Inici√≥ una Asistencia','Se registr√≥ la hora de entrada a las 15:20:15.824299600 en \"Mi Casa\". Asistencia en curso.','2025-07-22 15:20:16'),(113,2,'Registr√≥ una Asistencia','Se registr√≥ la hora de salida a las 15:25:53.280124300 en \"Mi Casa\". Asistencia completada.','2025-07-22 15:25:53'),(114,2,'Inici√≥ una Asistencia','Se registr√≥ la hora de entrada a las 15:29:40.295935400 en \"Mi Casa\". Asistencia en curso.','2025-07-22 15:29:40'),(115,2,'Registr√≥ una Asistencia','Se registr√≥ la hora de salida a las 15:29:40 en \"Mi Casa\". Asistencia completada.','2025-07-22 18:24:22'),(116,2,'Cambio de estado de espacio','Se cambi√≥ el estado del espacio \"Bal√≥n de oroo\" a \"Mantenimiento\".','2025-07-22 18:48:11'),(117,2,'Cambio de estado de espacio','Se cambi√≥ el estado del espacio \"Bal√≥n de oroo\" a \"Disponible\".','2025-07-22 18:48:19'),(118,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"Cancha Minas PUCP\" a las 11:55:53','2025-07-23 11:55:54'),(119,4,'Creaci√≥n de espacio','Cre√≥ el espacio \"Mi casa\" a las 12:23:37','2025-07-23 12:23:38'),(120,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Cancha Minas PUCP a las 13:37:51','2025-07-23 13:37:52'),(121,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de oro a las 13:41:55','2025-07-28 13:41:56'),(122,2,'Cambio de estado de espacio','Se cambi√≥ el estado del espacio \"gdfg23\" a \"Disponible\".','2025-07-30 20:37:01'),(123,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Bal√≥n de platino a las 11:26:16','2025-07-31 11:26:17'),(124,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo Azul a las 11:26:42','2025-07-31 11:26:43'),(125,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo celeste a las 11:27:00','2025-07-31 11:27:00'),(126,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo naranja a las 11:27:27','2025-07-31 11:27:28'),(127,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Complejo Deportivo Santos a las 11:28:05','2025-07-31 11:28:05'),(128,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo Deportivo Santos a las 11:29:01','2025-07-31 11:29:01'),(129,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Campo de F√∫tbol a las 11:30:13','2025-07-31 11:30:14'),(130,4,'Edici√≥n de espacio','Se edit√≥ el espacio: Piscina Nueva a las 11:31:30','2025-07-31 11:31:30'),(131,2,'Inici√≥ una Asistencia','Se registr√≥ la hora de ENTRADA a las 11:47:59 en el lugar \"Mi Casa\". Asistencia en curso.','2025-07-31 11:48:00'),(132,2,'Registr√≥ una Asistencia','Se registr√≥ la hora de SALIDA a las 11:48:00 en \"Mi Casa\". Asistencia completada.','2025-07-31 11:48:12'),(133,2,'Inici√≥ una Asistencia','Se registr√≥ la hora de ENTRADA a las 17:26:25 en el lugar \"Complejo deportivo Tierra\". Asistencia en curso.','2025-07-31 17:26:26'),(134,2,'Registr√≥ una Asistencia','Se registr√≥ la hora de SALIDA a las 17:26:26 en \"Complejo deportivo Tierra\". Asistencia completada.','2025-07-31 17:43:15'),(135,2,'Inici√≥ una Asistencia','Se registr√≥ la hora de ENTRADA a las 17:50:27 en el lugar \"Complejo deportivo Tierra\". Asistencia en curso.','2025-07-31 17:50:28'),(136,2,'Registr√≥ una Asistencia','Se registr√≥ la hora de SALIDA a las 17:50:27 en \"Complejo deportivo Tierra\". Asistencia completada.','2025-07-31 17:50:38');
/*!40000 ALTER TABLE `actividad` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `calificacion`
--

DROP TABLE IF EXISTS `calificacion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `calificacion` (
  `idCalificacion` int NOT NULL AUTO_INCREMENT,
  `idEspacio` int NOT NULL,
  `idReserva` int NOT NULL,
  `idVecino` int NOT NULL,
  `puntaje` decimal(2,1) NOT NULL,
  `comentario` varchar(300) DEFAULT NULL,
  `fechaCalificacion` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`idCalificacion`),
  UNIQUE KEY `idReserva` (`idReserva`),
  KEY `fk_calificacion_espacio` (`idEspacio`),
  KEY `fk_calificacion_vecino` (`idVecino`),
  CONSTRAINT `fk_calificacion_espacio` FOREIGN KEY (`idEspacio`) REFERENCES `espacio` (`idEspacio`),
  CONSTRAINT `fk_calificacion_reserva` FOREIGN KEY (`idReserva`) REFERENCES `reserva` (`idReserva`),
  CONSTRAINT `fk_calificacion_vecino` FOREIGN KEY (`idVecino`) REFERENCES `usuarios` (`idUsuarios`),
  CONSTRAINT `calificacion_chk_1` CHECK (((`puntaje` >= 1.0) and (`puntaje` <= 5.0)))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `calificacion`
--

LOCK TABLES `calificacion` WRITE;
/*!40000 ALTER TABLE `calificacion` DISABLE KEYS */;
/*!40000 ALTER TABLE `calificacion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_mensaje`
--

DROP TABLE IF EXISTS `chat_mensaje`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_mensaje` (
  `id` int NOT NULL AUTO_INCREMENT,
  `usuario_id` int NOT NULL,
  `id_conversacion` varchar(100) DEFAULT NULL,
  `rol` enum('USUARIO','BOT') NOT NULL,
  `contenido` text NOT NULL,
  `fecha` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `usuario_id` (`usuario_id`),
  CONSTRAINT `chat_mensaje_ibfk_1` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`idUsuarios`)
) ENGINE=InnoDB AUTO_INCREMENT=143 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_mensaje`
--

LOCK TABLES `chat_mensaje` WRITE;
/*!40000 ALTER TABLE `chat_mensaje` DISABLE KEYS */;
INSERT INTO `chat_mensaje` VALUES (1,7,'b96a481a-c6f6-4f86-a8d6-78516e4c370b','USUARIO','Quiero ver mis reservas','2025-07-04 16:27:16'),(2,7,'b96a481a-c6f6-4f86-a8d6-78516e4c370b','BOT','<div style=\'text-align: center; padding: 20px;\'><div style=\'font-size: 2rem; margin-bottom: 10px;\'>?</div><div style=\'font-weight: 600; color: #6c757d; margin-bottom: 10px;\'>No tienes reservas activas</div><div style=\'color: #6c757d;\'>¬øTe gustar√≠a hacer una nueva reserva? ?</div></div>','2025-07-04 16:27:19'),(3,7,'b4ebfa63-de78-48a3-9bb7-28a23e37c772','USUARIO','Quiero ver mis reservas','2025-07-04 23:29:05'),(4,7,'b4ebfa63-de78-48a3-9bb7-28a23e37c772','BOT','<div style=\'text-align: center; padding: 20px;\'><div style=\'font-size: 2rem; margin-bottom: 10px;\'>?</div><div style=\'font-weight: 600; color: #6c757d; margin-bottom: 10px;\'>No tienes reservas activas</div><div style=\'color: #6c757d;\'>¬øTe gustar√≠a hacer una nueva reserva? ?</div></div>','2025-07-04 23:29:08'),(5,7,'292cc34a-12e0-45a9-8b17-9111c709d2ce','USUARIO','Quiero ver mis reservas','2025-07-19 19:13:49'),(6,7,'292cc34a-12e0-45a9-8b17-9111c709d2ce','BOT','<div class=\'reservas-lista\'><div style=\'font-weight: 600; color: #495057; margin-bottom: 15px; display: flex; align-items: center;\'><span style=\'margin-right: 8px;\'>?</span>Tus reservas activas:</div><div style=\'background: #f8f9fa; border-left: 4px solid #28a745; border-radius: 8px; padding: 12px; margin-bottom: 12px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\'><div style=\'display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;\'><span style=\'font-weight: 600; color: #495057;\'>Reserva #1</span><span style=\'background: #28a745; color: white; padding: 4px 8px; border-radius: 12px; font-size: 0.85rem;\'>‚úÖ Confirmada</span></div><div style=\'display: grid; grid-template-columns: 1fr 1fr; gap: 8px; color: #6c757d; font-size: 0.9rem;\'><div><strong>? ID:</strong> 41</div><div><strong>? Lugar:</strong> Bal√≥n de oroo</div><div><strong>? Fecha:</strong> 2025-07-19</div><div><strong>? Hora:</strong> 19:00 - 21:00</div><div><strong>? Costo:</strong> S/25.00</div><div><strong>? Pago:</strong> En l√≠nea</div></div></div></div><div style=\'text-align: center; margin-top: 15px; color: #6c757d;\'>¬øEn qu√© m√°s puedo ayudarte? ?</div>','2025-07-19 19:13:52'),(7,7,'9fde6aac-fd40-4f09-9a92-156caea29b2b','USUARIO','Quiero ver mis reservas','2025-07-20 21:17:45'),(8,7,'9fde6aac-fd40-4f09-9a92-156caea29b2b','BOT','<div class=\'reservas-lista\'><div style=\'font-weight: 600; color: #495057; margin-bottom: 15px; display: flex; align-items: center;\'><span style=\'margin-right: 8px;\'>?</span>Tus reservas activas:</div><div style=\'background: #f8f9fa; border-left: 4px solid #ffc107; border-radius: 8px; padding: 12px; margin-bottom: 12px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\'><div style=\'display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;\'><span style=\'font-weight: 600; color: #495057;\'>Reserva #1</span><span style=\'background: #ffc107; color: white; padding: 4px 8px; border-radius: 12px; font-size: 0.85rem;\'>‚è≥ Pendiente</span></div><div style=\'display: grid; grid-template-columns: 1fr 1fr; gap: 8px; color: #6c757d; font-size: 0.9rem;\'><div><strong>? ID:</strong> 43</div><div><strong>? Lugar:</strong> Campo celeste</div><div><strong>? Fecha:</strong> 2025-07-21</div><div><strong>? Hora:</strong> 10:00 - 11:00</div><div><strong>? Costo:</strong> S/32.50</div><div><strong>? Pago:</strong> En banco</div></div></div></div><div style=\'text-align: center; margin-top: 15px; color: #6c757d;\'>¬øEn qu√© m√°s puedo ayudarte? ?</div>','2025-07-20 21:17:48'),(9,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','USUARIO','Ver lugares disponibles','2025-07-21 00:58:58'),(10,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','BOT','<div class=\'lugares-lista\'><div style=\'font-weight: 600; color: #495057; margin-bottom: 15px; display: flex; align-items: center;\'><span style=\'margin-right: 8px;\'>?</span>Lugares disponibles:</div><div style=\'background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;\'><strong>1. Complejo deportivo Sol</strong></div><div style=\'background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;\'><strong>2. Complejo deportivo Luna</strong></div><div style=\'background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;\'><strong>3. Complejo deportivo Marte</strong></div><div style=\'background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;\'><strong>4. Complejo deportivo Saturno</strong></div><div style=\'background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;\'><strong>5. Complejo deportivo Urano</strong></div><div style=\'background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;\'><strong>6. Complejo deportivo Tierra</strong></div></div><div style=\'text-align: center; margin-top: 15px; color: #6c757d;\'>¬øEn qu√© m√°s puedo ayudarte? ?</div>','2025-07-21 00:59:00'),(11,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','USUARIO','Quiero ver mis reservas','2025-07-21 01:05:34'),(12,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','BOT','<div class=\'reservas-lista\'><div style=\'font-weight: 600; color: #495057; margin-bottom: 15px; display: flex; align-items: center;\'><span style=\'margin-right: 8px;\'>?</span>Tus reservas activas:</div><div style=\'background: #f8f9fa; border-left: 4px solid #ffc107; border-radius: 8px; padding: 12px; margin-bottom: 12px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\'><div style=\'display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;\'><span style=\'font-weight: 600; color: #495057;\'>Reserva #1</span><span style=\'background: #ffc107; color: white; padding: 4px 8px; border-radius: 12px; font-size: 0.85rem;\'>‚è≥ Pendiente</span></div><div style=\'display: grid; grid-template-columns: 1fr 1fr; gap: 8px; color: #6c757d; font-size: 0.9rem;\'><div><strong>? ID:</strong> 43</div><div><strong>? Lugar:</strong> Campo celeste</div><div><strong>? Fecha:</strong> 2025-07-21</div><div><strong>? Hora:</strong> 10:00 - 11:00</div><div><strong>? Costo:</strong> S/32.50</div><div><strong>? Pago:</strong> En banco</div></div></div></div><div style=\'text-align: center; margin-top: 15px; color: #6c757d;\'>¬øEn qu√© m√°s puedo ayudarte? ?</div>','2025-07-21 01:05:35'),(13,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','USUARIO','Quiero hacer una reserva','2025-07-21 01:08:54'),(14,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','BOT','¬øEn qu√© lugar te gustar√≠a consultar disponibilidad? ?Ô∏è\n\n? Lugares disponibles:','2025-07-21 01:08:55'),(15,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','USUARIO','Complejo deportivo Marte','2025-07-21 01:09:02'),(16,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','BOT','No se encontraron espacios en Complejo deportivo Marte','2025-07-21 01:09:02'),(17,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','USUARIO','Complejo deportivo Tierra','2025-07-21 01:09:11'),(18,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','BOT','Lo siento, no entend√≠ tu solicitud ?','2025-07-21 01:09:12'),(19,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','USUARIO','Quiero hacer una reserva','2025-07-21 01:09:17'),(20,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','BOT','¬øEn qu√© lugar te gustar√≠a consultar disponibilidad? ?Ô∏è\n\n? Lugares disponibles:','2025-07-21 01:09:19'),(21,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','USUARIO','Complejo deportivo Sol','2025-07-21 01:09:24'),(22,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','BOT','¬øQu√© espacio deportivo dentro de Complejo deportivo Sol deseas consultar?','2025-07-21 01:09:24'),(23,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','USUARIO','Bal√≥n de oroo','2025-07-21 01:09:36'),(24,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','BOT','¬øPara qu√© fecha deseas consultar la disponibilidad? ? (Ej: 2024-12-25, hoy, ma√±ana, etc.)','2025-07-21 01:09:36'),(25,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','USUARIO','ya no','2025-07-21 01:09:46'),(26,7,'6dd631cf-21a5-4be5-a1e9-e6bf58e19c7b','BOT','‚úÖ Proceso cancelado. ¬øEn qu√© m√°s puedo ayudarte?','2025-07-21 01:09:46'),(27,7,'20a7f50a-309f-423f-a8db-384eefc02dd2','USUARIO','Quiero ver mis reservas','2025-07-21 07:47:08'),(28,7,'20a7f50a-309f-423f-a8db-384eefc02dd2','BOT','<div class=\'reservas-lista\'><div style=\'font-weight: 600; color: #495057; margin-bottom: 15px; display: flex; align-items: center;\'><span style=\'margin-right: 8px;\'>?</span>Tus reservas activas:</div><div style=\'background: #f8f9fa; border-left: 4px solid #ffc107; border-radius: 8px; padding: 12px; margin-bottom: 12px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\'><div style=\'display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;\'><span style=\'font-weight: 600; color: #495057;\'>Reserva #1</span><span style=\'background: #ffc107; color: white; padding: 4px 8px; border-radius: 12px; font-size: 0.85rem;\'>‚è≥ Pendiente</span></div><div style=\'display: grid; grid-template-columns: 1fr 1fr; gap: 8px; color: #6c757d; font-size: 0.9rem;\'><div><strong>? ID:</strong> 43</div><div><strong>? Lugar:</strong> Campo celeste</div><div><strong>? Fecha:</strong> 2025-07-21</div><div><strong>? Hora:</strong> 10:00 - 11:00</div><div><strong>? Costo:</strong> S/32.50</div><div><strong>? Pago:</strong> En banco</div></div></div></div><div style=\'text-align: center; margin-top: 15px; color: #6c757d;\'>¬øEn qu√© m√°s puedo ayudarte? ?</div>','2025-07-21 07:47:11'),(29,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','necesito ayuda','2025-07-21 15:32:25'),(30,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','Lo siento, no entend√≠ tu solicitud ?','2025-07-21 15:32:29'),(31,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Quiero hacer una reserva','2025-07-21 17:48:01'),(32,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øEn qu√© lugar te gustar√≠a consultar disponibilidad? ?Ô∏è\n\n? Lugares disponibles:','2025-07-21 17:48:04'),(33,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Complejo deportivo Sol','2025-07-21 17:48:10'),(34,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øQu√© espacio deportivo dentro de Complejo deportivo Sol deseas consultar?','2025-07-21 17:48:10'),(35,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Bal√≥n de oroo','2025-07-21 17:48:15'),(36,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øPara qu√© fecha deseas consultar la disponibilidad? ? (Ej: 2024-12-25, hoy, ma√±ana, etc.)','2025-07-21 17:48:15'),(37,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','ma√±ana','2025-07-21 17:48:21'),(38,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øQu√© horario deseas consultar? ‚è∞ Indica la hora de inicio y fin (Ej: de 9 a 11, 14-16)','2025-07-21 17:48:21'),(39,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','de 9 am a 10 am','2025-07-21 17:48:30'),(40,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','<div class=\'disponibilidad-resultado\'><div>‚úÖ <strong>¬°Espacio disponible!</strong></div><div>? Lugar: Complejo deportivo Sol</div><div>? Fecha: 2025-07-22</div><div>? Horario: 9:00 - 10:00</div><div>? Costo estimado: S/50.00</div></div><div style=\'margin-top:10px;\'>¬øDeseas crear la reserva con estos datos?</div>','2025-07-21 17:48:30'),(41,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','s√≠','2025-07-21 17:48:36'),(42,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øC√≥mo deseas pagar? ? Elige tu m√©todo de pago:','2025-07-21 17:48:36'),(43,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','En l√≠nea','2025-07-21 17:48:43'),(44,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','<div class=\'reserva-resumen\'><div style=\'font-weight:600;\'>? Resumen de tu reserva:</div><div>? Lugar: Complejo deportivo Sol</div><div>? Fecha: 2025-07-22</div><div>? Horario: 9:00 - 10:00</div><div>? Pago: En l√≠nea</div><div>? Costo estimado: S/50.00</div></div><div style=\'margin-top:10px;\'>¬øConfirmas que deseas crear esta reserva?</div>','2025-07-21 17:48:43'),(45,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','s√≠','2025-07-21 17:48:49'),(46,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','? <a href=\'https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=2514527579-f1cae75b-c195-46f2-8774-dc1c6d6ca0d5\' target=\'_blank\' class=\'btn btn-primary\'>Ir a MercadoPago para completar el pago</a>','2025-07-21 17:48:50'),(47,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Quiero ver mis reservas','2025-07-21 17:49:37'),(48,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','<div style=\'text-align: center; padding: 20px;\'><div style=\'font-size: 2rem; margin-bottom: 10px;\'>?</div><div style=\'font-weight: 600; color: #6c757d; margin-bottom: 10px;\'>No tienes reservas activas</div><div style=\'color: #6c757d;\'>¬øTe gustar√≠a hacer una nueva reserva? ?</div></div>','2025-07-21 17:49:39'),(49,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Quiero hacer una reserva','2025-07-21 17:57:30'),(50,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øEn qu√© lugar te gustar√≠a consultar disponibilidad? ?Ô∏è\n\n? Lugares disponibles:','2025-07-21 17:57:32'),(51,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','ya no','2025-07-21 18:05:07'),(52,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','‚úÖ Proceso cancelado. ¬øEn qu√© m√°s puedo ayudarte?','2025-07-21 18:05:07'),(53,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Quiero hacer una reserva','2025-07-21 18:05:21'),(54,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øEn qu√© lugar te gustar√≠a consultar disponibilidad? ?Ô∏è\n\n? Lugares disponibles:','2025-07-21 18:05:22'),(55,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Complejo deportivo Marte','2025-07-21 18:05:28'),(56,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','No se encontraron espacios en Complejo deportivo Marte','2025-07-21 18:05:28'),(57,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Quiero hacer una reserva','2025-07-21 18:05:34'),(58,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øEn qu√© lugar te gustar√≠a consultar disponibilidad? ?Ô∏è\n\n? Lugares disponibles:','2025-07-21 18:05:35'),(59,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Complejo deportivo Urano','2025-07-21 18:05:41'),(60,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øQu√© espacio deportivo dentro de Complejo deportivo Urano deseas consultar?','2025-07-21 18:05:41'),(61,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Complejo Deportivo Santos','2025-07-21 18:05:45'),(62,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øPara qu√© fecha deseas consultar la disponibilidad? ? (Ej: 2024-12-25, hoy, ma√±ana, etc.)','2025-07-21 18:05:45'),(63,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','2025-07-23','2025-07-21 18:06:00'),(64,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øQu√© horario deseas consultar? ‚è∞ Indica la hora de inicio y fin (Ej: de 9 a 11, 14-16)','2025-07-21 18:06:00'),(65,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','14 a 16','2025-07-21 18:06:14'),(66,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','<div class=\'disponibilidad-resultado\'><div>‚úÖ <strong>¬°Espacio disponible!</strong></div><div>? Lugar: Complejo deportivo Urano</div><div>? Fecha: 2025-07-23</div><div>? Horario: 14:00 - 16:00</div><div>? Costo estimado: S/40.00</div></div><div style=\'margin-top:10px;\'>¬øDeseas crear la reserva con estos datos?</div>','2025-07-21 18:06:14'),(67,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','S√≠, reservar','2025-07-21 18:06:42'),(68,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øC√≥mo deseas pagar? ? Elige tu m√©todo de pago:','2025-07-21 18:06:42'),(69,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','En banco','2025-07-21 18:06:46'),(70,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','<div class=\'reserva-resumen\'><div style=\'font-weight:600;\'>? Resumen de tu reserva:</div><div>? Lugar: Complejo deportivo Urano</div><div>? Fecha: 2025-07-23</div><div>? Horario: 14:00 - 16:00</div><div>? Pago: En banco</div><div>? Costo estimado: S/40.00</div></div><div style=\'margin-top:10px;\'>¬øConfirmas que deseas crear esta reserva?</div>','2025-07-21 18:06:46'),(71,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','S√≠, confirmar','2025-07-21 18:07:00'),(72,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','<div class=\'reserva-final\'><div style=\'font-weight:600;\'>‚úÖ ¬°Reserva registrada exitosamente!</div><div>? ID Reserva: 44</div><div>? Espacio: Complejo Deportivo Santos</div><div>? Fecha: 2025-07-23</div><div>? Hora: 14:00 - 16:00</div><div>? Total: S/40.00</div><div>? Pago: En banco</div></div><div style=\'margin-top:10px;\'>¬øEn qu√© m√°s puedo ayudarte?</div>','2025-07-21 18:07:01'),(73,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Quiero ver mis reservas','2025-07-21 18:17:27'),(74,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','<div class=\'reservas-lista\'><div style=\'font-weight: 600; color: #495057; margin-bottom: 15px; display: flex; align-items: center;\'><span style=\'margin-right: 8px;\'>?</span>Tus reservas activas:</div><div style=\'background: #f8f9fa; border-left: 4px solid #28a745; border-radius: 8px; padding: 12px; margin-bottom: 12px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\'><div style=\'display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;\'><span style=\'font-weight: 600; color: #495057;\'>Reserva #1</span><span style=\'background: #28a745; color: white; padding: 4px 8px; border-radius: 12px; font-size: 0.85rem;\'>‚úÖ Confirmada</span></div><div style=\'display: grid; grid-template-columns: 1fr 1fr; gap: 8px; color: #6c757d; font-size: 0.9rem;\'><div><strong>? ID:</strong> 44</div><div><strong>? Lugar:</strong> Complejo Deportivo Santos</div><div><strong>? Fecha:</strong> 2025-07-23</div><div><strong>? Hora:</strong> 14:00 - 16:00</div><div><strong>? Costo:</strong> S/40.00</div><div><strong>? Pago:</strong> En banco</div></div></div></div><div style=\'text-align: center; margin-top: 15px; color: #6c757d;\'>¬øEn qu√© m√°s puedo ayudarte? ?</div>','2025-07-21 18:17:29'),(75,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Quiero hacer una reserva','2025-07-21 18:17:35'),(76,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øEn qu√© lugar te gustar√≠a consultar disponibilidad? ?Ô∏è\n\n? Lugares disponibles:','2025-07-21 18:17:37'),(77,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Complejo deportivo Saturno','2025-07-21 18:17:42'),(78,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øQu√© espacio deportivo dentro de Complejo deportivo Saturno deseas consultar?','2025-07-21 18:17:42'),(79,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Campo naranja','2025-07-21 18:17:46'),(80,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øPara qu√© fecha deseas consultar la disponibilidad? ? (Ej: 2024-12-25, hoy, ma√±ana, etc.)','2025-07-21 18:17:46'),(81,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Ma√±ana','2025-07-21 18:17:52'),(82,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øQu√© horario deseas consultar? ‚è∞ Indica la hora de inicio y fin (Ej: de 9 a 11, 14-16)','2025-07-21 18:17:52'),(83,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','18 a 20','2025-07-21 18:18:15'),(84,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','<div class=\'disponibilidad-resultado\'><div>‚úÖ <strong>¬°Espacio disponible!</strong></div><div>? Lugar: Complejo deportivo Saturno</div><div>? Fecha: 2025-07-22</div><div>? Horario: 18:00 - 20:00</div><div>? Costo estimado: S/60.00</div></div><div style=\'margin-top:10px;\'>¬øDeseas crear la reserva con estos datos?</div>','2025-07-21 18:18:15'),(85,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','S√≠, reservar','2025-07-21 18:18:28'),(86,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','¬øC√≥mo deseas pagar? ? Elige tu m√©todo de pago:','2025-07-21 18:18:28'),(87,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','En banco','2025-07-21 18:18:31'),(88,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','<div class=\'reserva-resumen\'><div style=\'font-weight:600;\'>? Resumen de tu reserva:</div><div>? Lugar: Complejo deportivo Saturno</div><div>? Fecha: 2025-07-22</div><div>? Horario: 18:00 - 20:00</div><div>? Pago: En banco</div><div>? Costo estimado: S/60.00</div></div><div style=\'margin-top:10px;\'>¬øConfirmas que deseas crear esta reserva?</div>','2025-07-21 18:18:31'),(89,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','S√≠, confirmar','2025-07-21 18:18:39'),(90,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','<div class=\'reserva-final\'><div style=\'font-weight:600;\'>‚è≥ ¬°Reserva registrada exitosamente!</div><div style=\'color: #f39c12; font-weight: 500; margin: 10px 0;\'>‚ö†Ô∏è Pendiente de pago en banco</div><div>? ID Reserva: 45</div><div>? Espacio: Campo naranja</div><div>? Fecha: 2025-07-22</div><div>? Hora: 18:00 - 20:00</div><div>? Total a pagar: S/60.00</div><div>? Pago: En banco</div></div><div style=\'margin: 15px 0; padding: 10px; background-color: #fff3cd; border: 1px solid #ffeaa7; border-radius: 5px;\'><div style=\'font-weight: 600; color: #856404;\'>? Instrucciones de pago:</div><div style=\'color: #856404;\'>1. Realiza el pago en banco por el monto indicado</div><div style=\'color: #856404;\'>2. Sube el comprobante en tu boleta</div><div style=\'color: #856404;\'>3. Espera la confirmaci√≥n del administrador</div></div>','2025-07-21 18:18:39'),(91,7,'e7839516-2125-41d6-ac59-7dc9cade0108','USUARIO','Quiero ver mis reservas','2025-07-21 18:18:47'),(92,7,'e7839516-2125-41d6-ac59-7dc9cade0108','BOT','<div class=\'reservas-lista\'><div style=\'font-weight: 600; color: #495057; margin-bottom: 15px; display: flex; align-items: center;\'><span style=\'margin-right: 8px;\'>?</span>Tus reservas activas:</div><div style=\'background: #f8f9fa; border-left: 4px solid #ffc107; border-radius: 8px; padding: 12px; margin-bottom: 12px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\'><div style=\'display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;\'><span style=\'font-weight: 600; color: #495057;\'>Reserva #1</span><span style=\'background: #ffc107; color: white; padding: 4px 8px; border-radius: 12px; font-size: 0.85rem;\'>‚è≥ Pendiente</span></div><div style=\'display: grid; grid-template-columns: 1fr 1fr; gap: 8px; color: #6c757d; font-size: 0.9rem;\'><div><strong>? ID:</strong> 45</div><div><strong>? Lugar:</strong> Campo naranja</div><div><strong>? Fecha:</strong> 2025-07-22</div><div><strong>? Hora:</strong> 18:00 - 20:00</div><div><strong>? Costo:</strong> S/60.00</div><div><strong>? Pago:</strong> En banco</div></div></div><div style=\'background: #f8f9fa; border-left: 4px solid #28a745; border-radius: 8px; padding: 12px; margin-bottom: 12px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\'><div style=\'display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;\'><span style=\'font-weight: 600; color: #495057;\'>Reserva #2</span><span style=\'background: #28a745; color: white; padding: 4px 8px; border-radius: 12px; font-size: 0.85rem;\'>‚úÖ Confirmada</span></div><div style=\'display: grid; grid-template-columns: 1fr 1fr; gap: 8px; color: #6c757d; font-size: 0.9rem;\'><div><strong>? ID:</strong> 44</div><div><strong>? Lugar:</strong> Complejo Deportivo Santos</div><div><strong>? Fecha:</strong> 2025-07-23</div><div><strong>? Hora:</strong> 14:00 - 16:00</div><div><strong>? Costo:</strong> S/40.00</div><div><strong>? Pago:</strong> En banco</div></div></div></div><div style=\'text-align: center; margin-top: 15px; color: #6c757d;\'>¬øEn qu√© m√°s puedo ayudarte? ?</div>','2025-07-21 18:18:48'),(93,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','USUARIO','Quiero ver mis reservas','2025-07-22 00:24:06'),(94,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','BOT','<div style=\'text-align: center; padding: 20px;\'><div style=\'font-size: 2rem; margin-bottom: 10px;\'>?</div><div style=\'font-weight: 600; color: #6c757d; margin-bottom: 10px;\'>No tienes reservas activas</div><div style=\'color: #6c757d;\'>¬øTe gustar√≠a hacer una nueva reserva? ?</div></div>','2025-07-22 00:24:09'),(95,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','USUARIO','Quiero ver los lugares','2025-07-22 00:24:14'),(96,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','BOT','<div class=\'lugares-lista\'><div style=\'font-weight: 600; color: #495057; margin-bottom: 15px; display: flex; align-items: center;\'><span style=\'margin-right: 8px;\'>?</span>Lugares disponibles:</div><div style=\'background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;\'><strong>1. Complejo deportivo Sol</strong></div><div style=\'background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;\'><strong>2. Complejo deportivo Luna</strong></div><div style=\'background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;\'><strong>3. Complejo deportivo Marte</strong></div><div style=\'background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;\'><strong>4. Complejo deportivo Saturno</strong></div><div style=\'background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;\'><strong>5. Complejo deportivo Urano</strong></div><div style=\'background: #f8f9fa; border-left: 4px solid #0d6efd; border-radius: 8px; padding: 8px; margin-bottom: 8px;\'><strong>6. Complejo deportivo Tierra</strong></div></div><div style=\'text-align: center; margin-top: 15px; color: #6c757d;\'>¬øEn qu√© m√°s puedo ayudarte? ?</div>','2025-07-22 00:24:16'),(97,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','USUARIO','Quiero hacer una reserva','2025-07-22 00:40:55'),(98,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','BOT','¬øEn qu√© lugar te gustar√≠a consultar disponibilidad? ?Ô∏è\n\n? Lugares disponibles:','2025-07-22 00:40:57'),(99,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','USUARIO','Complejo deportivo Tierra','2025-07-22 00:41:03'),(100,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','BOT','¬øQu√© espacio deportivo dentro de Complejo deportivo Tierra deseas consultar?','2025-07-22 00:41:03'),(101,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','USUARIO','Campo Azul','2025-07-22 00:41:07'),(102,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','BOT','¬øPara qu√© fecha deseas consultar la disponibilidad? ? (Ej: 2024-12-25, hoy, ma√±ana, etc.)','2025-07-22 00:41:08'),(103,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','USUARIO','Hoy','2025-07-22 00:41:18'),(104,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','BOT','¬øQu√© horario deseas consultar? ‚è∞ Indica la hora de inicio y fin (Ej: de 9 a 11, 14-16)','2025-07-22 00:41:18'),(105,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','USUARIO','9 a 11','2025-07-22 00:41:30'),(106,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','BOT','¬øQu√© horario deseas consultar? ‚è∞ Indica la hora de inicio y fin (Ej: de 9 a 11, 14-16)','2025-07-22 00:41:30'),(107,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','USUARIO','9 am a 11 am','2025-07-22 00:41:41'),(108,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','BOT','La hora de inicio ya pas√≥. Indica un nuevo horario v√°lido.','2025-07-22 00:41:41'),(109,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','USUARIO','9 pm a 11 pm','2025-07-22 00:41:47'),(110,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','BOT','<div class=\'disponibilidad-resultado\'><div>‚úÖ <strong>¬°Espacio disponible!</strong></div><div>? Lugar: Complejo deportivo Tierra</div><div>? Fecha: 2025-07-21</div><div>? Horario: 21:00 - 23:00</div><div>? Costo estimado: S/40.00</div></div><div style=\'margin-top:10px;\'>¬øDeseas crear la reserva con estos datos?</div>','2025-07-22 00:41:48'),(111,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','USUARIO','S√≠, reservar','2025-07-22 00:41:56'),(112,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','BOT','¬øC√≥mo deseas pagar? ? Elige tu m√©todo de pago:','2025-07-22 00:41:56'),(113,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','USUARIO','En banco','2025-07-22 00:42:00'),(114,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','BOT','<div class=\'reserva-resumen\'><div style=\'font-weight:600;\'>? Resumen de tu reserva:</div><div>? Lugar: Complejo deportivo Tierra</div><div>? Fecha: 2025-07-21</div><div>? Horario: 21:00 - 23:00</div><div>? Pago: En banco</div><div>? Costo estimado: S/40.00</div></div><div style=\'margin-top:10px;\'>¬øConfirmas que deseas crear esta reserva?</div>','2025-07-22 00:42:00'),(115,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','USUARIO','s√≠','2025-07-22 00:42:04'),(116,7,'8bce33a7-171f-43d4-b125-a7dc531c09bb','BOT','<div class=\'reserva-final\'><div style=\'font-weight:600;\'>‚è≥ ¬°Reserva registrada exitosamente!</div><div style=\'color: #f39c12; font-weight: 500; margin: 10px 0;\'>‚ö†Ô∏è Pendiente de pago en banco</div><div>? ID Reserva: 48</div><div>? Espacio: Campo Azul</div><div>? Fecha: 2025-07-21</div><div>? Hora: 21:00 - 23:00</div><div>? Total a pagar: S/40.00</div><div>? Pago: En banco</div></div><div style=\'margin: 15px 0; padding: 10px; background-color: #fff3cd; border: 1px solid #ffeaa7; border-radius: 5px;\'><div style=\'font-weight: 600; color: #856404;\'>? Instrucciones de pago:</div><div style=\'color: #856404;\'>1. Realiza el pago en banco por el monto indicado</div><div style=\'color: #856404;\'>2. Sube el comprobante en tu boleta</div><div style=\'color: #856404;\'>3. Espera la confirmaci√≥n del administrador</div></div>','2025-07-22 00:42:04'),(117,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','USUARIO','Quiero hacer una reserva','2025-07-31 06:04:09'),(118,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','BOT','¬øEn qu√© lugar te gustar√≠a consultar disponibilidad? ?Ô∏è\n\n? Lugares disponibles:','2025-07-31 06:04:11'),(119,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','USUARIO','Pabellon V PUCP','2025-07-31 06:04:16'),(120,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','BOT','¬øQu√© espacio deportivo dentro de Pabellon V PUCP deseas consultar?','2025-07-31 06:04:16'),(121,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','USUARIO','Cancha Minas PUCP','2025-07-31 06:04:20'),(122,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','BOT','¬øPara qu√© fecha deseas consultar la disponibilidad? ? (Ej: 2024-12-25, hoy, ma√±ana, etc.)','2025-07-31 06:04:20'),(123,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','USUARIO','Pasado ma√±ana','2025-07-31 06:04:26'),(124,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','BOT','No entend√≠ la fecha. Por favor ingr√©sala en formato v√°lido.','2025-07-31 06:04:26'),(125,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','USUARIO','ma√±ana','2025-07-31 06:04:35'),(126,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','BOT','¬øQu√© horario deseas consultar? ‚è∞ Indica la hora de inicio y fin (Ej: de 8am a 9am, 14-16)','2025-07-31 06:04:35'),(127,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','USUARIO','De 9am a 11am','2025-07-31 06:04:41'),(128,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','BOT','<div class=\'disponibilidad-resultado\'><div>‚úÖ <strong>¬°Espacio disponible!</strong></div><div>? Lugar: Pabellon V PUCP</div><div>? Fecha: 2025-08-01</div><div>? Horario: 9:00 - 11:00</div><div>? Costo estimado: S/3.00</div></div><div style=\'margin-top:10px;\'>¬øDeseas crear la reserva con estos datos?</div>','2025-07-31 06:04:41'),(129,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','USUARIO','S√≠, reservar','2025-07-31 06:04:48'),(130,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','BOT','¬øC√≥mo deseas pagar? ? Elige tu m√©todo de pago:','2025-07-31 06:04:48'),(131,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','USUARIO','En banco','2025-07-31 06:04:52'),(132,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','BOT','<div class=\'reserva-resumen\'><div style=\'font-weight:600;\'>? Resumen de tu reserva:</div><div>? Lugar: Pabellon V PUCP</div><div>? Fecha: 2025-08-01</div><div>? Horario: 9:00 - 11:00</div><div>? Pago: En banco</div><div>? Costo estimado: S/3.00</div></div><div style=\'margin-top:10px;\'>¬øConfirmas que deseas crear esta reserva?</div>','2025-07-31 06:04:52'),(133,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','USUARIO','S√≠, confirmar','2025-07-31 06:05:00'),(134,22,'7e53e2ce-fd96-4883-b804-032709e42ea1','BOT','<div class=\'reserva-final\'><div style=\'font-weight:600;\'>‚è≥ ¬°Reserva registrada exitosamente!</div><div style=\'color: #f39c12; font-weight: 500; margin: 10px 0;\'>‚ö†Ô∏è Pendiente de pago en banco</div><div>? ID Reserva: 57</div><div>? Espacio: Cancha Minas PUCP</div><div>? Fecha: 2025-08-01</div><div>? Hora: 9:00 - 11:00</div><div>? Total a pagar: S/3.00</div><div>? Pago: En banco</div></div><div style=\'margin: 15px 0; padding: 10px; background-color: #fff3cd; border: 1px solid #ffeaa7; border-radius: 5px;\'><div style=\'font-weight: 600; color: #856404;\'>? Instrucciones de pago:</div><div style=\'color: #856404;\'>1. Realiza el pago en banco por el monto indicado</div><div style=\'color: #856404;\'>2. Sube el comprobante en tu boleta</div><div style=\'color: #856404;\'>3. Espera la confirmaci√≥n del administrador</div></div>','2025-07-31 06:05:01'),(135,7,'8e7bfb4b-079a-42de-b525-6424f4ec80b6','USUARIO','Quiero cancelar una reserva','2025-07-31 16:45:35'),(136,7,'8e7bfb4b-079a-42de-b525-6424f4ec80b6','BOT','<div class=\'reservas-cancelables\'><h6>?Ô∏è Reservas que puedes cancelar:</h6><div class=\'reserva-item mb-2 p-2 border rounded\'><strong>1. ‚úÖ Bal√≥n de oro</strong><br>? 2025-08-01 | ‚è∞ 13:00 - 14:00<br>?Ô∏è Estado: Pendiente de confirmaci√≥n</div></div><p><strong>Escribe el n√∫mero de la reserva que deseas cancelar:</strong></p>','2025-07-31 16:45:37'),(137,7,'8e7bfb4b-079a-42de-b525-6424f4ec80b6','USUARIO','1','2025-07-31 16:45:42'),(138,7,'8e7bfb4b-079a-42de-b525-6424f4ec80b6','BOT','<div class=\'confirmacion-cancelacion\'><h6>‚úÖ Cancelaci√≥n Autom√°tica Disponible</h6><div class=\'alert alert-info\'><strong>?Ô∏è Espacio:</strong> Bal√≥n de oro<br><strong>? Fecha:</strong> 2025-08-01<br><strong>‚è∞ Horario:</strong> 13:00 - 14:00<br></div><p>Como tu reserva es con m√°s de 24 horas de anticipaci√≥n, se cancelar√° autom√°ticamente <strong>sin reembolso</strong>.</p><p><strong>¬øConfirmas la cancelaci√≥n?</strong></p><p>Responde \'S√ç\' para confirmar o \'NO\' para cancelar.</p></div>','2025-07-31 16:45:42'),(139,7,'8e7bfb4b-079a-42de-b525-6424f4ec80b6','USUARIO','1','2025-07-31 16:45:50'),(140,7,'8e7bfb4b-079a-42de-b525-6424f4ec80b6','BOT','‚ùì Por favor responde \'S√ç\' para confirmar la cancelaci√≥n o \'NO\' para mantener tu reserva.','2025-07-31 16:45:50'),(141,7,'8e7bfb4b-079a-42de-b525-6424f4ec80b6','USUARIO','S√ç','2025-07-31 16:45:54'),(142,7,'8e7bfb4b-079a-42de-b525-6424f4ec80b6','BOT','‚úÖ Reserva cancelada exitosamente sin reembolso.\n\n¬øTe gustar√≠a hacer alguna otra gesti√≥n? ?','2025-07-31 16:45:54');
/*!40000 ALTER TABLE `chat_mensaje` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `deporte`
--

DROP TABLE IF EXISTS `deporte`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deporte` (
  `idDeporte` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  PRIMARY KEY (`idDeporte`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `deporte`
--

LOCK TABLES `deporte` WRITE;
/*!40000 ALTER TABLE `deporte` DISABLE KEYS */;
INSERT INTO `deporte` VALUES (3,'Atletismo'),(4,'B√°squet'),(1,'F√∫tbol'),(2,'Nataci√≥n'),(5,'V√≥ley');
/*!40000 ALTER TABLE `deporte` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `espacio`
--

DROP TABLE IF EXISTS `espacio`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `espacio` (
  `idEspacio` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(45) DEFAULT NULL,
  `idLugar` int NOT NULL,
  `idEstadoEspacio` int NOT NULL,
  `idTipoEspacio` int DEFAULT NULL,
  `costo` double DEFAULT NULL,
  `observaciones` varchar(300) DEFAULT NULL,
  `descripcion` text,
  `foto1_url` varchar(2048) DEFAULT NULL,
  `foto2_url` varchar(2048) DEFAULT NULL,
  `foto3_url` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`idEspacio`),
  KEY `fk_Cancha_Lugar1_idx` (`idLugar`),
  KEY `fk_Cancha_EstadoCancha1_idx` (`idEstadoEspacio`),
  KEY `fk_Espacio_TipoEspacio` (`idTipoEspacio`),
  CONSTRAINT `fk_Cancha_EstadoCancha1` FOREIGN KEY (`idEstadoEspacio`) REFERENCES `estadoespacio` (`idEstadoEspacio`),
  CONSTRAINT `fk_Cancha_Lugar1` FOREIGN KEY (`idLugar`) REFERENCES `lugar` (`idLugar`),
  CONSTRAINT `fk_Espacio_TipoEspacio` FOREIGN KEY (`idTipoEspacio`) REFERENCES `tipoespacio` (`idTipoEspacio`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `espacio`
--

LOCK TABLES `espacio` WRITE;
/*!40000 ALTER TABLE `espacio` DISABLE KEYS */;
INSERT INTO `espacio` VALUES (1,'Bal√≥n de oro',1,1,1,1.5,'\nprueba observacion 3 (Agregado el 2025-05-16)\nprueba observacion 2 (Agregado el 2025-05-17)\nobs 4 (Agregado el 2025-05-17)','El espacio deportivo \"Bal√≥n de oro\" cuenta con una cancha de grass sint√©tico ideal para la pr√°ctica de f√∫tbol, entrenamientos, actividades recreativas y eventos deportivos. Su superficie es resistente, de bajo mantenimiento y adecuada para todas las edades. Ubicada en una zona accesible y segura, esta cancha est√° equipada con iluminaci√≥n para actividades nocturnas y cercan√≠a a ba√±os p√∫blicos y bebederos.','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/publica/espacios/1/1753728111731_090e3331.webp',NULL,NULL),(2,'Bal√≥n de platino',2,1,1,2,NULL,'La cancha \"Bal√≥n de platino\" ofrece una loza deportiva amplia y resistente, ideal para la pr√°ctica de f√∫tbol y otras actividades recreativas. Su superficie de concreto proporciona un excelente desempe√±o para partidos intensos y eventos deportivos. Est√° estrat√©gicamente ubicada para f√°cil acceso y equipada con √°reas de descanso y zonas verdes cercanas.','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/publica/espacios/2/1753979170862_f747e8d4.jpg',NULL,NULL),(3,'Campo celeste',2,1,3,2.5,NULL,'El \"Campo celeste\" es una piscina moderna dise√±ada para actividades acu√°ticas recreativas y competitivas. Con dimensiones ideales para el entrenamiento y la nataci√≥n libre, cuenta con sistema de filtrado avanzado, vestuarios completos y zonas de descanso alrededor. Su ubicaci√≥n es segura y de f√°cil acceso para todas las edades.','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/publica/espacios/3/1753979219013_42df68c0.jpg',NULL,NULL),(4,'Campo naranja',4,1,4,5,'prueba obs campo naranja 1 (Agregado el 2025-05-17)\nprueba obs campo naranja 2 (Agregado el 2025-05-17)','El \"Campo naranja\" alberga una pista de atletismo de alto rendimiento, perfecta para corredores profesionales y amateurs. Su superficie est√° dise√±ada para absorber impactos, reduciendo el riesgo de lesiones. El espacio cuenta con se√±alizaci√≥n adecuada, √°reas de calentamiento y est√° ubicado en una zona de alta accesibilidad.','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/publica/espacios/4/1753979246776_0615af37.jpg',NULL,NULL),(5,'Campo Deportivo Santos',5,1,1,3,NULL,'El \"Complejo Deportivo Santos\" pone a tu disposici√≥n una loza deportiva vers√°til, ideal para f√∫tbol, v√≥ley y b√°squet. Con medidas reglamentarias y superficies acondicionadas, es un espacio apto tanto para entrenamientos como para torneos locales. Cuenta con iluminaci√≥n nocturna y servicios b√°sicos cercanos.','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/publica/espacios/5/1753979284371_7dd18ff2.jpg',NULL,NULL),(6,'Campo Azul',6,1,3,1.7,NULL,'El \"Campo Azul\" es una piscina semiol√≠mpica adecuada para actividades recreativas y entrenamientos. Su profundidad variable permite su uso por personas de todas las edades. Adem√°s, cuenta con zonas de sombra, vestuarios equipados y acceso para personas con movilidad reducida.','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/publica/espacios/6/1753979202326_887ba596.webp',NULL,NULL),(26,'Piscina Nueva',1,1,3,4,NULL,'Piscina nueva que cuenta con mantenimiento diario, iluminaci√≥n nocturna y agua temperada. Perfecta para practicar nataci√≥n u otros deportes relacionados','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/publica/espacios/26/1753979489258_2892411a.jpg',NULL,NULL),(27,'Campo de F√∫tbol',1,1,1,1.6,NULL,'Esta cancha de grass sint√©tico cuenta con iluminaci√≥n nocturna, grass en buen estado e indumentaria para el deporte. ','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/publica/espacios/27/photo_2024-01-07_11-36-44.jpg',NULL,NULL),(28,'Cancha Minas PUCP',7,1,2,1.5,NULL,'Loza deportiva en la PUCP','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/publica/espacios/28/Cancha%20minas%20PUCP%20Loza.webp','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/publica/espacios/28/1753295868707_92d816fb.jpg',NULL),(29,'Mi casa',12,1,1,2,NULL,'Descripci√≥n',NULL,NULL,NULL);
/*!40000 ALTER TABLE `espacio` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `espacio_deporte`
--

DROP TABLE IF EXISTS `espacio_deporte`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `espacio_deporte` (
  `id_espacio` int NOT NULL,
  `id_deporte` int NOT NULL,
  PRIMARY KEY (`id_espacio`,`id_deporte`),
  KEY `id_deporte` (`id_deporte`),
  CONSTRAINT `espacio_deporte_ibfk_1` FOREIGN KEY (`id_espacio`) REFERENCES `espacio` (`idEspacio`),
  CONSTRAINT `espacio_deporte_ibfk_2` FOREIGN KEY (`id_deporte`) REFERENCES `deporte` (`idDeporte`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `espacio_deporte`
--

LOCK TABLES `espacio_deporte` WRITE;
/*!40000 ALTER TABLE `espacio_deporte` DISABLE KEYS */;
INSERT INTO `espacio_deporte` VALUES (1,1),(2,1),(5,1),(28,1),(29,1),(3,2),(6,2),(26,2),(1,3),(4,3),(2,4),(5,4),(27,4),(28,4);
/*!40000 ALTER TABLE `espacio_deporte` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `estadoespacio`
--

DROP TABLE IF EXISTS `estadoespacio`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estadoespacio` (
  `idEstadoEspacio` int NOT NULL AUTO_INCREMENT,
  `estado` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`idEstadoEspacio`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `estadoespacio`
--

LOCK TABLES `estadoespacio` WRITE;
/*!40000 ALTER TABLE `estadoespacio` DISABLE KEYS */;
INSERT INTO `estadoespacio` VALUES (1,'Disponible'),(2,'Mantenimiento');
/*!40000 ALTER TABLE `estadoespacio` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `estadogeo`
--

DROP TABLE IF EXISTS `estadogeo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estadogeo` (
  `idEstadoGeo` int NOT NULL AUTO_INCREMENT,
  `estado` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`idEstadoGeo`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `estadogeo`
--

LOCK TABLES `estadogeo` WRITE;
/*!40000 ALTER TABLE `estadogeo` DISABLE KEYS */;
INSERT INTO `estadogeo` VALUES (1,'Asisti√≥'),(2,'En Curso');
/*!40000 ALTER TABLE `estadogeo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `estadomensaje`
--

DROP TABLE IF EXISTS `estadomensaje`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estadomensaje` (
  `idEstadoMensaje` int NOT NULL AUTO_INCREMENT,
  `estado` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`idEstadoMensaje`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `estadomensaje`
--

LOCK TABLES `estadomensaje` WRITE;
/*!40000 ALTER TABLE `estadomensaje` DISABLE KEYS */;
INSERT INTO `estadomensaje` VALUES (1,'Enviado'),(2,'Le√≠do');
/*!40000 ALTER TABLE `estadomensaje` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `estadoreserva`
--

DROP TABLE IF EXISTS `estadoreserva`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estadoreserva` (
  `idEstadoReserva` int NOT NULL AUTO_INCREMENT,
  `estado` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`idEstadoReserva`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `estadoreserva`
--

LOCK TABLES `estadoreserva` WRITE;
/*!40000 ALTER TABLE `estadoreserva` DISABLE KEYS */;
INSERT INTO `estadoreserva` VALUES (1,'Confirmada'),(2,'Pendiente de confirmaci√≥n'),(3,'Finalizada'),(4,'Cancelada'),(5,'Cancelada con reembolso'),(6,'Reembolso solicitado'),(7,'Cancelada sin reembolso');
/*!40000 ALTER TABLE `estadoreserva` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `estadousu`
--

DROP TABLE IF EXISTS `estadousu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estadousu` (
  `idEstado` int NOT NULL AUTO_INCREMENT,
  `estado` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`idEstado`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `estadousu`
--

LOCK TABLES `estadousu` WRITE;
/*!40000 ALTER TABLE `estadousu` DISABLE KEYS */;
INSERT INTO `estadousu` VALUES (1,'Activo'),(2,'Desactivado');
/*!40000 ALTER TABLE `estadousu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `geolocalizacion`
--

DROP TABLE IF EXISTS `geolocalizacion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `geolocalizacion` (
  `idGeolocalizacion` int NOT NULL AUTO_INCREMENT,
  `fecha` date DEFAULT NULL,
  `horaInicio` time DEFAULT NULL,
  `horaFin` time DEFAULT NULL,
  `coordinador` int NOT NULL,
  `lugarExacto` varchar(100) DEFAULT NULL,
  `observacion` varchar(300) DEFAULT NULL,
  `estado` int NOT NULL,
  `lugar` int DEFAULT NULL,
  PRIMARY KEY (`idGeolocalizacion`),
  KEY `fk_Geolocalizacion_Usuarios1_idx` (`coordinador`),
  KEY `fk_Geolocalizacion_Estado1_idx` (`estado`),
  KEY `fk_geolocalizacion_lugar` (`lugar`),
  CONSTRAINT `fk_Geolocalizacion_Estado1` FOREIGN KEY (`estado`) REFERENCES `estadogeo` (`idEstadoGeo`),
  CONSTRAINT `fk_geolocalizacion_lugar` FOREIGN KEY (`lugar`) REFERENCES `lugar` (`idLugar`),
  CONSTRAINT `fk_Geolocalizacion_Usuarios1` FOREIGN KEY (`coordinador`) REFERENCES `usuarios` (`idUsuarios`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `geolocalizacion`
--

LOCK TABLES `geolocalizacion` WRITE;
/*!40000 ALTER TABLE `geolocalizacion` DISABLE KEYS */;
INSERT INTO `geolocalizacion` VALUES (20,'2025-07-22','15:20:16','15:25:53',2,'-11.909352883946532,-77.04294132737972','Sal√≠',1,12),(21,'2025-07-22','15:29:40','18:24:22',2,'-11.90929144881999,-77.04293132724871','Todo perfecto',1,12),(22,'2025-07-31','11:48:00','11:48:12',2,'-11.909378842220889,-77.04289991995516','salida',1,12),(23,'2025-07-31','17:26:26','17:43:15',2,'-12.072715194573455,-77.07922883843983','salida',1,6),(24,'2025-07-31','17:50:27','17:50:37',2,'-12.072800228335215,-77.07890480877279','SDADFISMGIR',1,6);
/*!40000 ALTER TABLE `geolocalizacion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `intentos_ip`
--

DROP TABLE IF EXISTS `intentos_ip`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `intentos_ip` (
  `id_intento` bigint NOT NULL AUTO_INCREMENT,
  `ip` varchar(45) NOT NULL,
  `tipo` varchar(30) NOT NULL,
  `ultima_solicitud` datetime NOT NULL,
  `contador` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id_intento`),
  UNIQUE KEY `unique_ip_tipo` (`ip`,`tipo`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `intentos_ip`
--

LOCK TABLES `intentos_ip` WRITE;
/*!40000 ALTER TABLE `intentos_ip` DISABLE KEYS */;
INSERT INTO `intentos_ip` VALUES (1,'0:0:0:0:0:0:0:1','OLVIDO','2025-07-12 17:53:57',1),(2,'0:0:0:0:0:0:0:1','CONSULTA_DNI','2025-07-31 01:01:34',1);
/*!40000 ALTER TABLE `intentos_ip` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lugar`
--

DROP TABLE IF EXISTS `lugar`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lugar` (
  `idLugar` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) DEFAULT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `ubicacion` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`idLugar`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lugar`
--

LOCK TABLES `lugar` WRITE;
/*!40000 ALTER TABLE `lugar` DISABLE KEYS */;
INSERT INTO `lugar` VALUES (1,'Complejo deportivo Sol','Av. Costanera 2000, San Miguel','-12.085432,-77.093517'),(2,'Complejo deportivo Luna','Parque Media Luna, Av. La Marina 2400','-12.085944,-77.078871'),(3,'Complejo deportivo Marte','Parque Juan Pablo II, Av. Riva Ag√ºero','-12.080692,-77.078481'),(4,'Complejo deportivo Saturno','Parque del Mar, Malec√≥n Bertolotto','-12.084100,-77.093002'),(5,'Complejo deportivo Urano','Parque de las Leyendas (zona deportiva)','-12.086506,-77.069192'),(6,'Complejo deportivo Tierra','Av. Universitaria 1500, cerca a la UPC San Miguel','-12.075103,-77.078639'),(7,'Pabellon V PUCP','Av. Universitaria 1801, San Miguel 15088','-12.073079,-77.082224'),(8,'Complejo Deportivo Municipal San Miguel','Av. La Marina 2355, San Miguel 15087','-12.077542,-77.084156'),(9,'Polideportivo Plaza San Miguel','Av. La Marina cdra. 23, San Miguel 15087','-12.078924,-77.083567'),(10,'Centro Deportivo Magdalena','Av. Brasil 2890, San Miguel 15086','-12.086234,-77.089445'),(11,'Complejo Recreacional El Pueblo','Jr. Mariscal Castilla 456, San Miguel 15088','-12.075612,-77.077889'),(12,'Mi Casa','Av. Universitaria, Comas','-11.909262,-77.043075');
/*!40000 ALTER TABLE `lugar` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lugar_coordinador`
--

DROP TABLE IF EXISTS `lugar_coordinador`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lugar_coordinador` (
  `id_lugar` int NOT NULL,
  `id_coordinador` int NOT NULL,
  PRIMARY KEY (`id_lugar`,`id_coordinador`),
  KEY `id_coordinador` (`id_coordinador`),
  CONSTRAINT `lugar_coordinador_ibfk_1` FOREIGN KEY (`id_lugar`) REFERENCES `lugar` (`idLugar`),
  CONSTRAINT `lugar_coordinador_ibfk_2` FOREIGN KEY (`id_coordinador`) REFERENCES `usuarios` (`idUsuarios`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lugar_coordinador`
--

LOCK TABLES `lugar_coordinador` WRITE;
/*!40000 ALTER TABLE `lugar_coordinador` DISABLE KEYS */;
INSERT INTO `lugar_coordinador` VALUES (1,2),(2,2),(3,2),(4,2),(5,2),(6,2),(7,2),(8,2),(9,2),(10,2),(11,2),(12,2);
/*!40000 ALTER TABLE `lugar_coordinador` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mantenimiento`
--

DROP TABLE IF EXISTS `mantenimiento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mantenimiento` (
  `id_mantenimiento` int NOT NULL AUTO_INCREMENT,
  `id_espacio` int NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date NOT NULL,
  `hora_inicio` time NOT NULL,
  `hora_fin` time NOT NULL,
  `tipo_mantenimiento` varchar(50) NOT NULL,
  `prioridad` varchar(20) NOT NULL,
  `descripcion` text NOT NULL,
  `responsable_id` int DEFAULT NULL,
  `creado_por` int NOT NULL,
  `costo_estimado` double DEFAULT NULL,
  `costo_real` double DEFAULT NULL,
  `estado` varchar(20) NOT NULL DEFAULT 'PROGRAMADO',
  `fecha_creacion` datetime NOT NULL,
  `fecha_inicio_real` datetime DEFAULT NULL,
  `fecha_fin_real` datetime DEFAULT NULL,
  `observaciones` text,
  `motivo_cancelacion` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_mantenimiento`),
  KEY `fk_mantenimiento_espacio` (`id_espacio`),
  KEY `fk_mantenimiento_responsable` (`responsable_id`),
  KEY `fk_mantenimiento_creado_por` (`creado_por`),
  CONSTRAINT `fk_mantenimiento_creado_por` FOREIGN KEY (`creado_por`) REFERENCES `usuarios` (`idUsuarios`),
  CONSTRAINT `fk_mantenimiento_espacio` FOREIGN KEY (`id_espacio`) REFERENCES `espacio` (`idEspacio`),
  CONSTRAINT `fk_mantenimiento_responsable` FOREIGN KEY (`responsable_id`) REFERENCES `usuarios` (`idUsuarios`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mantenimiento`
--

LOCK TABLES `mantenimiento` WRITE;
/*!40000 ALTER TABLE `mantenimiento` DISABLE KEYS */;
INSERT INTO `mantenimiento` VALUES (1,1,'2025-08-02','2025-08-02','19:00:00','20:00:00','CORRECTIVO','MEDIA','PRUEBA DE MANTENIMIENTO',2,4,10,NULL,'PROGRAMADO','2025-07-26 15:00:07',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `mantenimiento` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mensaje`
--

DROP TABLE IF EXISTS `mensaje`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mensaje` (
  `idMensaje` int NOT NULL AUTO_INCREMENT,
  `horaEnvio` timestamp NULL DEFAULT NULL,
  `transmisor` int NOT NULL,
  `receptor` int NOT NULL,
  `mensaje` varchar(500) DEFAULT NULL,
  `estado` int NOT NULL,
  PRIMARY KEY (`idMensaje`),
  KEY `fk_Mensaje_Usuarios1_idx` (`transmisor`),
  KEY `fk_Mensaje_Usuarios2_idx` (`receptor`),
  KEY `fk_Mensaje_EstadoMensaje1_idx` (`estado`),
  CONSTRAINT `fk_Mensaje_EstadoMensaje1` FOREIGN KEY (`estado`) REFERENCES `estadomensaje` (`idEstadoMensaje`),
  CONSTRAINT `fk_Mensaje_Usuarios1` FOREIGN KEY (`transmisor`) REFERENCES `usuarios` (`idUsuarios`),
  CONSTRAINT `fk_Mensaje_Usuarios2` FOREIGN KEY (`receptor`) REFERENCES `usuarios` (`idUsuarios`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mensaje`
--

LOCK TABLES `mensaje` WRITE;
/*!40000 ALTER TABLE `mensaje` DISABLE KEYS */;
INSERT INTO `mensaje` VALUES (5,'2025-05-11 13:00:00',3,1,'Buenos d√≠as, recuerde su reserva',1),(6,'2025-05-11 13:02:00',3,1,'¬øAsistir√° al evento de hoy?',1),(7,'2025-05-11 13:05:00',3,1,'No olvide confirmar su asistencia',2),(8,'2025-05-11 13:07:00',3,1,'Gracias por participar',2);
/*!40000 ALTER TABLE `mensaje` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `observacion_espacio`
--

DROP TABLE IF EXISTS `observacion_espacio`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `observacion_espacio` (
  `id` int NOT NULL AUTO_INCREMENT,
  `contenido` text NOT NULL,
  `fecha` datetime NOT NULL,
  `espacio` int NOT NULL,
  `usuario` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `espacio` (`espacio`),
  KEY `usuario` (`usuario`),
  CONSTRAINT `observacion_espacio_ibfk_1` FOREIGN KEY (`espacio`) REFERENCES `espacio` (`idEspacio`),
  CONSTRAINT `observacion_espacio_ibfk_2` FOREIGN KEY (`usuario`) REFERENCES `usuarios` (`idUsuarios`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `observacion_espacio`
--

LOCK TABLES `observacion_espacio` WRITE;
/*!40000 ALTER TABLE `observacion_espacio` DISABLE KEYS */;
INSERT INTO `observacion_espacio` VALUES (1,'Prueba de observaci√≥n para este espacio','2025-07-18 00:00:00',1,2),(2,'Segunda prueba de obs','2025-07-18 00:00:00',1,2),(3,'obs 1 prueba','2025-07-18 22:42:50',3,2),(4,'otra obs alerta','2025-07-18 22:45:50',5,2),(5,'dasda','2025-07-18 22:48:17',2,2),(6,'asdasdasd','2025-07-18 22:58:21',2,2),(7,'prueba alert','2025-07-18 23:19:16',1,2),(8,'asdddd12','2025-07-18 23:20:14',2,2),(9,'fgdh234','2025-07-18 23:20:24',5,2),(10,'otraaaaa','2025-07-18 23:30:52',2,2),(11,'dhgfhfhh11111','2025-07-18 23:37:42',4,2),(12,'1232','2025-07-18 23:39:40',5,2),(13,'Ac√° falta limpiar','2025-07-19 14:18:56',1,2);
/*!40000 ALTER TABLE `observacion_espacio` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `otpverification`
--

DROP TABLE IF EXISTS `otpverification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `otpverification` (
  `idOtpVerification` int NOT NULL AUTO_INCREMENT,
  `identificador` varchar(100) NOT NULL,
  `otpCode` varchar(10) NOT NULL,
  `fechaCreacion` datetime DEFAULT CURRENT_TIMESTAMP,
  `fechaExpiracion` datetime NOT NULL,
  PRIMARY KEY (`idOtpVerification`),
  UNIQUE KEY `identificador_UNIQUE` (`identificador`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `otpverification`
--

LOCK TABLES `otpverification` WRITE;
/*!40000 ALTER TABLE `otpverification` DISABLE KEYS */;
INSERT INTO `otpverification` VALUES (13,'925253151','4895','2025-07-12 15:53:02','2025-07-12 15:58:02');
/*!40000 ALTER TABLE `otpverification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pago`
--

DROP TABLE IF EXISTS `pago`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pago` (
  `idPago` int NOT NULL AUTO_INCREMENT,
  `idReserva` int NOT NULL,
  `monto` decimal(10,2) NOT NULL,
  `fechaPago` timestamp NULL DEFAULT NULL,
  `tipoPago` varchar(50) DEFAULT NULL,
  `estado` varchar(50) DEFAULT NULL,
  `referencia` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`idPago`),
  KEY `fk_Pago_Reserva_idx` (`idReserva`),
  CONSTRAINT `fk_Pago_Reserva` FOREIGN KEY (`idReserva`) REFERENCES `reserva` (`idReserva`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pago`
--

LOCK TABLES `pago` WRITE;
/*!40000 ALTER TABLE `pago` DISABLE KEYS */;
INSERT INTO `pago` VALUES (35,54,2.00,'2025-07-31 05:59:58','En banco','Pagado','Confirmado por coordinador: Fernando'),(36,56,4.00,'2025-07-31 06:46:31','En banco','REEMBOLSO_SOLICITADO','Confirmado por coordinador: Fernando'),(37,60,1.50,'2025-07-31 11:59:17','En banco','REEMBOLSO_SOLICITADO','Confirmado por coordinador: Fernando'),(38,62,2.50,'2025-07-31 23:33:13','En banco','REEMBOLSO_SOLICITADO','Confirmado por coordinador: Fernando');
/*!40000 ALTER TABLE `pago` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_reset_token`
--

DROP TABLE IF EXISTS `password_reset_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `correo` varchar(100) NOT NULL,
  `token` varchar(64) NOT NULL,
  `fecha_creacion` timestamp NOT NULL,
  `fecha_expiracion` timestamp NOT NULL,
  `usado` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_token`
--

LOCK TABLES `password_reset_token` WRITE;
/*!40000 ALTER TABLE `password_reset_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `password_reset_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reembolso`
--

DROP TABLE IF EXISTS `reembolso`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reembolso` (
  `id_reembolso` int NOT NULL AUTO_INCREMENT,
  `solicitud_cancelacion_id` int NOT NULL,
  `reserva_id` int NOT NULL,
  `monto_reembolso` decimal(10,2) NOT NULL,
  `tipo_pago_original` varchar(20) NOT NULL,
  `estado_reembolso` varchar(20) NOT NULL,
  `metodo_reembolso` varchar(30) DEFAULT NULL,
  `aprobado_por_coordinador` int NOT NULL,
  `fecha_aprobacion` datetime NOT NULL,
  `motivo_aprobacion` text,
  `procesado_por_admin` int DEFAULT NULL,
  `fecha_procesamiento` datetime DEFAULT NULL,
  `observaciones_admin` text,
  `id_transaccion_reembolso` varchar(255) DEFAULT NULL,
  `respuesta_mercadopago` text,
  `numero_operacion` varchar(255) DEFAULT NULL,
  `entidad_bancaria` varchar(255) DEFAULT NULL,
  `fecha_creacion` datetime NOT NULL,
  PRIMARY KEY (`id_reembolso`),
  KEY `solicitud_cancelacion_id` (`solicitud_cancelacion_id`),
  KEY `reserva_id` (`reserva_id`),
  KEY `aprobado_por_coordinador` (`aprobado_por_coordinador`),
  KEY `procesado_por_admin` (`procesado_por_admin`),
  CONSTRAINT `reembolso_ibfk_1` FOREIGN KEY (`solicitud_cancelacion_id`) REFERENCES `solicitud_cancelacion` (`id`),
  CONSTRAINT `reembolso_ibfk_2` FOREIGN KEY (`reserva_id`) REFERENCES `reserva` (`idReserva`),
  CONSTRAINT `reembolso_ibfk_3` FOREIGN KEY (`aprobado_por_coordinador`) REFERENCES `usuarios` (`idUsuarios`),
  CONSTRAINT `reembolso_ibfk_4` FOREIGN KEY (`procesado_por_admin`) REFERENCES `usuarios` (`idUsuarios`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reembolso`
--

LOCK TABLES `reembolso` WRITE;
/*!40000 ALTER TABLE `reembolso` DISABLE KEYS */;
INSERT INTO `reembolso` VALUES (1,6,60,1.50,'En banco','COMPLETADO',NULL,2,'2025-07-31 07:03:41','prueba de aceptaci√≥n con comprobante subido por el vecino, se debe enviar a admin',4,'2025-07-31 07:41:19','aceptado',NULL,NULL,NULL,NULL,'2025-07-31 07:03:41');
/*!40000 ALTER TABLE `reembolso` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reserva`
--

DROP TABLE IF EXISTS `reserva`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reserva` (
  `idReserva` int NOT NULL AUTO_INCREMENT,
  `horaInicio` time DEFAULT NULL,
  `horaFin` time DEFAULT NULL,
  `fecha` date DEFAULT NULL,
  `coordinador` int NOT NULL,
  `costo` double DEFAULT NULL,
  `vecino` int NOT NULL,
  `estado` int NOT NULL,
  `estado_reembolso` varchar(30) DEFAULT NULL,
  `espacio` int NOT NULL,
  `momentoReserva` datetime DEFAULT NULL,
  `tipoPago` varchar(20) DEFAULT NULL,
  `estado_pago` varchar(20) DEFAULT NULL,
  `id_transaccion_pago` varchar(100) DEFAULT NULL,
  `fecha_pago` timestamp NULL DEFAULT NULL,
  `captura_url` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`idReserva`),
  KEY `fk_Reserva_Usuarios1_idx` (`vecino`),
  KEY `fk_Reserva_Usuarios2_idx` (`coordinador`),
  KEY `fk_Reserva_EstadoReserva1_idx` (`estado`),
  KEY `fk_Reserva_Cancha1_idx` (`espacio`),
  CONSTRAINT `fk_Reserva_Cancha1` FOREIGN KEY (`espacio`) REFERENCES `espacio` (`idEspacio`),
  CONSTRAINT `fk_Reserva_EstadoReserva1` FOREIGN KEY (`estado`) REFERENCES `estadoreserva` (`idEstadoReserva`),
  CONSTRAINT `fk_Reserva_Usuarios1` FOREIGN KEY (`vecino`) REFERENCES `usuarios` (`idUsuarios`),
  CONSTRAINT `fk_Reserva_Usuarios2` FOREIGN KEY (`coordinador`) REFERENCES `usuarios` (`idUsuarios`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reserva`
--

LOCK TABLES `reserva` WRITE;
/*!40000 ALTER TABLE `reserva` DISABLE KEYS */;
INSERT INTO `reserva` VALUES (53,'11:00:00','12:00:00','2025-07-30',2,1.5,7,3,'NO_APLICA',1,'2025-07-26 12:54:31','En banco',NULL,NULL,NULL,NULL),(54,'14:00:00','15:00:00','2025-08-01',2,2,7,5,'NO_APLICA',2,'2025-07-31 00:23:48','En banco','Pagado',NULL,'2025-07-31 05:59:58','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/privada/comprobantes/54/1753941449257_5be6f668.png'),(55,'18:00:00','20:00:00','2025-08-02',2,3,7,4,'NO_APLICA',1,'2025-07-31 00:49:28','En banco',NULL,NULL,NULL,NULL),(56,'08:00:00','10:00:00','2025-08-02',2,4,7,7,'PENDIENTE',2,'2025-07-31 00:56:00','En banco','Pagado',NULL,'2025-07-31 06:46:31','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/privada/comprobantes/56/1753944359952_e3fa39f5.png'),(57,'09:00:00','11:00:00','2025-08-01',2,3,22,4,NULL,28,'2025-07-31 01:05:01','En banco','Pendiente',NULL,NULL,NULL),(58,'10:00:00','11:00:00','2025-08-02',2,1.5,22,4,'NO_APLICA',1,'2025-07-31 01:21:45','En banco',NULL,NULL,NULL,NULL),(59,'12:00:00','13:00:00','2025-07-31',2,1.5,7,4,'NO_APLICA',1,'2025-07-31 06:20:04','En banco',NULL,NULL,NULL,NULL),(60,'08:00:00','09:00:00','2025-08-05',2,1.5,7,4,'APROBADO',1,'2025-07-31 06:58:39','En banco','Pagado',NULL,'2025-07-31 11:59:17','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/privada/comprobantes/60/1753963130159_0ffd5f5a.png'),(61,'13:00:00','14:00:00','2025-08-01',2,1.5,7,7,'NO_APLICA',1,'2025-07-31 09:39:28','En banco',NULL,NULL,NULL,NULL),(62,'11:00:00','12:00:00','2025-08-01',2,2.5,7,6,'PENDIENTE',3,'2025-07-31 18:32:34','En banco','Pagado',NULL,'2025-07-31 23:33:13','https://almacenamiento-aplicacion-archivos.s3.us-east-1.amazonaws.com/privada/comprobantes/62/1754004774281_c15f5512.webp');
/*!40000 ALTER TABLE `reserva` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rol`
--

DROP TABLE IF EXISTS `rol`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rol` (
  `idRol` int NOT NULL AUTO_INCREMENT,
  `rol` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`idRol`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rol`
--

LOCK TABLES `rol` WRITE;
/*!40000 ALTER TABLE `rol` DISABLE KEYS */;
INSERT INTO `rol` VALUES (1,'Usuario final'),(2,'Coordinador'),(3,'Administrador'),(4,'SuperAdmin');
/*!40000 ALTER TABLE `rol` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `solicitud_cancelacion`
--

DROP TABLE IF EXISTS `solicitud_cancelacion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitud_cancelacion` (
  `id` int NOT NULL AUTO_INCREMENT,
  `reserva_id` int NOT NULL,
  `motivo` text NOT NULL,
  `estado` varchar(20) DEFAULT 'Pendiente',
  `codigo_pago` varchar(50) DEFAULT NULL,
  `comprobante_url` varchar(255) DEFAULT NULL,
  `fecha_solicitud` datetime NOT NULL,
  `tiempo_respuesta` datetime DEFAULT NULL COMMENT 'Fecha y hora cuando se proces√≥ la solicitud',
  `motivo_respuesta` text COMMENT 'Motivo proporcionado por el coordinador al aceptar o rechazar la solicitud',
  PRIMARY KEY (`id`),
  KEY `reserva_id` (`reserva_id`),
  KEY `idx_solicitud_cancelacion_tiempo_respuesta` (`tiempo_respuesta`),
  KEY `idx_solicitud_cancelacion_estado_tiempo` (`estado`,`tiempo_respuesta`),
  CONSTRAINT `solicitud_cancelacion_ibfk_1` FOREIGN KEY (`reserva_id`) REFERENCES `reserva` (`idReserva`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `solicitud_cancelacion`
--

LOCK TABLES `solicitud_cancelacion` WRITE;
/*!40000 ALTER TABLE `solicitud_cancelacion` DISABLE KEYS */;
INSERT INTO `solicitud_cancelacion` VALUES (5,56,'prueba de cancelaci√≥n','Rechazada','123',NULL,'2025-07-31 06:16:27','2025-07-31 06:53:29','dsfgdgdgdg'),(6,60,'dsdggdfgdgse','Completado','',NULL,'2025-07-31 06:59:37','2025-07-31 07:41:19','Reembolso gestionado manualmente por el administrador: aceptado'),(7,62,'asdasdfsgsdfsd','Pendiente','',NULL,'2025-07-31 18:33:36',NULL,NULL);
/*!40000 ALTER TABLE `solicitud_cancelacion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `spring_session`
--

DROP TABLE IF EXISTS `spring_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spring_session` (
  `PRIMARY_ID` char(36) NOT NULL,
  `SESSION_ID` char(36) NOT NULL,
  `CREATION_TIME` bigint NOT NULL,
  `LAST_ACCESS_TIME` bigint NOT NULL,
  `MAX_INACTIVE_INTERVAL` int NOT NULL,
  `EXPIRY_TIME` bigint NOT NULL,
  `PRINCIPAL_NAME` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`PRIMARY_ID`),
  UNIQUE KEY `SPRING_SESSION_IX1` (`SESSION_ID`),
  KEY `SPRING_SESSION_IX2` (`EXPIRY_TIME`),
  KEY `SPRING_SESSION_IX3` (`PRINCIPAL_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `spring_session`
--

LOCK TABLES `spring_session` WRITE;
/*!40000 ALTER TABLE `spring_session` DISABLE KEYS */;
INSERT INTO `spring_session` VALUES ('0eeaefba-5dd0-456a-9a8d-ac7f579db5a1','016a2f1c-9cff-4e41-a671-9734bf44a22c',1754000769241,1754000787268,1800,1754002587268,'20206311'),('4d8da0b3-ee84-452f-9bbf-82ec0fdf92a2','809213bd-e45b-4dc5-b4b0-804f99446ebb',1754004764752,1754004798517,1800,1754006598517,'20206311'),('5052ebc2-f1ac-4f34-873d-01962b098470','2ba4200d-cf12-4ac1-b029-69b22cd22bc3',1754004687853,1754004851797,1800,1754006651797,'76608126'),('8de093c4-6dd0-4a15-b7dd-23a7667b725d','43cfda3b-f7b3-4449-8016-8c42c532ff43',1754001808447,1754002540911,1800,1754004340911,'20206311');
/*!40000 ALTER TABLE `spring_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `spring_session_attributes`
--

DROP TABLE IF EXISTS `spring_session_attributes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spring_session_attributes` (
  `SESSION_PRIMARY_ID` char(36) NOT NULL,
  `ATTRIBUTE_NAME` varchar(200) NOT NULL,
  `ATTRIBUTE_BYTES` blob NOT NULL,
  PRIMARY KEY (`SESSION_PRIMARY_ID`,`ATTRIBUTE_NAME`),
  CONSTRAINT `SPRING_SESSION_ATTRIBUTES_FK` FOREIGN KEY (`SESSION_PRIMARY_ID`) REFERENCES `spring_session` (`PRIMARY_ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `spring_session_attributes`
--

LOCK TABLES `spring_session_attributes` WRITE;
/*!40000 ALTER TABLE `spring_session_attributes` DISABLE KEYS */;
INSERT INTO `spring_session_attributes` VALUES ('0eeaefba-5dd0-456a-9a8d-ac7f579db5a1','idConversacionActual',_binary '¨\Ì\0t\0$2b72e8f1-0e3e-4172-bdcb-1f05aa8f1663'),('0eeaefba-5dd0-456a-9a8d-ac7f579db5a1','org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN',_binary '¨\Ì\0sr\06org.springframework.security.web.csrf.DefaultCsrfTokenZ\Ô∑\»/¢˚\’\0L\0\nheaderNamet\0Ljava/lang/String;L\0\rparameterNameq\0~\0L\0tokenq\0~\0xpt\0X-CSRF-TOKENt\0_csrft\0$7871c2d5-b756-4c3e-a31d-9970f4e25343'),('0eeaefba-5dd0-456a-9a8d-ac7f579db5a1','SPRING_SECURITY_CONTEXT',_binary '¨\Ì\0sr\0=org.springframework.security.core.context.SecurityContextImpl\0\0\0\0\0\0l\0L\0authenticationt\02Lorg/springframework/security/core/Authentication;xpsr\0Oorg.springframework.security.authentication.UsernamePasswordAuthenticationToken\0\0\0\0\0\0l\0L\0credentialst\0Ljava/lang/Object;L\0	principalq\0~\0xr\0Gorg.springframework.security.authentication.AbstractAuthenticationToken”™(~nGd\0Z\0\rauthenticatedL\0authoritiest\0Ljava/util/Collection;L\0detailsq\0~\0xpsr\0&java.util.Collections$UnmodifiableList¸%1µ\Ïé\0L\0listt\0Ljava/util/List;xr\0,java.util.Collections$UnmodifiableCollectionB\0Ä\À^˜\0L\0cq\0~\0xpsr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sr\0Borg.springframework.security.core.authority.SimpleGrantedAuthority\0\0\0\0\0\0l\0L\0rolet\0Ljava/lang/String;xpt\0ROLE_Coordinadorxq\0~\0\rsr\0Horg.springframework.security.web.authentication.WebAuthenticationDetails\0\0\0\0\0\0l\0L\0\rremoteAddressq\0~\0L\0	sessionIdq\0~\0xpt\00:0:0:0:0:0:0:1t\0$f56f1ae4-2fc0-4880-8ae1-f823f6747308psr\02org.springframework.security.core.userdetails.User\0\0\0\0\0\0l\0Z\0accountNonExpiredZ\0accountNonLockedZ\0credentialsNonExpiredZ\0enabledL\0authoritiest\0Ljava/util/Set;L\0passwordq\0~\0L\0usernameq\0~\0xpsr\0%java.util.Collections$UnmodifiableSetÄí—èõÄU\0\0xq\0~\0\nsr\0java.util.TreeSet›òPìï\Ìá[\0\0xpsr\0Forg.springframework.security.core.userdetails.User$AuthorityComparator\0\0\0\0\0\0l\0\0xpw\0\0\0q\0~\0xpt\020206311'),('0eeaefba-5dd0-456a-9a8d-ac7f579db5a1','SPRING_SECURITY_SAVED_REQUEST',_binary '¨\Ì\0sr\0Aorg.springframework.security.web.savedrequest.DefaultSavedRequest\0\0\0\0\0\0l\0I\0\nserverPortL\0contextPatht\0Ljava/lang/String;L\0cookiest\0Ljava/util/ArrayList;L\0headerst\0Ljava/util/Map;L\0localesq\0~\0L\0matchingRequestParameterNameq\0~\0L\0methodq\0~\0L\0\nparametersq\0~\0L\0pathInfoq\0~\0L\0queryStringq\0~\0L\0\nrequestURIq\0~\0L\0\nrequestURLq\0~\0L\0schemeq\0~\0L\0\nserverNameq\0~\0L\0servletPathq\0~\0xp\0\0êt\0\0sr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sr\09org.springframework.security.web.savedrequest.SavedCookie\0\0\0\0\0\0l\0I\0maxAgeZ\0secureI\0versionL\0commentq\0~\0L\0domainq\0~\0L\0nameq\0~\0L\0pathq\0~\0L\0valueq\0~\0xpˇˇˇˇ\0\0\0\0\0ppt\0_omappvppt\0`9H3RoDQFKq0HkhoIArvYwZF6YzLu4fZvlkLeFre9maawNjEqAylkQvM7cQcWwZR9Cd50nnK33OwengivqQUnoR8EGNM5zzbNsq\0~\0ˇˇˇˇ\0\0\0\0\0ppt\0_omrapt\0-%7B%22mmosgju0tkru8ajiwhig%22%3A%22view%22%7Dsq\0~\0ˇˇˇˇ\0\0\0\0\0ppt\0\rIdea-3d6b315apt\0$312b8825-2076-4231-8500-49d531a121e6sq\0~\0ˇˇˇˇ\0\0\0\0\0ppt\0SESSIONpt\00ZjU2ZjFhZTQtMmZjMC00ODgwLThhZTEtZjgyM2Y2NzQ3MzA4xsr\0java.util.TreeMap¡ˆ>-%j\Ê\0L\0\ncomparatort\0Ljava/util/Comparator;xpsr\0*java.lang.String$CaseInsensitiveComparatorw\\}\\P\Â\Œ\0\0xpw\0\0\0t\0acceptsq\0~\0\0\0\0w\0\0\0t\0*/*xt\0accept-encodingsq\0~\0\0\0\0w\0\0\0t\0gzip, deflate, br, zstdxt\0accept-languagesq\0~\0\0\0\0w\0\0\0t\0<es-419,es;q=0.9,en;q=0.8,en-US;q=0.7,es-PE;q=0.6,es-ES;q=0.5xt\0\nconnectionsq\0~\0\0\0\0w\0\0\0t\0\nkeep-alivext\0cookiesq\0~\0\0\0\0w\0\0\0t_omappvp=9H3RoDQFKq0HkhoIArvYwZF6YzLu4fZvlkLeFre9maawNjEqAylkQvM7cQcWwZR9Cd50nnK33OwengivqQUnoR8EGNM5zzbN; _omra=%7B%22mmosgju0tkru8ajiwhig%22%3A%22view%22%7D; Idea-3d6b315a=312b8825-2076-4231-8500-49d531a121e6; SESSION=ZjU2ZjFhZTQtMmZjMC00ODgwLThhZTEtZjgyM2Y2NzQ3MzA4xt\0hostsq\0~\0\0\0\0w\0\0\0t\0localhost:8080xt\0referersq\0~\0\0\0\0w\0\0\0t\0http://localhost:8080/loginxt\0	sec-ch-uasq\0~\0\0\0\0w\0\0\0t\0A\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Microsoft Edge\";v=\"138\"xt\0sec-ch-ua-mobilesq\0~\0\0\0\0w\0\0\0t\0?0xt\0sec-ch-ua-platformsq\0~\0\0\0\0w\0\0\0t\0	\"Windows\"xt\0sec-fetch-destsq\0~\0\0\0\0w\0\0\0t\0emptyxt\0sec-fetch-modesq\0~\0\0\0\0w\0\0\0t\0corsxt\0sec-fetch-sitesq\0~\0\0\0\0w\0\0\0t\0same-originxt\0\nuser-agentsq\0~\0\0\0\0w\0\0\0t\0}Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0xxsq\0~\0\0\0\0w\0\0\0sr\0java.util.Locale~¯`ú0˘\Ï\0I\0hashcodeL\0countryq\0~\0L\0\nextensionsq\0~\0L\0languageq\0~\0L\0scriptq\0~\0L\0variantq\0~\0xpˇˇˇˇt\0419q\0~\0t\0esq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇq\0~\0q\0~\0q\0~\0Hq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇq\0~\0q\0~\0t\0enq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇt\0USq\0~\0q\0~\0Kq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇt\0PEq\0~\0q\0~\0Hq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇt\0ESq\0~\0q\0~\0Hq\0~\0q\0~\0xxt\0continuet\0GETsq\0~\0pw\0\0\0\0xppt\0/json/locales/en.jsont\0*http://localhost:8080/json/locales/en.jsont\0httpt\0	localhostt\0/json/locales/en.json'),('0eeaefba-5dd0-456a-9a8d-ac7f579db5a1','usuario',_binary '¨\Ì\0sr\0#com.example.project.entity.Usuarios\0\0\0\0\0\0\0\0\rL\0	apellidost\0Ljava/lang/String;L\0confirmContrasenaq\0~\0L\0\ncontrasenaq\0~\0L\0correoq\0~\0L\0dniq\0~\0L\0estadot\0&Lcom/example/project/entity/EstadoUsu;L\0\rfechaCreaciont\0Ljava/sql/Timestamp;L\0\nfotoPerfilq\0~\0L\0\nidUsuariost\0Ljava/lang/Integer;L\0lugaresAsignadost\0Ljava/util/List;L\0nombresq\0~\0L\0rolt\0 Lcom/example/project/entity/Rol;L\0telefonoq\0~\0xpt\0Godoypt\0<$2a$10$JcUVX/2V/tpH8pk20dkHc.23lOFp78u0zS8RfVa/TgtLDev.3iiKet\0juanielulloavega2@gmail.comt\020206311sr\0$com.example.project.entity.EstadoUsu\0\0\0\0\0\0\0\0L\0estadoq\0~\0L\0idEstadoq\0~\0xpt\0Activosr\0java.lang.Integer‚†§˜Åá8\0I\0valuexr\0java.lang.NumberÜ¨ïî\‡ã\0\0xp\0\0\0sr\0java.sql.Timestamp&\’\»Søe\0I\0nanosxr\0java.util.DatehjÅKYt\0\0xpw\0\0ñ\⁄Ÿö\0x\0\0\0\0psq\0~\0\0\0\0sr\0*org.hibernate.collection.spi.PersistentBagñ<a#:rAí\0L\0bagq\0~\0L\0providedCollectiont\0Ljava/util/Collection;xr\09org.hibernate.collection.spi.AbstractPersistentCollection3§∞J<F\0Z\0allowLoadOutsideTransactionI\0\ncachedSizeZ\0dirtyZ\0elementRemovedZ\0initializedZ\0\risTempSessionL\0keyt\0Ljava/lang/Object;L\0ownerq\0~\0L\0roleq\0~\0L\0sessionFactoryUuidq\0~\0L\0storedSnapshott\0Ljava/io/Serializable;xp\0ˇˇˇˇ\0\0\0\0q\0~\0q\0~\0t\04com.example.project.entity.Usuarios.lugaresAsignadosppppt\0Fernandosr\0com.example.project.entity.Rol\0\0\0\0\0\0\0\0L\0idRolq\0~\0L\0rolq\0~\0xpq\0~\0t\0Coordinadort\0	987654322'),('4d8da0b3-ee84-452f-9bbf-82ec0fdf92a2','idConversacionActual',_binary '¨\Ì\0t\0$947c5d70-d143-423d-af46-4bca5439df22'),('4d8da0b3-ee84-452f-9bbf-82ec0fdf92a2','org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN',_binary '¨\Ì\0sr\06org.springframework.security.web.csrf.DefaultCsrfTokenZ\Ô∑\»/¢˚\’\0L\0\nheaderNamet\0Ljava/lang/String;L\0\rparameterNameq\0~\0L\0tokenq\0~\0xpt\0X-CSRF-TOKENt\0_csrft\0$df04d2b3-e7cc-4dfa-b6c7-9f739fc95491'),('4d8da0b3-ee84-452f-9bbf-82ec0fdf92a2','SPRING_SECURITY_CONTEXT',_binary '¨\Ì\0sr\0=org.springframework.security.core.context.SecurityContextImpl\0\0\0\0\0\0l\0L\0authenticationt\02Lorg/springframework/security/core/Authentication;xpsr\0Oorg.springframework.security.authentication.UsernamePasswordAuthenticationToken\0\0\0\0\0\0l\0L\0credentialst\0Ljava/lang/Object;L\0	principalq\0~\0xr\0Gorg.springframework.security.authentication.AbstractAuthenticationToken”™(~nGd\0Z\0\rauthenticatedL\0authoritiest\0Ljava/util/Collection;L\0detailsq\0~\0xpsr\0&java.util.Collections$UnmodifiableList¸%1µ\Ïé\0L\0listt\0Ljava/util/List;xr\0,java.util.Collections$UnmodifiableCollectionB\0Ä\À^˜\0L\0cq\0~\0xpsr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sr\0Borg.springframework.security.core.authority.SimpleGrantedAuthority\0\0\0\0\0\0l\0L\0rolet\0Ljava/lang/String;xpt\0ROLE_Coordinadorxq\0~\0\rsr\0Horg.springframework.security.web.authentication.WebAuthenticationDetails\0\0\0\0\0\0l\0L\0\rremoteAddressq\0~\0L\0	sessionIdq\0~\0xpt\00:0:0:0:0:0:0:1t\0$7c41ff03-521a-4cfb-b97d-26eace23e86apsr\02org.springframework.security.core.userdetails.User\0\0\0\0\0\0l\0Z\0accountNonExpiredZ\0accountNonLockedZ\0credentialsNonExpiredZ\0enabledL\0authoritiest\0Ljava/util/Set;L\0passwordq\0~\0L\0usernameq\0~\0xpsr\0%java.util.Collections$UnmodifiableSetÄí—èõÄU\0\0xq\0~\0\nsr\0java.util.TreeSet›òPìï\Ìá[\0\0xpsr\0Forg.springframework.security.core.userdetails.User$AuthorityComparator\0\0\0\0\0\0l\0\0xpw\0\0\0q\0~\0xpt\020206311'),('4d8da0b3-ee84-452f-9bbf-82ec0fdf92a2','SPRING_SECURITY_SAVED_REQUEST',_binary '¨\Ì\0sr\0Aorg.springframework.security.web.savedrequest.DefaultSavedRequest\0\0\0\0\0\0l\0I\0\nserverPortL\0contextPatht\0Ljava/lang/String;L\0cookiest\0Ljava/util/ArrayList;L\0headerst\0Ljava/util/Map;L\0localesq\0~\0L\0matchingRequestParameterNameq\0~\0L\0methodq\0~\0L\0\nparametersq\0~\0L\0pathInfoq\0~\0L\0queryStringq\0~\0L\0\nrequestURIq\0~\0L\0\nrequestURLq\0~\0L\0schemeq\0~\0L\0\nserverNameq\0~\0L\0servletPathq\0~\0xp\0\0êt\0\0sr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sr\09org.springframework.security.web.savedrequest.SavedCookie\0\0\0\0\0\0l\0I\0maxAgeZ\0secureI\0versionL\0commentq\0~\0L\0domainq\0~\0L\0nameq\0~\0L\0pathq\0~\0L\0valueq\0~\0xpˇˇˇˇ\0\0\0\0\0ppt\0SESSIONpt\00N2M0MWZmMDMtNTIxYS00Y2ZiLWI5N2QtMjZlYWNlMjNlODZhxsr\0java.util.TreeMap¡ˆ>-%j\Ê\0L\0\ncomparatort\0Ljava/util/Comparator;xpsr\0*java.lang.String$CaseInsensitiveComparatorw\\}\\P\Â\Œ\0\0xpw\0\0\0t\0acceptsq\0~\0\0\0\0w\0\0\0t\0*/*xt\0accept-encodingsq\0~\0\0\0\0w\0\0\0t\0gzip, deflate, br, zstdxt\0accept-languagesq\0~\0\0\0\0w\0\0\0t\0es-419,es;q=0.9xt\0\nconnectionsq\0~\0\0\0\0w\0\0\0t\0\nkeep-alivext\0cookiesq\0~\0\0\0\0w\0\0\0t\08SESSION=N2M0MWZmMDMtNTIxYS00Y2ZiLWI5N2QtMjZlYWNlMjNlODZhxt\0hostsq\0~\0\0\0\0w\0\0\0t\0localhost:8080xt\0referersq\0~\0\0\0\0w\0\0\0t\0http://localhost:8080/loginxt\0	sec-ch-uasq\0~\0\0\0\0w\0\0\0t\0A\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Microsoft Edge\";v=\"138\"xt\0sec-ch-ua-mobilesq\0~\0\0\0\0w\0\0\0t\0?0xt\0sec-ch-ua-platformsq\0~\0\0\0\0w\0\0\0t\0	\"Windows\"xt\0sec-fetch-destsq\0~\0\0\0\0w\0\0\0t\0emptyxt\0sec-fetch-modesq\0~\0\0\0\0w\0\0\0t\0corsxt\0sec-fetch-sitesq\0~\0\0\0\0w\0\0\0t\0same-originxt\0\nuser-agentsq\0~\0\0\0\0w\0\0\0t\0}Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0xxsq\0~\0\0\0\0w\0\0\0sr\0java.util.Locale~¯`ú0˘\Ï\0I\0hashcodeL\0countryq\0~\0L\0\nextensionsq\0~\0L\0languageq\0~\0L\0scriptq\0~\0L\0variantq\0~\0xpˇˇˇˇt\0419q\0~\0t\0esq\0~\0q\0~\0xsq\0~\0<ˇˇˇˇq\0~\0q\0~\0q\0~\0?q\0~\0q\0~\0xxt\0continuet\0GETsq\0~\0pw\0\0\0\0xppt\0/json/locales/en.jsont\0*http://localhost:8080/json/locales/en.jsont\0httpt\0	localhostt\0/json/locales/en.json'),('4d8da0b3-ee84-452f-9bbf-82ec0fdf92a2','usuario',_binary '¨\Ì\0sr\0#com.example.project.entity.Usuarios\0\0\0\0\0\0\0\0\rL\0	apellidost\0Ljava/lang/String;L\0confirmContrasenaq\0~\0L\0\ncontrasenaq\0~\0L\0correoq\0~\0L\0dniq\0~\0L\0estadot\0&Lcom/example/project/entity/EstadoUsu;L\0\rfechaCreaciont\0Ljava/sql/Timestamp;L\0\nfotoPerfilq\0~\0L\0\nidUsuariost\0Ljava/lang/Integer;L\0lugaresAsignadost\0Ljava/util/List;L\0nombresq\0~\0L\0rolt\0 Lcom/example/project/entity/Rol;L\0telefonoq\0~\0xpt\0Godoypt\0<$2a$10$JcUVX/2V/tpH8pk20dkHc.23lOFp78u0zS8RfVa/TgtLDev.3iiKet\0juanielulloavega2@gmail.comt\020206311sr\0$com.example.project.entity.EstadoUsu\0\0\0\0\0\0\0\0L\0estadoq\0~\0L\0idEstadoq\0~\0xpt\0Activosr\0java.lang.Integer‚†§˜Åá8\0I\0valuexr\0java.lang.NumberÜ¨ïî\‡ã\0\0xp\0\0\0sr\0java.sql.Timestamp&\’\»Søe\0I\0nanosxr\0java.util.DatehjÅKYt\0\0xpw\0\0ñ\⁄Ÿö\0x\0\0\0\0psq\0~\0\0\0\0sr\0*org.hibernate.collection.spi.PersistentBagñ<a#:rAí\0L\0bagq\0~\0L\0providedCollectiont\0Ljava/util/Collection;xr\09org.hibernate.collection.spi.AbstractPersistentCollection3§∞J<F\0Z\0allowLoadOutsideTransactionI\0\ncachedSizeZ\0dirtyZ\0elementRemovedZ\0initializedZ\0\risTempSessionL\0keyt\0Ljava/lang/Object;L\0ownerq\0~\0L\0roleq\0~\0L\0sessionFactoryUuidq\0~\0L\0storedSnapshott\0Ljava/io/Serializable;xp\0ˇˇˇˇ\0\0\0\0q\0~\0q\0~\0t\04com.example.project.entity.Usuarios.lugaresAsignadosppppt\0Fernandosr\0com.example.project.entity.Rol\0\0\0\0\0\0\0\0L\0idRolq\0~\0L\0rolq\0~\0xpq\0~\0t\0Coordinadort\0	987654322'),('5052ebc2-f1ac-4f34-873d-01962b098470','idConversacionActual',_binary '¨\Ì\0t\0$5cdcfeeb-5060-4e33-8ef8-649e7884d8f5'),('5052ebc2-f1ac-4f34-873d-01962b098470','org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN',_binary '¨\Ì\0sr\06org.springframework.security.web.csrf.DefaultCsrfTokenZ\Ô∑\»/¢˚\’\0L\0\nheaderNamet\0Ljava/lang/String;L\0\rparameterNameq\0~\0L\0tokenq\0~\0xpt\0X-CSRF-TOKENt\0_csrft\0$6e996659-58ff-41a7-b9c3-e067cc08a91c'),('5052ebc2-f1ac-4f34-873d-01962b098470','SPRING_SECURITY_CONTEXT',_binary '¨\Ì\0sr\0=org.springframework.security.core.context.SecurityContextImpl\0\0\0\0\0\0l\0L\0authenticationt\02Lorg/springframework/security/core/Authentication;xpsr\0Oorg.springframework.security.authentication.UsernamePasswordAuthenticationToken\0\0\0\0\0\0l\0L\0credentialst\0Ljava/lang/Object;L\0	principalq\0~\0xr\0Gorg.springframework.security.authentication.AbstractAuthenticationToken”™(~nGd\0Z\0\rauthenticatedL\0authoritiest\0Ljava/util/Collection;L\0detailsq\0~\0xpsr\0&java.util.Collections$UnmodifiableList¸%1µ\Ïé\0L\0listt\0Ljava/util/List;xr\0,java.util.Collections$UnmodifiableCollectionB\0Ä\À^˜\0L\0cq\0~\0xpsr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sr\0Borg.springframework.security.core.authority.SimpleGrantedAuthority\0\0\0\0\0\0l\0L\0rolet\0Ljava/lang/String;xpt\0ROLE_Usuario finalxq\0~\0\rsr\0Horg.springframework.security.web.authentication.WebAuthenticationDetails\0\0\0\0\0\0l\0L\0\rremoteAddressq\0~\0L\0	sessionIdq\0~\0xpt\00:0:0:0:0:0:0:1t\0$c104cc41-1263-4d0a-ba02-a2590d94a60bpsr\02org.springframework.security.core.userdetails.User\0\0\0\0\0\0l\0Z\0accountNonExpiredZ\0accountNonLockedZ\0credentialsNonExpiredZ\0enabledL\0authoritiest\0Ljava/util/Set;L\0passwordq\0~\0L\0usernameq\0~\0xpsr\0%java.util.Collections$UnmodifiableSetÄí—èõÄU\0\0xq\0~\0\nsr\0java.util.TreeSet›òPìï\Ìá[\0\0xpsr\0Forg.springframework.security.core.userdetails.User$AuthorityComparator\0\0\0\0\0\0l\0\0xpw\0\0\0q\0~\0xpt\076608126'),('5052ebc2-f1ac-4f34-873d-01962b098470','SPRING_SECURITY_SAVED_REQUEST',_binary '¨\Ì\0sr\0Aorg.springframework.security.web.savedrequest.DefaultSavedRequest\0\0\0\0\0\0l\0I\0\nserverPortL\0contextPatht\0Ljava/lang/String;L\0cookiest\0Ljava/util/ArrayList;L\0headerst\0Ljava/util/Map;L\0localesq\0~\0L\0matchingRequestParameterNameq\0~\0L\0methodq\0~\0L\0\nparametersq\0~\0L\0pathInfoq\0~\0L\0queryStringq\0~\0L\0\nrequestURIq\0~\0L\0\nrequestURLq\0~\0L\0schemeq\0~\0L\0\nserverNameq\0~\0L\0servletPathq\0~\0xp\0\0êt\0\0sr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sr\09org.springframework.security.web.savedrequest.SavedCookie\0\0\0\0\0\0l\0I\0maxAgeZ\0secureI\0versionL\0commentq\0~\0L\0domainq\0~\0L\0nameq\0~\0L\0pathq\0~\0L\0valueq\0~\0xpˇˇˇˇ\0\0\0\0\0ppt\0_omappvppt\0`9H3RoDQFKq0HkhoIArvYwZF6YzLu4fZvlkLeFre9maawNjEqAylkQvM7cQcWwZR9Cd50nnK33OwengivqQUnoR8EGNM5zzbNsq\0~\0ˇˇˇˇ\0\0\0\0\0ppt\0_omrapt\0-%7B%22mmosgju0tkru8ajiwhig%22%3A%22view%22%7Dsq\0~\0ˇˇˇˇ\0\0\0\0\0ppt\0\rIdea-3d6b315apt\0$312b8825-2076-4231-8500-49d531a121e6sq\0~\0ˇˇˇˇ\0\0\0\0\0ppt\0SESSIONpt\00YzEwNGNjNDEtMTI2My00ZDBhLWJhMDItYTI1OTBkOTRhNjBixsr\0java.util.TreeMap¡ˆ>-%j\Ê\0L\0\ncomparatort\0Ljava/util/Comparator;xpsr\0*java.lang.String$CaseInsensitiveComparatorw\\}\\P\Â\Œ\0\0xpw\0\0\0t\0acceptsq\0~\0\0\0\0w\0\0\0t\0*/*xt\0accept-encodingsq\0~\0\0\0\0w\0\0\0t\0gzip, deflate, br, zstdxt\0accept-languagesq\0~\0\0\0\0w\0\0\0t\0<es-419,es;q=0.9,en;q=0.8,en-US;q=0.7,es-PE;q=0.6,es-ES;q=0.5xt\0\nconnectionsq\0~\0\0\0\0w\0\0\0t\0\nkeep-alivext\0cookiesq\0~\0\0\0\0w\0\0\0t_omappvp=9H3RoDQFKq0HkhoIArvYwZF6YzLu4fZvlkLeFre9maawNjEqAylkQvM7cQcWwZR9Cd50nnK33OwengivqQUnoR8EGNM5zzbN; _omra=%7B%22mmosgju0tkru8ajiwhig%22%3A%22view%22%7D; Idea-3d6b315a=312b8825-2076-4231-8500-49d531a121e6; SESSION=YzEwNGNjNDEtMTI2My00ZDBhLWJhMDItYTI1OTBkOTRhNjBixt\0hostsq\0~\0\0\0\0w\0\0\0t\0localhost:8080xt\0referersq\0~\0\0\0\0w\0\0\0t\0http://localhost:8080/loginxt\0	sec-ch-uasq\0~\0\0\0\0w\0\0\0t\0A\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Microsoft Edge\";v=\"138\"xt\0sec-ch-ua-mobilesq\0~\0\0\0\0w\0\0\0t\0?0xt\0sec-ch-ua-platformsq\0~\0\0\0\0w\0\0\0t\0	\"Windows\"xt\0sec-fetch-destsq\0~\0\0\0\0w\0\0\0t\0emptyxt\0sec-fetch-modesq\0~\0\0\0\0w\0\0\0t\0corsxt\0sec-fetch-sitesq\0~\0\0\0\0w\0\0\0t\0same-originxt\0\nuser-agentsq\0~\0\0\0\0w\0\0\0t\0}Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0xxsq\0~\0\0\0\0w\0\0\0sr\0java.util.Locale~¯`ú0˘\Ï\0I\0hashcodeL\0countryq\0~\0L\0\nextensionsq\0~\0L\0languageq\0~\0L\0scriptq\0~\0L\0variantq\0~\0xpˇˇˇˇt\0419q\0~\0t\0esq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇq\0~\0q\0~\0q\0~\0Hq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇq\0~\0q\0~\0t\0enq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇt\0USq\0~\0q\0~\0Kq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇt\0PEq\0~\0q\0~\0Hq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇt\0ESq\0~\0q\0~\0Hq\0~\0q\0~\0xxt\0continuet\0GETsq\0~\0pw\0\0\0\0xppt\0/json/locales/en.jsont\0*http://localhost:8080/json/locales/en.jsont\0httpt\0	localhostt\0/json/locales/en.json'),('5052ebc2-f1ac-4f34-873d-01962b098470','usuario',_binary '¨\Ì\0sr\0#com.example.project.entity.Usuarios\0\0\0\0\0\0\0\0\rL\0	apellidost\0Ljava/lang/String;L\0confirmContrasenaq\0~\0L\0\ncontrasenaq\0~\0L\0correoq\0~\0L\0dniq\0~\0L\0estadot\0&Lcom/example/project/entity/EstadoUsu;L\0\rfechaCreaciont\0Ljava/sql/Timestamp;L\0\nfotoPerfilq\0~\0L\0\nidUsuariost\0Ljava/lang/Integer;L\0lugaresAsignadost\0Ljava/util/List;L\0nombresq\0~\0L\0rolt\0 Lcom/example/project/entity/Rol;L\0telefonoq\0~\0xpt\0Ulloapt\0<$2a$10$S6eOEeuA3oiDjNdunnU/W.bDVGyr09G9cqio0T./Yfn/2sc7Nb.sat\0juanulloavega3@gmail.comt\076608126sr\0$com.example.project.entity.EstadoUsu\0\0\0\0\0\0\0\0L\0estadoq\0~\0L\0idEstadoq\0~\0xpt\0Activosr\0java.lang.Integer‚†§˜Åá8\0I\0valuexr\0java.lang.NumberÜ¨ïî\‡ã\0\0xp\0\0\0sr\0java.sql.Timestamp&\’\»Søe\0I\0nanosxr\0java.util.DatehjÅKYt\0\0xpw\0\0ñ\⁄Ÿö\0x\0\0\0\0psq\0~\0\0\0\0sr\0*org.hibernate.collection.spi.PersistentBagñ<a#:rAí\0L\0bagq\0~\0L\0providedCollectiont\0Ljava/util/Collection;xr\09org.hibernate.collection.spi.AbstractPersistentCollection3§∞J<F\0Z\0allowLoadOutsideTransactionI\0\ncachedSizeZ\0dirtyZ\0elementRemovedZ\0initializedZ\0\risTempSessionL\0keyt\0Ljava/lang/Object;L\0ownerq\0~\0L\0roleq\0~\0L\0sessionFactoryUuidq\0~\0L\0storedSnapshott\0Ljava/io/Serializable;xp\0ˇˇˇˇ\0\0\0\0q\0~\0q\0~\0t\04com.example.project.entity.Usuarios.lugaresAsignadosppppt\0Juansr\0com.example.project.entity.Rol\0\0\0\0\0\0\0\0L\0idRolq\0~\0L\0rolq\0~\0xpq\0~\0t\0\rUsuario finalt\0	925253151'),('8de093c4-6dd0-4a15-b7dd-23a7667b725d','idConversacionActual',_binary '¨\Ì\0t\0$57b7cb14-1d5b-4ba2-9de2-a3df6ddb4911'),('8de093c4-6dd0-4a15-b7dd-23a7667b725d','org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN',_binary '¨\Ì\0sr\06org.springframework.security.web.csrf.DefaultCsrfTokenZ\Ô∑\»/¢˚\’\0L\0\nheaderNamet\0Ljava/lang/String;L\0\rparameterNameq\0~\0L\0tokenq\0~\0xpt\0X-CSRF-TOKENt\0_csrft\0$48ee722b-8b1e-4055-8b5b-047ea2f32806'),('8de093c4-6dd0-4a15-b7dd-23a7667b725d','SPRING_SECURITY_CONTEXT',_binary '¨\Ì\0sr\0=org.springframework.security.core.context.SecurityContextImpl\0\0\0\0\0\0l\0L\0authenticationt\02Lorg/springframework/security/core/Authentication;xpsr\0Oorg.springframework.security.authentication.UsernamePasswordAuthenticationToken\0\0\0\0\0\0l\0L\0credentialst\0Ljava/lang/Object;L\0	principalq\0~\0xr\0Gorg.springframework.security.authentication.AbstractAuthenticationToken”™(~nGd\0Z\0\rauthenticatedL\0authoritiest\0Ljava/util/Collection;L\0detailsq\0~\0xpsr\0&java.util.Collections$UnmodifiableList¸%1µ\Ïé\0L\0listt\0Ljava/util/List;xr\0,java.util.Collections$UnmodifiableCollectionB\0Ä\À^˜\0L\0cq\0~\0xpsr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sr\0Borg.springframework.security.core.authority.SimpleGrantedAuthority\0\0\0\0\0\0l\0L\0rolet\0Ljava/lang/String;xpt\0ROLE_Coordinadorxq\0~\0\rsr\0Horg.springframework.security.web.authentication.WebAuthenticationDetails\0\0\0\0\0\0l\0L\0\rremoteAddressq\0~\0L\0	sessionIdq\0~\0xpt\00:0:0:0:0:0:0:1t\0$dd245245-5245-4a2e-ad16-b93ef91db27dpsr\02org.springframework.security.core.userdetails.User\0\0\0\0\0\0l\0Z\0accountNonExpiredZ\0accountNonLockedZ\0credentialsNonExpiredZ\0enabledL\0authoritiest\0Ljava/util/Set;L\0passwordq\0~\0L\0usernameq\0~\0xpsr\0%java.util.Collections$UnmodifiableSetÄí—èõÄU\0\0xq\0~\0\nsr\0java.util.TreeSet›òPìï\Ìá[\0\0xpsr\0Forg.springframework.security.core.userdetails.User$AuthorityComparator\0\0\0\0\0\0l\0\0xpw\0\0\0q\0~\0xpt\020206311'),('8de093c4-6dd0-4a15-b7dd-23a7667b725d','SPRING_SECURITY_SAVED_REQUEST',_binary '¨\Ì\0sr\0Aorg.springframework.security.web.savedrequest.DefaultSavedRequest\0\0\0\0\0\0l\0I\0\nserverPortL\0contextPatht\0Ljava/lang/String;L\0cookiest\0Ljava/util/ArrayList;L\0headerst\0Ljava/util/Map;L\0localesq\0~\0L\0matchingRequestParameterNameq\0~\0L\0methodq\0~\0L\0\nparametersq\0~\0L\0pathInfoq\0~\0L\0queryStringq\0~\0L\0\nrequestURIq\0~\0L\0\nrequestURLq\0~\0L\0schemeq\0~\0L\0\nserverNameq\0~\0L\0servletPathq\0~\0xp\0\0êt\0\0sr\0java.util.ArrayListxÅ\“ô\«aù\0I\0sizexp\0\0\0w\0\0\0sr\09org.springframework.security.web.savedrequest.SavedCookie\0\0\0\0\0\0l\0I\0maxAgeZ\0secureI\0versionL\0commentq\0~\0L\0domainq\0~\0L\0nameq\0~\0L\0pathq\0~\0L\0valueq\0~\0xpˇˇˇˇ\0\0\0\0\0ppt\0_omappvppt\0`9H3RoDQFKq0HkhoIArvYwZF6YzLu4fZvlkLeFre9maawNjEqAylkQvM7cQcWwZR9Cd50nnK33OwengivqQUnoR8EGNM5zzbNsq\0~\0ˇˇˇˇ\0\0\0\0\0ppt\0_omrapt\0-%7B%22mmosgju0tkru8ajiwhig%22%3A%22view%22%7Dsq\0~\0ˇˇˇˇ\0\0\0\0\0ppt\0\rIdea-3d6b315apt\0$312b8825-2076-4231-8500-49d531a121e6sq\0~\0ˇˇˇˇ\0\0\0\0\0ppt\0SESSIONpt\00ZGQyNDUyNDUtNTI0NS00YTJlLWFkMTYtYjkzZWY5MWRiMjdkxsr\0java.util.TreeMap¡ˆ>-%j\Ê\0L\0\ncomparatort\0Ljava/util/Comparator;xpsr\0*java.lang.String$CaseInsensitiveComparatorw\\}\\P\Â\Œ\0\0xpw\0\0\0t\0acceptsq\0~\0\0\0\0w\0\0\0t\0*/*xt\0accept-encodingsq\0~\0\0\0\0w\0\0\0t\0gzip, deflate, br, zstdxt\0accept-languagesq\0~\0\0\0\0w\0\0\0t\0<es-419,es;q=0.9,en;q=0.8,en-US;q=0.7,es-PE;q=0.6,es-ES;q=0.5xt\0\nconnectionsq\0~\0\0\0\0w\0\0\0t\0\nkeep-alivext\0cookiesq\0~\0\0\0\0w\0\0\0t_omappvp=9H3RoDQFKq0HkhoIArvYwZF6YzLu4fZvlkLeFre9maawNjEqAylkQvM7cQcWwZR9Cd50nnK33OwengivqQUnoR8EGNM5zzbN; _omra=%7B%22mmosgju0tkru8ajiwhig%22%3A%22view%22%7D; Idea-3d6b315a=312b8825-2076-4231-8500-49d531a121e6; SESSION=ZGQyNDUyNDUtNTI0NS00YTJlLWFkMTYtYjkzZWY5MWRiMjdkxt\0hostsq\0~\0\0\0\0w\0\0\0t\0localhost:8080xt\0referersq\0~\0\0\0\0w\0\0\0t\0http://localhost:8080/loginxt\0	sec-ch-uasq\0~\0\0\0\0w\0\0\0t\0A\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Microsoft Edge\";v=\"138\"xt\0sec-ch-ua-mobilesq\0~\0\0\0\0w\0\0\0t\0?0xt\0sec-ch-ua-platformsq\0~\0\0\0\0w\0\0\0t\0	\"Windows\"xt\0sec-fetch-destsq\0~\0\0\0\0w\0\0\0t\0emptyxt\0sec-fetch-modesq\0~\0\0\0\0w\0\0\0t\0corsxt\0sec-fetch-sitesq\0~\0\0\0\0w\0\0\0t\0same-originxt\0\nuser-agentsq\0~\0\0\0\0w\0\0\0t\0}Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0xxsq\0~\0\0\0\0w\0\0\0sr\0java.util.Locale~¯`ú0˘\Ï\0I\0hashcodeL\0countryq\0~\0L\0\nextensionsq\0~\0L\0languageq\0~\0L\0scriptq\0~\0L\0variantq\0~\0xpˇˇˇˇt\0419q\0~\0t\0esq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇq\0~\0q\0~\0q\0~\0Hq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇq\0~\0q\0~\0t\0enq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇt\0USq\0~\0q\0~\0Kq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇt\0PEq\0~\0q\0~\0Hq\0~\0q\0~\0xsq\0~\0Eˇˇˇˇt\0ESq\0~\0q\0~\0Hq\0~\0q\0~\0xxt\0continuet\0GETsq\0~\0pw\0\0\0\0xppt\0/json/locales/en.jsont\0*http://localhost:8080/json/locales/en.jsont\0httpt\0	localhostt\0/json/locales/en.json'),('8de093c4-6dd0-4a15-b7dd-23a7667b725d','usuario',_binary '¨\Ì\0sr\0#com.example.project.entity.Usuarios\0\0\0\0\0\0\0\0\rL\0	apellidost\0Ljava/lang/String;L\0confirmContrasenaq\0~\0L\0\ncontrasenaq\0~\0L\0correoq\0~\0L\0dniq\0~\0L\0estadot\0&Lcom/example/project/entity/EstadoUsu;L\0\rfechaCreaciont\0Ljava/sql/Timestamp;L\0\nfotoPerfilq\0~\0L\0\nidUsuariost\0Ljava/lang/Integer;L\0lugaresAsignadost\0Ljava/util/List;L\0nombresq\0~\0L\0rolt\0 Lcom/example/project/entity/Rol;L\0telefonoq\0~\0xpt\0Godoypt\0<$2a$10$JcUVX/2V/tpH8pk20dkHc.23lOFp78u0zS8RfVa/TgtLDev.3iiKet\0juanielulloavega2@gmail.comt\020206311sr\0$com.example.project.entity.EstadoUsu\0\0\0\0\0\0\0\0L\0estadoq\0~\0L\0idEstadoq\0~\0xpt\0Activosr\0java.lang.Integer‚†§˜Åá8\0I\0valuexr\0java.lang.NumberÜ¨ïî\‡ã\0\0xp\0\0\0sr\0java.sql.Timestamp&\’\»Søe\0I\0nanosxr\0java.util.DatehjÅKYt\0\0xpw\0\0ñ\⁄Ÿö\0x\0\0\0\0psq\0~\0\0\0\0sr\0*org.hibernate.collection.spi.PersistentBagñ<a#:rAí\0L\0bagq\0~\0L\0providedCollectiont\0Ljava/util/Collection;xr\09org.hibernate.collection.spi.AbstractPersistentCollection3§∞J<F\0Z\0allowLoadOutsideTransactionI\0\ncachedSizeZ\0dirtyZ\0elementRemovedZ\0initializedZ\0\risTempSessionL\0keyt\0Ljava/lang/Object;L\0ownerq\0~\0L\0roleq\0~\0L\0sessionFactoryUuidq\0~\0L\0storedSnapshott\0Ljava/io/Serializable;xp\0ˇˇˇˇ\0\0\0\0q\0~\0q\0~\0t\04com.example.project.entity.Usuarios.lugaresAsignadosppppt\0Fernandosr\0com.example.project.entity.Rol\0\0\0\0\0\0\0\0L\0idRolq\0~\0L\0rolq\0~\0xpq\0~\0t\0Coordinadort\0	987654322');
/*!40000 ALTER TABLE `spring_session_attributes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tipoespacio`
--

DROP TABLE IF EXISTS `tipoespacio`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tipoespacio` (
  `idTipoEspacio` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  PRIMARY KEY (`idTipoEspacio`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tipoespacio`
--

LOCK TABLES `tipoespacio` WRITE;
/*!40000 ALTER TABLE `tipoespacio` DISABLE KEYS */;
INSERT INTO `tipoespacio` VALUES (1,'Cancha de f√∫tbol - Grass Sint√©tico'),(2,'Cancha de f√∫tbol - Loza'),(3,'Piscina'),(4,'Pista de Atletismo');
/*!40000 ALTER TABLE `tipoespacio` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuarios`
--

DROP TABLE IF EXISTS `usuarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios` (
  `idUsuarios` int NOT NULL AUTO_INCREMENT,
  `nombres` varchar(45) DEFAULT NULL,
  `apellidos` varchar(45) DEFAULT NULL,
  `dni` varchar(8) NOT NULL,
  `correo` varchar(100) DEFAULT NULL,
  `contrasena` varchar(100) DEFAULT NULL,
  `rol` int NOT NULL,
  `estado` int NOT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `foto_perfil` varchar(255) DEFAULT NULL,
  `fechaCreacion` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`idUsuarios`),
  UNIQUE KEY `dni` (`dni`),
  UNIQUE KEY `telefono` (`telefono`),
  UNIQUE KEY `telefono_2` (`telefono`),
  KEY `fk_Usuarios_Rol_idx` (`rol`),
  KEY `fk_Usuarios_Estado1_idx` (`estado`),
  CONSTRAINT `fk_Usuarios_Estado1` FOREIGN KEY (`estado`) REFERENCES `estadousu` (`idEstado`),
  CONSTRAINT `fk_Usuarios_Rol` FOREIGN KEY (`rol`) REFERENCES `rol` (`idRol`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuarios`
--

LOCK TABLES `usuarios` WRITE;
/*!40000 ALTER TABLE `usuarios` DISABLE KEYS */;
INSERT INTO `usuarios` VALUES (1,'Daniel','Ulloa','20193265','a20193265@pucp.edu.pe','$2a$10$QlUn5iKzdK64.mef3jGcLeBlnruee9h0PXeKNjl4LGZtKqiklsrIy',1,1,'999999999',NULL,'2025-05-16 20:46:56'),(2,'Fernando','Godoy','20206311','juanielulloavega2@gmail.com','$2a$10$JcUVX/2V/tpH8pk20dkHc.23lOFp78u0zS8RfVa/TgtLDev.3iiKe',2,1,'987654322',NULL,'2025-05-16 20:46:56'),(3,'Juan','Ulloa','20206452','a20206452@pucp.edu.pe','$2a$10$m9VC1k5fJ6IEt268icoPo.GNAyNydV41SRcKe60wUUFT9QBlXDwKW',4,1,'987654323',NULL,'2025-05-16 20:46:56'),(4,'Jeanpier','Gutierrez','20213805','a20213805@pucp.edu.pe','$2a$10$Ki3lIDRtwkO7K9mAC4r.GOiEYYR2QY3xXRHE3pkGm/VEBxrshkFmm',3,1,'987654324',NULL,'2025-05-16 20:46:56'),(7,'Juan','Ulloa','76608126','juanulloavega3@gmail.com','$2a$10$S6eOEeuA3oiDjNdunnU/W.bDVGyr09G9cqio0T./Yfn/2sc7Nb.sa',1,1,'925253151',NULL,'2025-05-16 20:46:56'),(22,'JUAN MANUEL','ULLOA JARA','10740498','juaniel_7@hotmail.com','$2a$10$w1jxAyoqNbffoXPxnZKVVup36Fsvf7t4GTGs/t/Ybce9oX6l/uWuu',1,1,'',NULL,'2025-07-31 06:02:17');
/*!40000 ALTER TABLE `usuarios` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-09-09 12:58:49
