package com.xtechcn.common.binlog.core.parser;

import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import com.xtechcn.common.binlog.core.message.InsertMessage;
import com.xtechcn.common.binlog.model.TableSchema;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 插入数据事件 消息解析
 *
 * @author Alay
 * @date 2022-11-14 17:18
 */
public class InsertEventParser implements BinlogEventParser<WriteRowsEventData, InsertMessage> {
    private static final String ACTION = "INSERT";

    @Override
    public InsertMessage parseEvent(WriteRowsEventData event, Object... args) {
        // 表结构
        TableSchema tableSchema = (TableSchema) args[0];

        // 新增的这一行数据
        List<Serializable[]> newRows = event.getRows();

        // 新增数据记录转 Map
        Map<String, Serializable> insertData = this.parseTableData2Map(tableSchema.columnNames(), newRows);

        InsertMessage insertMessage = InsertMessage.build().insertData(insertData);
        insertMessage.tableId(event.getTableId())
                .tableName(tableSchema.tableName())
                .schema(tableSchema.tableSchema())
                .action(ACTION);

        return insertMessage;
    }

}
