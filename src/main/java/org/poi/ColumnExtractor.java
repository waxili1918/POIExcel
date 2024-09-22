package org.poi;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ColumnExtractor {

    public Set<String> extractColumnValues(String filename, String columnName) throws Exception {
        Set<String> columnValues = new HashSet<>();
        OPCPackage pkg = OPCPackage.open(filename);
        XSSFReader r = new XSSFReader(pkg);
        SharedStringsTable sst = r.getSharedStringsTable();
        XMLReader parser = fetchSheetParser(sst, columnName, columnValues);

        Iterator<InputStream> sheets = r.getSheetsData();
        while (sheets.hasNext()) {
            InputStream sheet = sheets.next();
            InputSource sheetSource = new InputSource(sheet);
            parser.parse(sheetSource);
            sheet.close();
        }
        return columnValues;
    }

    private XMLReader fetchSheetParser(SharedStringsTable sst, String columnName, Set<String> columnValues)
            throws SAXException, ParserConfigurationException {
        XMLReader parser = XMLHelper.newXMLReader();
        ContentHandler handler = new SheetHandler(sst, columnName, columnValues);
        parser.setContentHandler(handler);
        return parser;
    }

    private static class SheetHandler extends DefaultHandler {
        private final SharedStringsTable sst;
        private final Set<String> columnValues;
        private final String targetColumnName;
        private String lastContents;
        private boolean nextIsString;
        private int targetColumnIndex = -1;
        private int currentColumnIndex = -1;

        private SheetHandler(SharedStringsTable sst, String targetColumnName, Set<String> columnValues) {
            this.sst = sst;
            this.columnValues = columnValues;
            this.targetColumnName = targetColumnName;
        }

        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            if ("c".equals(name)) {
                currentColumnIndex++;
                if (targetColumnIndex == -1 && currentColumnIndex == 0) {
                    // Assuming the first row contains headers
                    if (targetColumnName.equalsIgnoreCase(lastContents)) {
                        targetColumnIndex = currentColumnIndex;
                    }
                }

                String cellType = attributes.getValue("t");
                nextIsString = "s".equals(cellType);
            }
            lastContents = "";
        }

        public void endElement(String uri, String localName, String name) throws SAXException {
            if (nextIsString) {
                int idx = Integer.parseInt(lastContents);
                lastContents = sst.getItemAt(idx).getString();
            }

            if ("v".equals(name) && currentColumnIndex == targetColumnIndex) {
                columnValues.add(lastContents);
            }

            if ("row".equals(name)) {
                currentColumnIndex = -1; // Reset column index for the new row
            }
        }

        public void characters(char[] ch, int start, int length) {
            lastContents += new String(ch, start, length);
        }
    }

    public static void main(String[] args) throws Exception {
        ColumnExtractor extractor = new ColumnExtractor();
        Set<String> columnValues = extractor.extractColumnValues("your-file.xlsx", "siteList");
        System.out.println(columnValues);
    }
}

