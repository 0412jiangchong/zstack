# Add primary key to PrimaryStorageHostRefVO and make SharedBlockGroupPrimaryStorageHostRefVO inherit it

ALTER TABLE PrimaryStorageHostRefVO ADD id BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT;
ALTER TABLE PrimaryStorageHostRefVO DROP FOREIGN KEY fkPrimaryStorageHostRefVOHostEO, DROP FOREIGN KEY fkPrimaryStorageHostRefVOPrimaryStorageEO;
ALTER TABLE PrimaryStorageHostRefVO DROP PRIMARY KEY, ADD PRIMARY KEY ( `id` );
ALTER TABLE PrimaryStorageHostRefVO ADD CONSTRAINT fkPrimaryStorageHostRefVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE;
ALTER TABLE PrimaryStorageHostRefVO ADD CONSTRAINT fkPrimaryStorageHostRefVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;
INSERT INTO PrimaryStorageHostRefVO (primaryStorageUuid, hostUuid, status, lastOpDate, createDate) SELECT s.primaryStorageUuid, s.hostUuid, s.status, s.lastOpDate, s.createDate FROM SharedBlockGroupPrimaryStorageHostRefVO s;

ALTER TABLE SharedBlockGroupPrimaryStorageHostRefVO DROP FOREIGN KEY fkSharedBlockGroupPrimaryStorageHostRefVOPrimaryStorageEO, DROP FOREIGN KEY fkSharedBlockGroupPrimaryStorageHostRefVOHostEO;
ALTER TABLE SharedBlockGroupPrimaryStorageHostRefVO DROP INDEX ukSharedBlockGroupPrimaryStorageHostRefVO;
ALTER TABLE SharedBlockGroupPrimaryStorageHostRefVO ADD id BIGINT UNSIGNED UNIQUE;
UPDATE SharedBlockGroupPrimaryStorageHostRefVO s, PrimaryStorageHostRefVO p SET s.id = p.id WHERE s.primaryStorageUuid = p.primaryStorageUuid AND s.hostUuid = p.hostUuid;
ALTER TABLE SharedBlockGroupPrimaryStorageHostRefVO ADD CONSTRAINT fkSharedBlockGroupPrimaryStorageHostRefVOPrimaryStorageHostRefVO FOREIGN KEY (id) REFERENCES PrimaryStorageHostRefVO (id) ON DELETE CASCADE;
ALTER TABLE SharedBlockGroupPrimaryStorageHostRefVO DROP PRIMARY KEY, ADD PRIMARY KEY ( `id` );
ALTER TABLE SharedBlockGroupPrimaryStorageHostRefVO DROP COLUMN primaryStorageUuid, DROP COLUMN hostUuid, DROP COLUMN status, DROP COLUMN lastOpDate, DROP COLUMN createDate;

-- ----------------------------
--  For unattended baremetal provisioning
-- ----------------------------
CREATE TABLE `PreconfigurationTemplateVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `distribution` VARCHAR(64) NOT NULL,
    `type` VARCHAR(32) NOT NULL,
    `content` MEDIUMTEXT NOT NULL,
    `md5sum` VARCHAR(255) NOT NULL,
    `isPredefined` TINYINT(1) UNSIGNED DEFAULT 0,
    `state` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TemplateCustomParamVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `templateUuid` VARCHAR(32) NOT NULL,
    `param` VARCHAR(255) NOT NULL,
    CONSTRAINT fkTemplateCustomParamVOPreconfigurationTemplateVO FOREIGN KEY (templateUuid) REFERENCES PreconfigurationTemplateVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `CustomPreconfigurationVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `baremetalInstanceUuid` VARCHAR(32) NOT NULL,
    `param` VARCHAR(255) NOT NULL,
    `value` TEXT NOT NULL,
    CONSTRAINT fkCustomPreconfigurationVOBaremetalInstanceVO FOREIGN KEY (baremetalInstanceUuid) REFERENCES BaremetalInstanceVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- we don't know mac address of the bm vlan nic
