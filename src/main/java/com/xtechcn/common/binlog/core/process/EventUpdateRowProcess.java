package com.xtechcn.common.binlog.core.process;

import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.xtechcn.common.binlog.component.TableSchemaManager;
import com.xtechcn.common.binlog.core.message.UpdateMessage;
import com.xtechcn.common.binlog.core.parser.UpdateEventParser;
import com.xtechcn.common.binlog.model.TableSchema;
import lombok.RequiredArgsConstructor;

/**
 * @author Alay
 * @date 2022-11-15 11:38
 */
@RequiredArgsConstructor
public class EventUpdateRowProcess implements BinlogEventProcess<UpdateRowsEventData> {

    private final TableSchemaManager tableSchemaManager;
    private final UpdateEventParser updateEventParser = new UpdateEventParser();

    @Override
    public boolean support(EventType eventType) {
        return EventType.EXT_UPDATE_ROWS == eventType;
    }


    @Override
    public UpdateMessage process(UpdateRowsEventData eventData) {
        long tableId = eventData.getTableId();
        // 表结构数据获取
        TableSchema cacheSchema = tableSchemaManager.getCacheSchema(tableId);
        // 解析 binlog 事件数据
        return updateEventParser.parseEvent(eventData, cacheSchema);
    }
}
