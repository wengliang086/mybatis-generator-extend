package com.my.util.mybatis;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;

import java.io.File;
import java.io.IOException;

public class MyExtendShellCallback extends DefaultShellCallback {

    private ShellCallback outCallback;

    public MyExtendShellCallback(ShellCallback outCallback) {
        super(true);
        this.outCallback = outCallback;
    }

    @Override
    public boolean isMergeSupported() {
        return true;
    }

    @Override
    public String mergeJavaFile(String newFileSource, String existingFileFullPath, String[] javadocTags, String fileEncoding) throws ShellException {
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
}
