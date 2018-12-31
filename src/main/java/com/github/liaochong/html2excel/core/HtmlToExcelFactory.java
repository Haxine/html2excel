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
package com.github.liaochong.html2excel.core;

import com.github.liaochong.html2excel.core.parser.HtmlTableParser;
import com.github.liaochong.html2excel.core.parser.Table;
import com.github.liaochong.html2excel.core.parser.Td;
import com.github.liaochong.html2excel.core.parser.Tr;
import com.github.liaochong.html2excel.core.style.BackgroundStyle;
import com.github.liaochong.html2excel.core.style.BorderStyle;
import com.github.liaochong.html2excel.core.style.FontStyle;
import com.github.liaochong.html2excel.core.style.TdDefaultCellStyle;
import com.github.liaochong.html2excel.core.style.TextAlignStyle;
import com.github.liaochong.html2excel.core.style.ThDefaultCellStyle;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HtmlToExcelFactory
 * <p>
 * 用于将html table解析成excel
 * </p>
 *
 * @author liaochong
 * @version 1.0
 */
@Slf4j
public class HtmlToExcelFactory {

    private HtmlTableParser htmlTableParser;
    /**
     * excel workbook
     */
    private Workbook workbook;
    /**
     * 冻结区域
     */
    private FreezePane[] freezePanes;
    /**
     * 样式容器
     */
    private Map<HtmlTableParser.TableTag, CellStyle> defaultCellStyleMap;
    /**
     * 单元格样式映射
     */
    private Map<Map<String, String>, CellStyle> cellStyleMap;
    /**
     * 每行的单元格最大高度map
     */
    private Map<Integer, Short> maxTdHeightMap;
    /**
     * 字体map
     */
    private Map<String, Font> fontMap;
    /**
     * 是否使用默认样式
     */
    private boolean useDefaultStyle;
    /**
     * 内存数据保有量
     */
    private Integer rowAccessWindowSize = SXSSFWorkbook.DEFAULT_WINDOW_SIZE;
    /**
     * 自定义颜色索引
     */
    private AtomicInteger colorIndex = new AtomicInteger(56);

    public HtmlToExcelFactory() {
    }

    /**
     * 读取html
     *
     * @param htmlFile html文件
     * @return HtmlToExcelFactory
     * @throws Exception 解析异常
     */
    public static HtmlToExcelFactory readHtml(File htmlFile) throws Exception {
        if (Objects.isNull(htmlFile) || !htmlFile.exists()) {
            throw new NoSuchFileException("html file is not exist");
        }
        HtmlToExcelFactory factory = new HtmlToExcelFactory();
        factory.htmlTableParser = HtmlTableParser.of(htmlFile);
        return factory;
    }

    /**
     * 读取html
     *
     * @param html html字符串
     * @return HtmlToExcelFactory
     */
    public static HtmlToExcelFactory readHtml(String html) {
        Objects.requireNonNull(html);
        HtmlToExcelFactory factory = new HtmlToExcelFactory();
        factory.htmlTableParser = HtmlTableParser.of(html);
        return factory;
    }

    /**
     * 读取html
     *
     * @param htmlFile           html文件
     * @param htmlToExcelFactory 实例对象
     * @return HtmlToExcelFactory
     * @throws Exception 解析异常
     */
    public static HtmlToExcelFactory readHtml(File htmlFile, HtmlToExcelFactory htmlToExcelFactory) throws Exception {
        if (Objects.isNull(htmlFile) || !htmlFile.exists()) {
            throw new NoSuchFileException("Html file is not exist");
        }
        if (Objects.isNull(htmlToExcelFactory)) {
            throw new NullPointerException("HtmlToExcelFactory can not be null");
        }
        htmlToExcelFactory.htmlTableParser = HtmlTableParser.of(htmlFile);
        return htmlToExcelFactory;
    }

    /**
     * 设置使用默认样式
     *
     * @return HtmlToExcelFactory
     */
    public HtmlToExcelFactory useDefaultStyle() {
        this.useDefaultStyle = true;
        return this;
    }

    /**
     * 创建固定区域
     *
     * @param freezePanes 固定区域，二维数组
     * @return HtmlToExcelFactory
     */
    public HtmlToExcelFactory freezePanes(FreezePane... freezePanes) {
        this.freezePanes = freezePanes;
        return this;
    }

    /**
     * 设置workbookType为SXSSFWorkbook的内存数据保有量
     *
     * @param rowAccessWindowSize 内存数据保有量
     * @return HtmlToExcelFactory
     */
    public HtmlToExcelFactory rowAccessWindowSize(int rowAccessWindowSize) {
        if (rowAccessWindowSize <= 0) {
            return this;
        }
        this.rowAccessWindowSize = rowAccessWindowSize;
        return this;
    }

    /**
     * 设置workbook类型
     *
     * @param workbookType 工作簿类型
     * @return HtmlToExcelFactory
     */
    public HtmlToExcelFactory workbookType(WorkbookType workbookType) {
        if (Objects.isNull(workbookType)) {
            throw new IllegalArgumentException("WorkbookType must be specified,or remove this method, use the default workbookType");
        }
        switch (workbookType) {
            case XLS:
                workbook = new HSSFWorkbook();
                break;
            case XLSX:
                workbook = new XSSFWorkbook();
                break;
            case SXLSX:
                workbook = new SXSSFWorkbook(rowAccessWindowSize);
                break;
            default:
        }
        return this;
    }

    /**
     * 开始构建
     *
     * @return Workbook
     */
    public Workbook build() {
        List<Table> tables = htmlTableParser.getAllTable();
        return this.build(tables);
    }

