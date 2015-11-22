/******************************************************************
 * File:        ExcelWriter.java
 * Created by:  Dave Reynolds
 * Created on:  22 Nov 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.standardReports.aggregators;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/**
 * Utility for creating and writing LR-style excel sheets
 */
public class ExcelWriter {
    XSSFWorkbook wb;
    
    public XSSFCellStyle highlightStyle;
    public XSSFCellStyle normalStyle;
    XSSFCellStyle normalStripeStyle;
    XSSFCellStyle normalStripeBoldStyle;
    public XSSFCellStyle currencyStyle;
    XSSFCellStyle currencyStripeStyle;
    public XSSFCellStyle currencyHighlightStyle;
    public XSSFCellStyle boldStyle;
    
    Font normal;
    Font bold;
    XSSFSheet sheet;
    int rownum = 0;
    Row row;
    int colnum = 0;
    boolean striping = false;
    boolean stripeOn = true;

    public ExcelWriter() {
        wb = new XSSFWorkbook();
        sheet = wb.createSheet("Top sheet");
        sheet.setColumnWidth(0, 35 * 256);
        sheet.setColumnWidth(1, 15 * 256);
        sheet.setColumnWidth(3, 15 * 256);
        sheet.setColumnWidth(5, 15 * 256);
        sheet.setColumnWidth(7, 15 * 256);
        sheet.setColumnWidth(9, 15 * 256);
        sheet.setColumnWidth(10, 15 * 256);
        
        XSSFColor green = new XSSFColor(new java.awt.Color(0x87, 0xc4, 0x26));

        normal = wb.createFont();
        normal.setFontName("Calibri");
        
        bold = wb.createFont();
        bold.setFontName("Calibri");
        bold.setBold(true);
        
        normalStyle = wb.createCellStyle();
        normalStyle.setFont(normal);
        
        normalStripeStyle = wb.createCellStyle();
        normalStripeStyle.setFont(normal);
        normalStripeStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        normalStripeStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        
        normalStripeBoldStyle = wb.createCellStyle();
        normalStripeBoldStyle.cloneStyleFrom(normalStripeStyle);
        normalStripeBoldStyle.setFont(bold);
        
        highlightStyle = wb.createCellStyle();
        highlightStyle.setFillForegroundColor( green );
        highlightStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        highlightStyle.setFont(bold);
        
        DataFormat df = wb.createDataFormat();
        currencyStyle = wb.createCellStyle();
        currencyStyle.setDataFormat( df.getFormat("£#,##0") );
        currencyStyle.setFont(normal);
        
        currencyStripeStyle = wb.createCellStyle();
        currencyStripeStyle.cloneStyleFrom(currencyStyle);
        currencyStripeStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        currencyStripeStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

        currencyHighlightStyle = wb.createCellStyle();
        currencyHighlightStyle.cloneStyleFrom(currencyStripeStyle);
        currencyHighlightStyle.setFillForegroundColor( green );
        currencyHighlightStyle.setFont(bold);
        
        boldStyle = wb.createCellStyle();
        boldStyle.setFont(bold);
    }
    
    public void addMetaRow(String meta) {
        startRow();
        addCell(meta, boldStyle);
    }
    
    public void addHeaderRow(String[] headers) {
        startRow();
        for (int i = 0; i < headers.length; i++) {
            addCell(headers[i], highlightStyle);
        }
        striping = true;
    }
    
    public void startRow() {
        row = sheet.createRow(rownum++);
        colnum = 0;
        if (striping) {
            stripeOn = !stripeOn;
        }
    }
    
    public void addAccumulator(Accumulator a, boolean highlight) {
        addCell( a.getAverage().longValue(), highlight ? currencyHighlightStyle : (stripeOn ? currencyStripeStyle : currencyStyle) );
        addCell( a.getCount(), highlight ? highlightStyle : (stripeOn ? normalStripeStyle : normalStyle) );
    }
    
    public void addLabelCell(String value) {
        addCell(value, stripeOn ? normalStripeBoldStyle : boldStyle);
    }
    
    public void addCell(String value, CellStyle style) {
        Cell cell  = row.createCell(colnum++);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    
    public void addCell(long value, CellStyle style) {
        Cell cell  = row.createCell(colnum++);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    
    public void write(OutputStream out) throws IOException {
        wb.write(out);
        out.close();
        wb.close();
    }
    
}
