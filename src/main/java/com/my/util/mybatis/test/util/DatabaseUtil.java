package com.my.util.mybatis.test.util;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class DatabaseUtil {

    public static void copyTable(DataSource dataSource, String srcDatabase, String tableName) throws SQLException {
        Connection connection = null;
        Statement createStatement = null;
        try {
            connection = dataSource.getConnection();
            createStatement = connection.createStatement();
            createStatement.execute("show   create table  `" + srcDatabase + "`.`" + tableName + "`");
            ResultSet resultSet = createStatement.getResultSet();
            resultSet.next();
            String create_table = resultSet.getString(2);
            create_table = resetAutoIncrement(create_table);
            createStatement.execute(create_table);
        } finally {
            if (createStatement != null) {
                try {
                    createStatement.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String resetAutoIncrement(String create_table) {
        return create_table.replaceAll("AUTO_INCREMENT=\\d+", "AUTO_INCREMENT=1");
    }

    public static void dropTable(DataSource dataSource, String tableName) throws SQLException {
        Connection connection = null;
        Statement createStatement = null;
        try {
            connection = dataSource.getConnection();
            createStatement = connection.createStatement();
            createStatement.execute("drop   table if exists `" + tableName + "`");
        } finally {
            if (createStatement != null) {
                try {
                    createStatement.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
