package com.my.util.mybatis;

import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;

public class DaoGenerator2 {

    public static void main(String[] args) {
        Generator.generate(new DefaultShellCallback(true) {

            @Override
            public boolean isMergeSupported() {
                return true;
            }

            @Override
            public String mergeJavaFile(String newFileSource,
                                        String existingFileFullPath, String[] javadocTags,
                                        String fileEncoding) throws ShellException {
                int lastIndexOf = existingFileFullPath.lastIndexOf("\\");

                if (!"vo".equals(existingFileFullPath.substring(lastIndexOf - 2, lastIndexOf))) {
                    return newFileSource;
                }
                String[] split = newFileSource.split("\r\n");
                StringBuilder sb = new StringBuilder();
                boolean appendImport = false;
                if (!newFileSource.contains("Example")) {
                    for (String s : split) {
                        if (s.startsWith("package") && !newFileSource.contains("java.io.Serializable") && !appendImport) {
                            sb.append(s);
                            sb.append("\r\n");
                            sb.append("\r\n");
                            sb.append("import java.io.Serializable;\r\n");
                            appendImport = true;
                            continue;
                        }
                        if (s.contains("public class")) {
                            s = s.replace("{", " implements Serializable {");
                            s = "@SuppressWarnings(\"serial\")\r\n" + s;
                        }
                        sb.append(s).append("\r\n");
                    }
                } else {
                    sb.append(newFileSource);
                }
                return sb.toString();
            }
        });
    }

}
