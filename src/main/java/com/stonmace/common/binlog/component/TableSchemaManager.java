package com.stonmace.common.binlog.component;

import com.stonmace.common.binlog.config.IBinlogProperties;
import com.stonmace.common.binlog.model.TableSchema;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 查询数据库表结构信息的
 *
 * @author Alay
 * @date 2022-11-14 19:16
 */
@Component
@RequiredArgsConstructor
public class TableSchemaManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * tableId 与 表全名的映射关系
     */
    private static final Map<Long, String> TABLE_ID_MAP = new HashMap<>(1 << 4);
    /**
     * 表名(全名)与表结构的映射
     */
    private static final Map<String, TableSchema> SCHEMA_MAP = new HashMap<>(1 << 4);

    private final IBinlogProperties binlogProperties;
    private String url;

    @EventListener(value = WebServerInitializedEvent.class)
    public void initSqlUrl() {
        url = String.format("jdbc:mysql://%s:%s/information_schema", binlogProperties.host(), binlogProperties.port());
    }

    private static final String TABLE_SCHEMA_SQL =
            "SELECT " +
                "`TABLE_SCHEMA`," +
                "`TABLE_NAME`," +
                "`COLUMN_NAME`," +
                "`ORDINAL_POSITION`," +
                "`COLUMN_DEFAULT`," +
                "`IS_NULLABLE`," +
                "`DATA_TYPE`," +
                "`CHARACTER_MAXIMUM_LENGTH`," +
                "`CHARACTER_OCTET_LENGTH`," +
                "`NUMERIC_PRECISION`," +
                "`NUMERIC_SCALE`," +
                "`CHARACTER_SET_NAME`," +
                "`COLLATION_NAME` " +
            "FROM" +
                "`information_schema`.`columns` " +
            "WHERE " +
                "`TABLE_SCHEMA` = ? " +
            "AND `TABLE_NAME` = ?";


    public TableSchema queryTableSchema(Long tableId, String database, String tableName) {
        // 内存中获取
        TableSchema tableSchema = SCHEMA_MAP.get(database + "." + tableName);
        if (null != tableSchema) return tableSchema;

        // 数据库中查询获取
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = DriverManager.getConnection(url, binlogProperties.username(), binlogProperties.password());
            statement = connection.prepareStatement(TABLE_SCHEMA_SQL);
            statement.setString(1, database);
            statement.setString(2, tableName);

            // 执行update语句
            rs = statement.executeQuery();
            tableSchema = TableSchema.build(database, tableName);
            while (rs.next()) {
                // 字段创建
                TableSchema.Column column = tableSchema.buildColumn(rs.getString(3));
                column.ordinalPosition(rs.getInt(4));
                column.columnDefault(rs.getString(5));
                column.isNullable(rs.getString(6));
                column.dataType(rs.getString(7));
                column.characterMaximumLength(rs.getString(8));
                column.characterOctetLength(rs.getString(9));
                column.numericPrecision(rs.getString(10));
                column.numericScale(rs.getString(11));
                column.characterSetName(rs.getString(12));
                column.collationName(rs.getString(13));

                // 每个字段
                tableSchema.addColumn(column);
            }
            // 排序
            List<TableSchema.Column> columns = tableSchema.columns().stream().sorted().collect(Collectors.toList());
            // 为了解决存储空间,将字段数据设置为 null
            tableSchema.columns(null);

            List<String> columnNames = columns.stream().map(TableSchema.Column::columnName).collect(Collectors.toList());
            tableSchema.columnNames(columnNames);

            tableSchema.tableId(tableId);
            return tableSchema;
        } catch (SQLException e) {
            logger.error("message:{}, case:{}", e.getMessage(), e.getCause());
            throw new RuntimeException(e.getMessage());
        } finally {
            try {
                if (null != rs) rs.close();
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage());
            }
            try {
                if (null != statement) statement.close();
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage());
            }
            try {
                if (null != connection) connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    public TableSchema getCacheSchema(Long tableId) {
        String tableInfo = TABLE_ID_MAP.get(tableId);
        return SCHEMA_MAP.get(tableInfo);
    }

    public boolean hasCache(Long tableId) {
        return TABLE_ID_MAP.get(tableId) != null;
    }

    public void cacheTable(TableSchema tableSchema) {
        // 本地缓存
        SCHEMA_MAP.put(tableSchema.tableSchema() + "." + tableSchema.tableName(), tableSchema);
        // tableId 映射关系缓存
        TABLE_ID_MAP.put(tableSchema.tableId(), tableSchema.tableSchema() + "." + tableSchema.tableName());
    }


    public void removeCache(String tableInfo) {
        TableSchema tableSchema = SCHEMA_MAP.get(tableInfo);
        if (null != tableSchema) {
            // 移除
            TABLE_ID_MAP.remove(tableSchema.tableId());
            SCHEMA_MAP.remove(tableInfo);
        }
    }

}