ALTER TABLE `BaremetalNicVO` DROP INDEX `mac`;
ALTER TABLE `BaremetalNicVO` MODIFY `mac` varchar(17) DEFAULT NULL;
ALTER TABLE `BaremetalNicVO` ADD COLUMN `baremetalBondingUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `BaremetalNicVO` ADD CONSTRAINT `ukBaremetalNicVO` UNIQUE (`mac`,`baremetalBondingUuid`);

CREATE TABLE  `BaremetalVlanNicVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vlan` int unsigned NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE BaremetalVlanNicVO ADD CONSTRAINT fkBaremetalVlanNicVOBaremetalNicVO FOREIGN KEY (uuid) REFERENCES BaremetalNicVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

CREATE TABLE `BaremetalBondingVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `chassisUuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `mode` TINYINT UNSIGNED NOT NULL,
    `slaves` VARCHAR(2048) NOT NULL,
    `opts` VARCHAR(1024) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `BaremetalInstanceVO` ADD COLUMN `templateUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `BaremetalInstanceVO` ADD CONSTRAINT `fkBaremetalInstanceVOPreconfigurationTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `PreconfigurationTemplateVO` (`uuid`) ON DELETE SET NULL;

CREATE INDEX idxVmUuid ON VmUsageVO(vmUuid) USING BTREE;

DELIMITER $$
CREATE PROCEDURE cleanExpireVmUsageVO()
		BEGIN
				DECLARE done INT DEFAULT FALSE;
			  DECLARE vmUuid VARCHAR(32);
				DECLARE name VARCHAR(255);
				DECLARE accountUuid VARCHAR(32);
				DECLARE cpuNum INT(10);
				DECLARE state VARCHAR(64);
				DECLARE memorySize BIGINT(20);
				DECLARE rootVolumeSize BIGINT(20);
			  DECLARE inventory Text;
				DECLARE lastOpDate TIMESTAMP;
				DEClARE cur CURSOR FOR SELECT v.vmUuid,v.name,v.accountUuid,v.state,v.cpuNum,v.memorySize,v.rootVolumeSize,v.inventory from VmUsageVO v
								where v.id IN (select MAX(a.id) FROM VmUsageVO a GROUP BY a.vmUuid)
								AND v.vmUuid NOT IN (select DISTINCT uuid from VmInstanceEO) AND v.state = 'Running';
				DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
			  OPEN cur;
				read_loop: LOOP
						FETCH cur INTO vmUuid,name,accountUuid,state,cpuNum,memorySize,rootVolumeSize,inventory;
						IF done THEN
								LEAVE read_loop;
						END IF;

						INSERT zstack.VmUsageVO(vmUuid,name,accountUuid,state,cpuNum,memorySize,dateInLong,rootVolumeSize,inventory,lastOpDate,createDate)
						VALUES (vmUuid,name,accountUuid,'Destroyed',cpuNum,memorySize,UNIX_TIMESTAMP(),rootVolumeSize,inventory,NOW(),NOW());

				END LOOP;
				CLOSE cur;
				SELECT CURTIME();
		END $$
DELIMITER ;

call cleanExpireVmUsageVO();
DROP PROCEDURE IF EXISTS cleanExpireVmUsageVO;

CREATE TABLE `zstack`.`SchedulerJobGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `jobType` VARCHAR(32),
    `jobClassName` varchar(255),
    `jobData` text,
    `state` varchar(255),
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`SchedulerJobGroupJobRefVO` (
    `schedulerJobUuid` varchar(32) NOT NULL,
    `schedulerJobGroupUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`schedulerJobUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`SchedulerJobGroupSchedulerTriggerRefVO` (
    `schedulerJobGroupUuid` varchar(32) NOT NULL,
    `schedulerTriggerUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`schedulerJobGroupUuid`, `schedulerTriggerUuid`),
    CONSTRAINT `fkSchedulerJobGroupSchedulerTriggerRefVOSchedulerJobVO` FOREIGN KEY (`schedulerJobGroupUuid`) REFERENCES `SchedulerJobGroupVO` (`uuid`),
    CONSTRAINT `fkSchedulerJobGroupSchedulerTriggerRefVOSchedulerTriggerVO` FOREIGN KEY (`schedulerTriggerUuid`) REFERENCES `SchedulerTriggerVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE migrateSchedulerJob()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE triggerUuid VARCHAR(32);
        DECLARE groupUuid VARCHAR(32);
        DECLARE legacyJobUuid VARCHAR(32);
        DECLARE legacyJobName VARCHAR(255);
        DECLARE legacyJobDescription VARCHAR(2048);
        DECLARE legacyJobClassName VARCHAR(255);
        DECLARE legacyJobData TEXT;
        DECLARE legacyJobstate VARCHAR(255);
        DECLARE legacyJobAccountUuid VARCHAR(32);
        DECLARE groupJobType VARCHAR(32);
        DEClARE cur CURSOR FOR SELECT uuid, name, description, jobClassName, jobData, state from SchedulerJobVO
        where jobClassName in ('org.zstack.storage.backup.CreateVolumeBackupJob', 'org.zstack.storage.backup.CreateVmBackupJob');
        DEClARE tcur CURSOR FOR SELECT schedulerTriggerUuid from SchedulerJobSchedulerTriggerRefVO where schedulerJobUuid = legacyJobUuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        insert_group_loop: LOOP
            FETCH cur INTO legacyJobUuid, legacyJobName, legacyJobDescription, legacyJobClassName, legacyJobData, legacyJobstate;
            IF done THEN
                LEAVE insert_group_loop;
            END IF;

            IF legacyJobClassName = 'org.zstack.storage.backup.CreateVolumeBackupJob' THEN
                SET groupJobType = 'volumeBackup';
            ELSE
                SET groupJobType = 'vmBackup';
            END IF;

            SELECT DISTINCT accountUuid INTO legacyJobAccountUuid FROM AccountResourceRefVO WHERE resourceUuid = legacyJobUuid;
            SET groupUuid = (REPLACE(UUID(), '-', ''));
            INSERT INTO zstack.SchedulerJobGroupVO(uuid, name, description, jobClassName, jobData, jobType, state, lastOpDate, createDate)
            VALUES(groupUuid, legacyJobName, legacyJobDescription, legacyJobClassName, legacyJobData, groupJobType, legacyJobstate, NOW(), NOW());
            INSERT INTO zstack.ResourceVO(uuid, resourceName, resourceType, concreteResourceType)
            VALUES(groupUuid, legacyJobName, 'SchedulerJobGroupVO', 'org.zstack.header.scheduler.SchedulerJobGroupVO');
            INSERT INTO zstack.AccountResourceRefVO (accountUuid, ownerAccountUuid, resourceUuid, resourceType, permission, isShared, lastOpDate, createDate)
            VALUES(legacyJobAccountUuid, legacyJobAccountUuid, groupUuid, 'SchedulerJobGroupVO', 2, 0,  NOW(), NOW());

            INSERT INTO zstack.SchedulerJobGroupJobRefVO(schedulerJobUuid, schedulerJobGroupUuid, lastOpDate, createDate)
            VALUES(legacyJobUuid, groupUuid, NOW(), NOW());

            OPEN tcur;
            migrate_ref_loop: LOOP
                FETCH tcur INTO triggerUuid;
                IF done THEN
                    LEAVE migrate_ref_loop;
                END IF;

                INSERT INTO zstack.SchedulerJobGroupSchedulerTriggerRefVO(schedulerJobGroupUuid, schedulerTriggerUuid, lastOpDate, createDate)
                VALUES (groupUuid, triggerUuid, NOW(), NOW());

            END LOOP;
            CLOSE tcur;

            DELETE FROM zstack.SchedulerJobSchedulerTriggerRefVO where schedulerJobUuid = legacyJobUuid;
            SET done = FALSE;
        END LOOP;
        CLOSE cur;
    END $$
DELIMITER ;

call migrateSchedulerJob();
DROP PROCEDURE IF EXISTS migrateSchedulerJob;
