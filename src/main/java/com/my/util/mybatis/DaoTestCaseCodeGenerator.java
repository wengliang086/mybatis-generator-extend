package com.my.util.mybatis;

import com.alibaba.druid.pool.DruidDataSource;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.my.util.mybatis.entity.Column;
import com.my.util.mybatis.entity.PrimaryKey;
import com.my.util.mybatis.test.util.Util;
import httl.Engine;
import httl.Template;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.config.*;
import org.mybatis.generator.exception.XMLParserException;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.*;

public class DaoTestCaseCodeGenerator {

    private static String voPackage;

    private static String targetPackage;

    private static JDBCConnectionConfiguration jdbcConnectionConfiguration;

    public static List<TableConfiguration> tableConfigurations;

    private static boolean isInit = false;

    private static List<IntrospectedTable> introspectedTables = Generator.introspectedTables;

    private synchronized static void init() {
        if (isInit) {
            return;
        }
        try {
            List<String> warnings = new ArrayList<String>();
            HConfigurationParser cp = new HConfigurationParser(warnings);
            Configuration config = cp.parseConfiguration(new ClassPathResource("mybatis/generatorConfig.xml").getInputStream());
            Context context = config.getContexts().get(0);
            JavaModelGeneratorConfiguration javaModelGeneratorConfiguration = context.getJavaModelGeneratorConfiguration();

            voPackage = javaModelGeneratorConfiguration.getTargetPackage();
            Field propertiesField = HConfigurationParser.class.getDeclaredField("properties");
            propertiesField.setAccessible(true);
            Properties initProperties = (Properties) propertiesField.get(cp);
            targetPackage = initProperties.getProperty("basePackage");
            tableConfigurations = context.getTableConfigurations();
            jdbcConnectionConfiguration = context.getJdbcConnectionConfiguration();
        } catch (Exception e) {
            throw new RuntimeException("DaoTestCaseCodeGenerator.init failed", e);
        }
        isInit = true;
    }

