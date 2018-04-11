CREATE TABLE  `zstack`.`CertificateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(256) NOT NULL,
    `certificate` text NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LoadBalancerListenerCertificateRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `listenerUuid` varchar(32) NOT NULL,
    `certificateUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkLoadBalancerListenerCertificateRefVOLoadBalancerListenerVO` FOREIGN KEY (`listenerUuid`) REFERENCES `zstack`.`LoadBalancerListenerVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkLoadBalancerListenerCertificateRefVOCertificateVO` FOREIGN KEY (`certificateUuid`) REFERENCES `zstack`.`CertificateVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;