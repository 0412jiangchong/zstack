CREATE TABLE IF NOT EXISTS `InstallPathRecycleVO` (
    `trashId` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `resourceUuid` varchar(32) NOT NULL,
    `resourceType` varchar(32) NOT NULL,
    `storageUuid` varchar(32) NOT NULL,
    `storageType` varchar(32) NOT NULL,
    `installPath` varchar(1024) NOT NULL,
    `hypervisorType` varchar(32) DEFAULT NULL,
    `trashType` varchar(32) NOT NULL,
    `isFolder` boolean NOT NULL DEFAULT FALSE,
    `size` bigint unsigned NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`trashId`),
    UNIQUE KEY `trashId` (`trashId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VmPriorityConfigVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `level` VARCHAR(255) NOT NULL UNIQUE,
    `cpuShares` int NOT NULL,
    `oomScoreAdj` int NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `SNSEmailEndpointVO` modify column email varchar(1024) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`SNSEmailAddressVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `emailAddress` varchar(1024) NOT NULL,
    `endpointUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS upgradeEmailAddressFromEndpoint;

DELIMITER $$
CREATE PROCEDURE upgradeEmailAddressFromEndpoint()
    BEGIN
        DECLARE email_address varchar(1024);
        DECLARE endpoint_uuid varchar(32);
        DECLARE email_address_count INT DEFAULT 0;
        DECLARE email_address_uuid varchar(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT uuid, email FROM zstack.SNSEmailEndpointVO WHERE `email` IS NOT NULL;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO endpoint_uuid, email_address;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT count(*) INTO email_address_count FROM zstack.SNSEmailAddressVO WHERE emailAddress = email_address and endpointUuid = endpoint_uuid;

            IF (email_address_count = 0) THEN
                SET email_address_uuid = REPLACE(UUID(), '-', '');

                INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`)
                VALUES (email_address_uuid, NULL, 'SNSEmailAddressVO', 'org.zstack.sns.platform.email.SNSEmailAddressVO');

                INSERT INTO `SNSEmailAddressVO` (`uuid`, `emailAddress`, `endpointUuid`, `createDate`, `lastOpDate`)
                VALUES (email_address_uuid, email_address, endpoint_uuid, NOW(), NOW());
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

CALL upgradeEmailAddressFromEndpoint();
DROP PROCEDURE IF EXISTS upgradeEmailAddressFromEndpoint;

UPDATE zstack.SNSEmailEndpointVO SET email = NULL;

-- ----------------------------
--  add h3c hardware sdn
-- ----------------------------
CREATE TABLE IF NOT EXISTS `SdnControllerVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `vendorType` VARCHAR(255) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `ip` VARCHAR(255) NOT NULL,
    `username` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  IF NOT EXISTS `HardwareL2VxlanNetworkPoolVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `sdnControllerUuid` VARCHAR(32) NOT NULL,
  PRIMARY KEY  (`uuid`),
  CONSTRAINT fkHardwareL2VxlanNetworkPoolVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