    public static void generate(TableConfiguration tableConfiguration) {
        init();
        try {
            Class<?> entityClass = Class.forName(voPackage + "." + tableConfiguration.getDomainObjectName());
            Object[] primaryKeyAndColumnNames = getPrimaryKeyAndColumnNames(tableConfiguration, jdbcConnectionConfiguration, entityClass);
            PrimaryKey primaryKey = (PrimaryKey) primaryKeyAndColumnNames[0];
            List<String> columns = (List<String>) primaryKeyAndColumnNames[1];
            String tableName = tableConfiguration.getTableName();
            IntrospectedTable introspectedTable = getIntrospectedTable(tableName);
            _generate(entityClass, targetPackage, voPackage, tableName, primaryKey, columns, introspectedTable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IntrospectedTable getIntrospectedTable(String tableName) {
        for (IntrospectedTable introspectedTable : introspectedTables) {
            if (introspectedTable.getTableConfiguration().getTableName().equals(tableName)) {
                return introspectedTable;
            }
        }
        return null;
    }

    public static void generate() {
        init();
        for (TableConfiguration tableConfiguration : tableConfigurations) {
            generate(tableConfiguration);
        }
    }

    private static Object[] getPrimaryKeyAndColumnNames(TableConfiguration tableConfiguration, JDBCConnectionConfiguration connectionConfiguration, Class<?> entityClass) throws SQLException, MalformedURLException {
        BeanInfo bi = null;
        try {
            bi = Introspector.getBeanInfo(entityClass, Object.class);
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        PropertyDescriptor[] propertyDescriptors = bi.getPropertyDescriptors();

        String tableName = tableConfiguration.getTableName();
        DataSource druidDataSource = getDruidDataSource(connectionConfiguration.getDriverClass(), connectionConfiguration.getConnectionURL(), connectionConfiguration.getUserId(), connectionConfiguration.getPassword());

        Connection connection = druidDataSource.getConnection();
        Statement statement = connection.createStatement();
        String path = parseDBName(connectionConfiguration.getConnectionURL());

        statement.execute("select  * from information_schema.columns where table_name = '" + tableName + "' and table_schema = '" + path + "' ");
        ResultSet resultSet = statement.getResultSet();
        PrimaryKey primaryInfo = new PrimaryKey();
        primaryInfo.setTableName(tableName);
        List<String> columnNames = new ArrayList<String>();
        while (resultSet.next()) {
            String columnKey = resultSet.getString("column_key");
            String columnName = resultSet.getString("column_name");
            columnNames.add(columnName);
            if ("PRI".equals(columnKey)) {
                String extra = resultSet.getString("EXTRA");
                if (extra != null && extra.contains("auto_increment")) {
                    primaryInfo.setIncr(true);
                }
                Column column = new Column();
                column.setName(resultSet.getString("column_name"));
                primaryInfo.addColumns(column);
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    if (column.getJavaProperty().equalsIgnoreCase(propertyDescriptor.getName())) {
                        column.setType(propertyDescriptor.getPropertyType());
                    }
                }
            }
        }
        if (primaryInfo.isIncr()) {
            GeneratedKey generatedKey = tableConfiguration.getGeneratedKey();
            if (generatedKey == null) {
                throw new RuntimeException(tableName + "表有自增主键,generatorConfig.xml文件中的" + tableName + "配置中缺少<generatedKey>元素");
            }
        }
        return new Object[]{primaryInfo, columnNames};
    }

    private static String parseDBName(String url) {
        int i = url.indexOf("?"); // seperator between body and parameters
        url = url.substring(0, i);
        i = url.indexOf("://");
        url = url.substring(i + 3);
        i = url.indexOf("/");
        return url.substring(i + 1);
    }

    private static void _generate(Class entityClass, String daoPackage, String voPackage, String tableName, PrimaryKey primaryKey, List<String> columns, IntrospectedTable introspectedTable) throws IOException, ParseException {
        BeanInfo bi = null;
        try {
            bi = Introspector.getBeanInfo(entityClass, Object.class);
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        PropertyDescriptor[] propertyDescriptors = bi.getPropertyDescriptors();
        List<String> importPacakges = new ArrayList<String>();
        if (hasDateType(propertyDescriptors)) {
            importPacakges.add("java.util.Date");
        }
        PropertyDescriptor[] removePrimaryKey = removePrimaryKey(primaryKey, propertyDescriptors);
        PropertyDescriptor[] removeProperty = removeProperty(removePrimaryKey, columns);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("importPackages", importPacakges);
        parameters.put("entityClassName", entityClass.getSimpleName());
        parameters.put("daoTestCasePackage", daoPackage);//"com.my.fastaccess.provider.access.dao");
        parameters.put("voPackage", voPackage);//"com.my.fastaccess.provider.access.dao.mybatis.vo");
        parameters.put("propertyDescriptors", removeProperty);
        parameters.put("excludePropertyNames", getExcludePropertyNames(primaryKey, columns, propertyDescriptors));
        parameters.put("firstPropertyDescriptor", removeProperty[0]);
        parameters.put("tableName", tableName);//"p_channel_info");
        parameters.put("primaryKey", primaryKey);
        if (primaryKey.hasPrimaryKey() && !primaryKey.isComposite()) {
            parameters.put("primaryKeyColumn", primaryKey.getColumns().get(0));
        }
        if (introspectedTable.hasBLOBColumns()) {
            IntrospectedColumn introspectedColumn = introspectedTable.getBLOBColumns().get(0);
            String javaProperty = Util.getJavaProperty(introspectedColumn.getActualColumnName());
            PropertyDescriptor blobPropertyDescriptor = getPropertyDescriptor(propertyDescriptors, javaProperty);
            parameters.put("blobPropertyDescriptor", blobPropertyDescriptor);
            parameters.put("hasBlobField", true);
        }

        parameters.put("primaryKeyClassName", getPrimaryKeyClassName(primaryKey, entityClass));
        Engine engine = Engine.getEngine();
        Template template = engine.getTemplate("/httl/dao.httl");
        StringWriter writer = new StringWriter();
        template.render(parameters, writer);
        String newSource = writer.getBuffer().toString();
        File targetSourceFile = new File("src/test/java/" + daoPackage.replaceAll("\\.", "/"), entityClass.getSimpleName() + "DaoTest.java");
        if (targetSourceFile.exists()) {
            List<String> readLines = Files.readLines(targetSourceFile, Charsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            for (String line : readLines) {
                sb.append(line).append("\n");
            }
            newSource = Generator.merge(newSource, sb.toString());
        } else {
            targetSourceFile.getParentFile().mkdirs();
        }
        Files.write(newSource, targetSourceFile, Charsets.UTF_8);
        System.out.println("单元测试文件[" + targetSourceFile.getName() + "]写入成功,路径" + targetSourceFile.getAbsolutePath());
    }

    private static PropertyDescriptor getPropertyDescriptor(PropertyDescriptor[] propertyDescriptors,
                                                            String javaProperty) {
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            if (propertyDescriptor.getName().equals(javaProperty)) {
                return propertyDescriptor;
            }
        }
        return null;
    }

    private static String[] getExcludePropertyNames(PrimaryKey primaryKey, List<String> columns, PropertyDescriptor[] propertyDescriptors) {
        List<String> excludeProperties = new ArrayList<String>();
        if (primaryKey.isIncr()) {
            excludeProperties.add(primaryKey.getColumns().get(0).getJavaProperty());
        }
        outter:
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            for (String column : columns) {
                String javaProperty = Util.getJavaProperty(column);
                if (propertyDescriptor.getName().equals(javaProperty)) {
                    continue outter;
                }
            }
            excludeProperties.add(propertyDescriptor.getName());
        }

        return excludeProperties.toArray(new String[0]);
    }

    private static PropertyDescriptor[] removeProperty(PropertyDescriptor[] propertyDescriptors, List<String> columns) {
        List<PropertyDescriptor> targetDescriptors = new ArrayList<PropertyDescriptor>();
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            for (String column : columns) {
                String javaProperty = Util.getJavaProperty(column);
                if (propertyDescriptor.getName().equals(javaProperty)) {
                    targetDescriptors.add(propertyDescriptor);
                }
            }
        }
        return targetDescriptors.toArray(new PropertyDescriptor[0]);
    }

    private static String getPrimaryKeyClassName(PrimaryKey primaryKey, Class entityClass) {
        if (primaryKey.isComposite()) {
            return entityClass.getSimpleName();
        }
        if (!primaryKey.hasPrimaryKey()) {
            return "unkow";
        }
        return primaryKey.getColumns().get(0).getType().getSimpleName();
    }

    private static PropertyDescriptor[] removePrimaryKey(PrimaryKey primaryKey, PropertyDescriptor[] propertyDescriptors) {
        if (!primaryKey.isIncr()) {
            return propertyDescriptors;
        }
        PropertyDescriptor[] targetDescriptors = new PropertyDescriptor[propertyDescriptors.length - primaryKey.getColumns().size()];
        int i = 0;
        outter:
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            for (Column column : primaryKey.getColumns()) {
                if (column.getJavaProperty().equals(propertyDescriptor.getName())) {
                    continue outter;
                }
            }
            targetDescriptors[i++] = propertyDescriptor;
        }
        return targetDescriptors;
    }

    private static DataSource getDruidDataSource(String driverClassName, String url, String user, String password) {
        final DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl(url);
        druidDataSource.setUsername(user);
        druidDataSource.setPassword(password);
        druidDataSource.setDriverClassName(driverClassName);
        druidDataSource.setMaxActive(200);
        druidDataSource.setInitialSize(1);
        druidDataSource.setMaxWait(3000);
        druidDataSource.setMinIdle(1);
        druidDataSource.setTimeBetweenEvictionRunsMillis(60000);
        druidDataSource.setMinEvictableIdleTimeMillis(300000);
        druidDataSource.setTestWhileIdle(true);
        druidDataSource.setTestOnBorrow(true);
        druidDataSource.setTestOnReturn(true);
        druidDataSource.setPoolPreparedStatements(true);
        druidDataSource.setMaxOpenPreparedStatements(200);
        try {
            druidDataSource.init();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return druidDataSource;
    }

    private static boolean hasDateType(PropertyDescriptor[] propertyDescriptors) {
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            if (propertyDescriptor.getPropertyType() == Date.class) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws IOException, ParseException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, XMLParserException, ClassNotFoundException, SQLException {
        generate();
    }
}
