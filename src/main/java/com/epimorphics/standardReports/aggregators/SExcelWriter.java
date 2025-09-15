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

import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/**
 * Utility for creating and writing LR-style excel sheets.
 * Streaming version
 */
public class SExcelWriter implements SheetWriter {
    XSSFWorkbook basewb;
    SXSSFWorkbook wb;
    CellStyle highlightStyle;
    CellStyle normalStyle;
    CellStyle normalStripeStyle;
    CellStyle normalStripeBoldStyle;
    CellStyle currencyStyle;
    CellStyle currencyStripeStyle;
    CellStyle currencyHighlightStyle;
    CellStyle boldStyle;
    
    Font normal;
    Font bold;
    Sheet sheet;
    int rownum = 0;
    Row row;
    int colnum = 0;

    public SExcelWriter() {
        basewb = new XSSFWorkbook();
        wb = new SXSSFWorkbook(basewb, 100, true);

        sheet = wb.createSheet("Top sheet");
        // Fix libreoffice issues. Taken from: https://lists.apache.org/thread/fr1ddq8j4047jc9c73y531g6o8oqlj4x
        // However fails for office 2016
        // Alternative would be to use non-stream version. Standard Reports are generally pretty small sheets
        // wb.setZip64Mode(Zip64Mode.AlwaysWithCompatibility);

        XSSFColor green = new XSSFColor(new byte[]{ (byte) 0x87, (byte) 0xc4, (byte) 0x26 });

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
        normalStripeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        normalStripeBoldStyle = wb.createCellStyle();
        normalStripeBoldStyle.cloneStyleFrom(normalStripeStyle);
        normalStripeBoldStyle.setFont(bold);
        
        highlightStyle = wb.createCellStyle();
        ((XSSFCellStyle)highlightStyle).setFillForegroundColor( green );
        highlightStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        highlightStyle.setFont(bold);
        highlightStyle.setWrapText(true);
        
        DataFormat df = wb.createDataFormat();
        currencyStyle = wb.createCellStyle();
        currencyStyle.setDataFormat( df.getFormat("£#,##0") );
        currencyStyle.setFont(normal);
        
        currencyStripeStyle = wb.createCellStyle();
        currencyStripeStyle.cloneStyleFrom(currencyStyle);
        currencyStripeStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        currencyStripeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        currencyHighlightStyle = wb.createCellStyle();
        currencyHighlightStyle.cloneStyleFrom(currencyStripeStyle);
        ((XSSFCellStyle)currencyHighlightStyle).setFillForegroundColor( green );
        currencyHighlightStyle.setFont(bold);
        
        boldStyle = wb.createCellStyle();
        boldStyle.setFont(bold);
    }
    
    public void setColumnWidths(int[] widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]*256);
        }
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
    }
    
    public void startRow() {
        row = sheet.createRow(rownum++);
        colnum = 0;
    }
    
    /** handle string, long, average-accumulator */
    public void add(Object o, Style style) {
        add(o, style, false);
    }
    
    /** handle string, long, average-accumulator */
    public void add(Object o, Style style, boolean stripe) {
        CellStyle cs = null;
        CellStyle currCS = null;
        switch (style) {
        case Plain:
            cs = stripe ? normalStripeStyle : normalStyle;
            currCS = stripe ? currencyStripeStyle : currencyStyle;
            break;
        case Bold:
            cs = stripe ? normalStripeBoldStyle : boldStyle;
            currCS = stripe ? currencyStripeStyle : currencyStyle;
            break;
        case Header:
            cs = highlightStyle;
            currCS = currencyHighlightStyle;
            break;
        }
        if (o instanceof Accumulator) {
            Accumulator a = (Accumulator)o;
            addCell( a.getAverage().longValue(), currCS );
            addCell( a.getCount(), cs );
        } else if (o instanceof Long) {
            addCell( (long)o, cs );
        } else {
            addCell( o.toString(), cs );
        }
    }
    
    private void addCell(String value, CellStyle style) {
        Cell cell  = row.createCell(colnum++);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    
    private void addCell(long value, CellStyle style) {
        Cell cell  = row.createCell(colnum++);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    
    public void write(OutputStream out) throws IOException {
        wb.write(out);
        out.close();
        wb.dispose();
        wb.close();
    }
    
}
