package org.zstack.header.core.trash

doc {

	title "清理回收站数据返回信息"

	field {
		name "resourceUuids"
		desc "清理掉数据对应的UUID集合"
		type "List"
		since "3.3.0"
	}
	field {
		name "size"
		desc "清理数据总大小"
		type "Long"
		since "3.3.0"
	}
}
