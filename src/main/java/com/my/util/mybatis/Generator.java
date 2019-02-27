package com.my.util.mybatis;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.config.*;
import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mybatis.generator.internal.util.messages.Messages.getString;

public class Generator {

    public static List<IntrospectedTable> introspectedTables;

    public static void generate() {
        generate(null);
    }

    public static void generate(final ShellCallback outCallback) {
        try {
            List<String> warnings = new ArrayList<String>();
            HConfigurationParser cp = new HConfigurationParser(warnings);
            Configuration config = cp.parseConfiguration(new ClassPathResource("mybatis/generatorConfig.xml").getInputStream());
            ShellCallback callback = new DefaultShellCallback(true) {

                @Override
                public boolean isMergeSupported() {
                    return true;
                }

                @Override
                public String mergeJavaFile(String newFileSource,
                                            String existingFileFullPath, String[] javadocTags,
                                            String fileEncoding) throws ShellException {
                    newFileSource = appendCustomArea(newFileSource);

                    File file = new File(existingFileFullPath);
                    if (!file.exists()) {
                        return newFileSource;
                    }
                    String oldSrouce = null;
                    try {
                        oldSrouce = Generator.toString(Files.readLines(file, Charsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String merge = Generator.merge(newFileSource, oldSrouce);
                    if (outCallback == null) {
                        return merge;
                    }
                    return outCallback.mergeJavaFile(merge, existingFileFullPath, javadocTags, fileEncoding);
                }

                private String appendCustomArea(String newFileSource) {
                    String[] split = newFileSource.split("(\r\n|\n+)");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < split.length; i++) {
                        if (i == split.length - 1) {
                            sb.append("\r\n");
                            sb.append("\r\n");
                            sb.append("    ////*******自定义开始********/\r\n");
                            sb.append("    //***********自定义结束****////\r\n");
                        }
                        sb.append(split[i]).append("\r\n");
                    }
                    return sb.toString();
                }
            };
            config = dealConfiguration(config);
            MyBatisGenerator myBatisGenerator = new MyBatisGenerator(config, callback, warnings);
            myBatisGenerator.generate(null);

            for (String warString : warnings) {
                System.out.println(warString);
            }

            Field propertiesField = HConfigurationParser.class.getDeclaredField("properties");
            propertiesField.setAccessible(true);
            Properties initProperties = (Properties) propertiesField.get(cp);

            List<Context> contexts = config.getContexts();
            Context context = contexts.get(0);
            JavaModelGeneratorConfiguration javaModelGeneratorConfiguration = context.getJavaModelGeneratorConfiguration();
            SqlMapGeneratorConfiguration sqlMapGeneratorConfiguration = context.getSqlMapGeneratorConfiguration();
            JavaClientGeneratorConfiguration javaClientGeneratorConfiguration = context.getJavaClientGeneratorConfiguration();
            List<TableConfiguration> tableConfigurations = context.getTableConfigurations();
            //System.out.println(javaClientGeneratorConfiguration.getImplementationPackage());

            Field field = Context.class.getDeclaredField("introspectedTables");
            field.setAccessible(true);
            introspectedTables = (List<IntrospectedTable>) field.get(context);
            Properties p = new Properties();

            ClassPathResource daoPathResource = new ClassPathResource("mybatis/dao.template");
            String daoSourceTemplate = toString(daoPathResource.getInputStream());
            ClassPathResource daoimplResource = new ClassPathResource("mybatis/daoimpl.template");
            String daoImplSourceTemplate = toString(daoimplResource.getInputStream());
            ClassPathResource basedaoPathResource = new ClassPathResource("mybatis/basedao.template");
            String basedaoSourceTemplate = toString(basedaoPathResource.getInputStream());

            for (TableConfiguration tc : tableConfigurations) {
                for (IntrospectedTable itable : introspectedTables) {
                    if (tc.getTableName().equals(itable.getTableConfiguration().getTableName())) {

                        p.setProperty("mapper.package", javaClientGeneratorConfiguration.getTargetPackage());
                        p.setProperty("mybatis.vo.name", tc.getDomainObjectName());
                        p.setProperty("mybatis.vo.package", javaModelGeneratorConfiguration.getTargetPackage());
                        p.setProperty("mybatis.vo.name.uncapitalize", StringUtils.uncapitalize(tc.getDomainObjectName()));

                        if (itable.getPrimaryKeyColumns().size() == 1) {
                            IntrospectedColumn introspectedColumn = itable.getPrimaryKeyColumns().get(0);
                            FullyQualifiedJavaType fullPrimaryKeyType = introspectedColumn.getFullyQualifiedJavaType();
                            p.setProperty("primary.key.name", introspectedColumn.getJavaProperty());
                            p.setProperty("primary.key.fulltype", introspectedColumn.getFullyQualifiedJavaType().toString());
                            p.setProperty("primary.key.type", fullPrimaryKeyType.getShortName());
                        } else {
                            String primaryKeyType = itable.getPrimaryKeyType();
                            String shortPrimaryKeyType = primaryKeyType.split("\\.")[primaryKeyType.split("\\.").length - 1];
                            p.setProperty("primary.key.name", StringUtils.uncapitalize(shortPrimaryKeyType));
                            p.setProperty("primary.key.fulltype", itable.getPrimaryKeyType());
                            p.setProperty("primary.key.type", shortPrimaryKeyType);
                        }

                        if (itable.hasBLOBColumns()) {
                            p.setProperty("withBLOBs", "WithBLOBs");
                        } else {
                            p.setProperty("withBLOBs", "");
                        }

                        String daoPackage = initProperties.getProperty("basePackage");
                        p.setProperty("dao.package", daoPackage);

                        File daoDirectory = getDirectory(javaClientGeneratorConfiguration.getTargetProject(), daoPackage);

                        File baseDaoFile = new File(daoDirectory, "Dao.java");

                        File daofile = new File(daoDirectory, tc.getDomainObjectName() + "Dao.java");
                        String mapperPackage = javaClientGeneratorConfiguration.getTargetPackage();
                        File mapperDirectory = getDirectory(javaClientGeneratorConfiguration.getTargetProject(), mapperPackage);
                        String voPackage = context.getJavaModelGeneratorConfiguration().getTargetPackage();
                        File voDirectory = getDirectory(javaClientGeneratorConfiguration.getTargetProject(), voPackage);
                        File daoImplFile = new File(daoDirectory.getPath() + "/mybatis/impl/", tc.getDomainObjectName() + "DaoImpl.java");
                        File voFile = new File(voDirectory, tc.getDomainObjectName() + ".java");
                        File voExampleFile = new File(voDirectory, tc.getDomainObjectName() + "Example.java");
                        File mapperFile = new File(mapperDirectory, tc.getDomainObjectName() + "Mapper.java");

                        boolean isFirstGenerate = !daofile.exists();

                        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
                        writeToFile(propertyPlaceholderHelper.replacePlaceholders(daoSourceTemplate, p), daofile);
                        writeToFile(propertyPlaceholderHelper.replacePlaceholders(daoImplSourceTemplate, p), daoImplFile);
                        writeToFile(propertyPlaceholderHelper.replacePlaceholders(basedaoSourceTemplate, p), new File(daoDirectory.getPath(), "Dao.java"));
                        if (isFirstGenerate) {

                            Properties properties = System.getProperties();
                            String targetDirectory = (String) properties.get("user.dir") + "/target/classes/";
                            JavaCompiler javac;
                            javac = ToolProvider.getSystemJavaCompiler();
                            javac.run(null, null, null, "-g", "-verbose", "-d", targetDirectory, voFile.getAbsolutePath());
                            Generator.class.getClassLoader().loadClass(voPackage + "." + tc.getDomainObjectName());
                            javac.run(null, null, null, "-g", "-verbose", "-d", targetDirectory, voExampleFile.getAbsolutePath());
                            Generator.class.getClassLoader().loadClass(voPackage + "." + tc.getDomainObjectName() + "Example");
                            javac.run(null, null, null, "-g", "-verbose", "-d", targetDirectory, mapperFile.getAbsolutePath());
                            Generator.class.getClassLoader().loadClass(mapperPackage + "." + tc.getDomainObjectName() + "Mapper");
                            javac.run(null, null, null, "-g", "-verbose", "-d", targetDirectory, baseDaoFile.getAbsolutePath());
                            Generator.class.getClassLoader().loadClass(daoPackage + "." + "Dao");
                            javac.run(null, null, null, "-g", "-verbose", "-d", targetDirectory, daofile.getAbsolutePath());
                            Generator.class.getClassLoader().loadClass(daoPackage + "." + tc.getDomainObjectName() + "Dao");
                            javac.run(null, null, null, "-g", "-verbose", "-d", targetDirectory, daoImplFile.getAbsolutePath());
                            Generator.class.getClassLoader().loadClass(daoPackage + ".mybatis.impl." + tc.getDomainObjectName() + "DaoImpl");
                        }
                        DaoTestCaseCodeGenerator.generate(tc);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String pacakgeToPath(String targetPackage) {
        return "\\" + targetPackage.replaceAll("\\.", "\\\\");
    }

    private static void writeToFile(String source, File file) throws IOException {
        if (!file.exists()) {
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            Files.write(source, file, Charsets.UTF_8);
            return;
        }
        String oldSource = toString(Files.readLines(file, Charsets.UTF_8));
        source = merge(source, oldSource);

        Files.write(source, file, Charsets.UTF_8);
    }

    public static String merge(String source, String oldSource) {
        List<String> sourceImports = findImports(source);
        List<String> oldSourceImports = findImports(oldSource);
        List<String> mergeImports = mergeImports(sourceImports, oldSourceImports);

        String regex = "////[\\s\\S]+////";
        Pattern compile = Pattern.compile(regex);
        Matcher matcher = compile.matcher(oldSource);
        if (matcher.find()) {
            source = source.replaceAll(regex, matcher.group());
        }
        source = replaceImports(source, mergeImports);
        return source;
    }

    private static String replaceImports(String source, List<String> mergeImports) {
        String[] split = source.split("\r\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            if (i == 0) {
                sb.append(split[0]);
                sb.append("\r\n");
                sb.append("\r\n");
                for (String _import : mergeImports) {
                    sb.append(_import);
                    sb.append("\r\n");
                }
                continue;
            }
            if (split[i].startsWith("import")) {
                continue;
            }
            sb.append(split[i]);
            sb.append("\r\n");
        }
        return sb.toString();
    }

    private static List<String> mergeImports(List<String> sourceImports,
                                             List<String> oldSourceImports) {
        Set<String> set = new TreeSet<>();
        set.addAll(sourceImports);
        set.addAll(oldSourceImports);
        return new ArrayList<>(set);
    }

    private static List<String> findImports(String source) {
        List<String> imports = new ArrayList<String>();
        String[] split = source.split("(\r\n|\n)");
        for (String line : split) {
            if (line.startsWith("import")) {
                imports.add(line);
            }
        }
        return imports;
    }

    private static File getDirectory(String targetProject, String targetPackage)
            throws ShellException {
        // targetProject is interpreted as a directory that must exist
        //
        // targetPackage is interpreted as a sub directory, but in package
        // format (with dots instead of slashes). The sub directory will be
        // created
        // if it does not already exist

        File project = new File(targetProject);
        if (!project.isDirectory()) {
            throw new ShellException(getString("Warning.9", //$NON-NLS-1$
                    targetProject));
        }

        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(targetPackage, "."); //$NON-NLS-1$
        while (st.hasMoreTokens()) {
            sb.append(st.nextToken());
            sb.append(File.separatorChar);
        }

        File directory = new File(project, sb.toString());
        if (!directory.isDirectory()) {
            boolean rc = directory.mkdirs();
            if (!rc) {
                throw new ShellException(getString("Warning.10", //$NON-NLS-1$
                        directory.getAbsolutePath()));
            }
        }
        return directory;
    }

    private static String toString(List<String> readLines) {
        StringBuilder ss = new StringBuilder();
        for (String s : readLines) {
            ss.append(s).append("\r\n");
        }
        return ss.toString();
    }

    private static String toString(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String readLine = null;
        while ((readLine = bufferedReader.readLine()) != null) {
            sb.append(readLine).append("\r\n");
        }
        return sb.toString();
    }

    private static final String projectPath = ClassLoader.getSystemResource("").getPath().replace("target/test-classes/", "");

    private static Configuration dealConfiguration(Configuration configuration) {
        if (configuration == null) {
            return null;
        }
        System.out.println("path" + projectPath);
        for (Context context : configuration.getContexts()) {
            System.out.println(context);
            String targetProject = context.getJavaModelGeneratorConfiguration().getTargetProject();
            if (isRelativePath(targetProject))
                context.getJavaModelGeneratorConfiguration().setTargetProject(projectPath + targetProject);

            String sqlMapGenerator = context.getSqlMapGeneratorConfiguration().getTargetProject();
            if (isRelativePath(sqlMapGenerator))
                context.getSqlMapGeneratorConfiguration().setTargetProject(projectPath + sqlMapGenerator);

            String javaClientGenerator = context.getJavaClientGeneratorConfiguration().getTargetProject();
            if (isRelativePath(javaClientGenerator))
                context.getJavaClientGeneratorConfiguration().setTargetProject(projectPath + javaClientGenerator);
        }
        return configuration;
    }

    private static boolean isRelativePath(String path) {
        // 不跨盘符会好使
        return !(path.startsWith(projectPath) || ("/" + path).startsWith(projectPath));
    }
}
