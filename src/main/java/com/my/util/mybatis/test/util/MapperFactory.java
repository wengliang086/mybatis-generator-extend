package com.my.util.mybatis.test.util;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;


public class MapperFactory {

    private DataSource dataSource;

    public MapperFactory(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T getMapper(Class<T> mapperClass) {
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(mapperClass);
        SqlSessionFactory build = new SqlSessionFactoryBuilder().build(configuration);
        return build.openSession().getMapper(mapperClass);
    }

    public <T> T getMapper(Class<T> mapperClass, DataSource dataSource) {
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(mapperClass);
        SqlSessionFactory build = new SqlSessionFactoryBuilder().build(configuration);
        return build.openSession().getMapper(mapperClass);
    }


}
