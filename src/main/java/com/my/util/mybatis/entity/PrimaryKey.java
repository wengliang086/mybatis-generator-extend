package com.my.util.mybatis.entity;

import java.util.ArrayList;
import java.util.List;

public class PrimaryKey {

    private String tableName;

    private List<Column> columns = new ArrayList<Column>();

    private boolean incr;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public void addColumns(Column column) {
        this.columns.add(column);
    }

    public boolean isIncr() {
        return incr;
    }

    public void setIncr(boolean incr) {
        this.incr = incr;
    }

    public boolean isComposite() {
        return columns.size() > 1;
    }

    public boolean hasPrimaryKey() {
        return !columns.isEmpty() && columns != null;
    }

}