    /**
     * 开始构建
     *
     * @param tables tables
     *
     * @return Workbook
     */
    public Workbook build(List<Table> tables) {
        if (Objects.isNull(tables) || tables.isEmpty()) {
            log.warn("There is no any table exist");
            return emptyWorkbook();
        }
        log.info("Start building excel");
        long startTime = System.currentTimeMillis();
        // 1、创建工作簿
        if (Objects.isNull(workbook)) {
            workbook = new XSSFWorkbook();
        }
        if (useDefaultStyle) {
            defaultCellStyleMap = new EnumMap<>(HtmlTableParser.TableTag.class);
            defaultCellStyleMap.put(HtmlTableParser.TableTag.th, new ThDefaultCellStyle().supply(workbook));
            defaultCellStyleMap.put(HtmlTableParser.TableTag.td, new TdDefaultCellStyle().supply(workbook));
        }
        // 2、处理解析表格
        cellStyleMap = new HashMap<>();
        fontMap = new HashMap<>();
        for (int i = 0, size = tables.size(); i < size; i++) {
            Table table = tables.get(i);
            String sheetName = Objects.isNull(table.getCaption()) || table.getCaption().length() < 1 ? "sheet" + (i + 1) : table.getCaption();
            Sheet sheet = workbook.createSheet(sheetName);

            // 设置单元格样式
            this.setTdOfTable(table, sheet);

            if (Objects.nonNull(freezePanes) && freezePanes.length > i) {
                FreezePane freezePane = freezePanes[i];
                if (Objects.isNull(freezePane)) {
                    throw new IllegalStateException("FreezePane is null");
                }
                sheet.createFreezePane(freezePane.getColSplit(), freezePane.getRowSplit());
            }
        }
        log.info("Build excel takes {} ms", System.currentTimeMillis() - startTime);
        return workbook;
    }

    /**
     * 空工作簿
     *
     * @return Workbook
     */
    private Workbook emptyWorkbook() {
        if (Objects.isNull(workbook)) {
            workbook = new XSSFWorkbook();
        }
        Sheet sheet = workbook.createSheet();
        Row row = sheet.createRow(0);
        row.createCell(0);
        return workbook;
    }

    /**
     * 设置所有单元格，自适应列宽，单元格最大支持字符长度255
     */
    private void setTdOfTable(Table table, Sheet sheet) {
        maxTdHeightMap = new HashMap<>();
        for (int i = 0, size = table.getTrList().size(); i < size; i++) {
            Tr tr = table.getTrList().get(i);
            tr.getTdList().forEach(td -> this.setCell(td, sheet));

            // 设置行高，最小12
            Row row = sheet.getRow(tr.getIndex());
            if (Objects.isNull(maxTdHeightMap.get(row.getRowNum()))) {
                row.setHeightInPoints(row.getHeightInPoints() + 5);
            } else {
                row.setHeightInPoints((short) (maxTdHeightMap.get(row.getRowNum()) + 5));
            }
            table.getTrList().set(i, null);
        }

        table.getColMaxWidthMap().forEach((key, value) -> {
            int contentLength = value << 1;
            if (contentLength > 255) {
                contentLength = 255;
            }
            sheet.setColumnWidth(key, contentLength << 8);
        });
    }

    /**
     * 设置单元格
     *
     * @param td    单元格
     * @param sheet 单元格所在的sheet
     */
    private void setCell(Td td, Sheet sheet) {
        Row currentRow = sheet.getRow(td.getRow());
        if (Objects.isNull(currentRow)) {
            currentRow = sheet.createRow(td.getRow());
        }

        Cell cell = currentRow.getCell(td.getCol());
        if (Objects.isNull(cell)) {
            cell = currentRow.createCell(td.getCol());
        }
        cell.setCellValue(td.getContent());


        // 设置单元格样式
        for (int i = td.getRow(), rowBound = td.getRowBound(); i <= rowBound; i++) {
            Row row = sheet.getRow(i);
            if (Objects.isNull(row)) {
                row = sheet.createRow(i);
            }
            for (int j = td.getCol(), colBound = td.getColBound(); j <= colBound; j++) {
                cell = row.getCell(j);
                if (Objects.isNull(cell)) {
                    cell = row.createCell(j);
                }
                this.setCellStyle(row, cell, td);
            }
        }
        if (td.getColSpan() > 0 || td.getRowSpan() > 0) {
            sheet.addMergedRegion(new CellRangeAddress(td.getRow(), td.getRowBound(), td.getCol(), td.getColBound()));
        }
    }

    /**
     * 设置单元格样式
     *
     * @param cell 单元格
     * @param td   td单元格
     */
    private void setCellStyle(Row row, Cell cell, Td td) {
        if (useDefaultStyle) {
            if (td.isTh()) {
                cell.setCellStyle(defaultCellStyleMap.get(HtmlTableParser.TableTag.th));
            } else {
                cell.setCellStyle(defaultCellStyleMap.get(HtmlTableParser.TableTag.td));
            }
        } else {
            if (cellStyleMap.containsKey(td.getStyle())) {
                cell.setCellStyle(cellStyleMap.get(td.getStyle()));
                return;
            }
            CellStyle cellStyle = workbook.createCellStyle();
            // background-color
            BackgroundStyle.setBackgroundColor(workbook, cellStyle, td.getStyle(), colorIndex);
            // text-align
            TextAlignStyle.setTextAlign(cellStyle, td.getStyle());
            // border
            BorderStyle.setBorder(cellStyle, td.getStyle());
            // font
            FontStyle.setFont(workbook, row, cellStyle, td.getStyle(), fontMap, maxTdHeightMap);
            cell.setCellStyle(cellStyle);
            cellStyleMap.put(td.getStyle(), cellStyle);
        }
    }
}
