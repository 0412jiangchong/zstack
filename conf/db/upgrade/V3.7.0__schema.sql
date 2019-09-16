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
