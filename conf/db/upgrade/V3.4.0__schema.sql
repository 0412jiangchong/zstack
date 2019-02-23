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

INSERT INTO AccountResourceRefVO (`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`) SELECT "36c27e8ff05c4780bf6d2fa65700f22e", "36c27e8ff05c4780bf6d2fa65700f22e", t.uuid, "L2NetworkVO", 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() FROM L2NetworkVO t where t.type in ("L2VlanNetwork", "L2NoVlanNetwork");