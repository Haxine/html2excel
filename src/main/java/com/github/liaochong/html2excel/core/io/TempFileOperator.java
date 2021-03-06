/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.liaochong.html2excel.core.io;


import com.github.liaochong.html2excel.exception.ExcelBuildException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 临时文件操作类
 *
 * @author liaochong
 * @version 1.0
 */
@Slf4j
public class TempFileOperator {

    public static final String HTML_SUFFIX = ".html";

    private static final int MAX_CREATE_NO = 9_999;

    private static int createNo;

    private static Path templateDir;

    private Path templateFile;

    static {
        try {
            templateDir = Paths.get(new File("").getCanonicalPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 依据前缀名称创建临时文件
     *
     * @param prefix 临时文件前缀
     * @param suffix 临时文件后缀
     * @return Path
     */
    public Path createTempFile(String prefix, String suffix) {
        try {
            templateFile = Files.createTempFile(templateDir, this.getTempFileName(prefix), suffix);
            return templateFile;
        } catch (IOException e) {
            throw ExcelBuildException.of("Failed to create temp file", e);
        }
    }

    /**
     * 获取临时文件名称
     *
     * @param prefix 文件前缀
     * @return 文件名称
     */
    private String getTempFileName(String prefix) {
        long currentTimeMillis = System.currentTimeMillis();
        synchronized (this) {
            if (createNo > MAX_CREATE_NO) {
                createNo = 0;
            }
            createNo++;
        }
        return prefix + "_" + Thread.currentThread().getId() + "_" + currentTimeMillis + "_" + createNo;
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile() {
        try {
            Files.deleteIfExists(templateFile);
        } catch (IOException e) {
            log.warn("Delete temp file failure");
        }
    }

}
