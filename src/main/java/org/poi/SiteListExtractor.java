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

public class SiteListExtractor {

    public Set<String> extractSiteList(String filename) throws Exception {
        Set<String> siteListSet = new HashSet<>();
        OPCPackage pkg = OPCPackage.open(filename);
        XSSFReader r = new XSSFReader(pkg);
        SharedStringsTable sst = r.getSharedStringsTable();
        XMLReader parser = fetchSheetParser(sst, siteListSet);

        Iterator<InputStream> sheets = r.getSheetsData();
        while (sheets.hasNext()) {
            InputStream sheet = sheets.next();
            InputSource sheetSource = new InputSource(sheet);
            parser.parse(sheetSource);
            sheet.close();
        }
        return siteListSet;
    }

    private XMLReader fetchSheetParser(SharedStringsTable sst, Set<String> siteListSet)
            throws SAXException, ParserConfigurationException {
        XMLReader parser = XMLHelper.newXMLReader();
        ContentHandler handler = new SheetHandler(sst, siteListSet);
        parser.setContentHandler(handler);
        return parser;
    }

    private static class SheetHandler extends DefaultHandler {
        private final SharedStringsTable sst;
        private final Set<String> siteListSet;
        private String lastContents;
        private boolean nextIsString;
        private int siteListColumn = -1;
        private int currentColumn = -1;

        private SheetHandler(SharedStringsTable sst, Set<String> siteListSet) {
            this.sst = sst;
            this.siteListSet = siteListSet;
        }

        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            if ("c".equals(name)) {
                currentColumn++;
                String cellRef = attributes.getValue("r");
                if (siteListColumn == -1 && cellRef != null && cellRef.matches("[A-Z]+1")) {
                    if ("siteList".equalsIgnoreCase(lastContents)) {
                        siteListColumn = currentColumn;
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

            if ("v".equals(name) && currentColumn == siteListColumn) {
                siteListSet.add(lastContents);
            }

            if ("row".equals(name)) {
                currentColumn = -1;
            }
        }

        public void characters(char[] ch, int start, int length) {
            lastContents += new String(ch, start, length);
        }
    }

    public static void main(String[] args) throws Exception {
        SiteListExtractor extractor = new SiteListExtractor();
        Set<String> siteList = extractor.extractSiteList("your-file.xlsx");
        System.out.println(siteList);
    }
}
