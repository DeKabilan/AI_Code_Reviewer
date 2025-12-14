//$Id$
package com.reviewer.ai.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class HTMLParser {

    public static List<String> extractStyleBlocks(String htmlContent) {
        List<String> styles = new ArrayList<>();
        Document document = Jsoup.parse(htmlContent);
        Elements styleElements = document.select("style");

        for (Element style : styleElements) {
            styles.add(style.html());
        }

        return styles;
    }

    public static List<List<String>> extractLearnTableData(String htmlContent) {
        List<List<String>> tableData = new ArrayList<>();
        Document document = Jsoup.parse(htmlContent);
        Elements rows = document.select("table.zw-table tr");

        for (Element row : rows) {
            List<String> rowData = new ArrayList<>();
            Elements cells = row.select("td");
            for (Element cell : cells) {
                rowData.add(cell.text());
            }
            if (!rowData.isEmpty()) {
                tableData.add(rowData);
            }
        }

        return tableData;
    }
}
