package com.zhmf5.mifangdaquan;

import common.crawler.CrawlerBase;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xava.server.contentservice.entity.Category;

import java.util.List;

public class MifangdaquanCrawler extends CrawlerBase {
    private String baseurl = "http://www.zhmf5.com";

    public MifangdaquanCrawler(String cachedir) {
        super(cachedir);
    }

    public List<Category> getTopCategories() throws Exception {
        Document document = getDocument(baseurl + "/mifangdaquan/");
        Element element = selectElement(document, "div", "class", "menu");
        List<Element> elements = selectElements(element, "a");
        for(Element e : elements) {
            System.out.println(e.asXML());
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        MifangdaquanCrawler crawler = new MifangdaquanCrawler("/Users/zhangk5/Documents/bsc/projects/gaoshin/documents/zhmf5.com/mifangdaquan");
        crawler.getTopCategories();
    }
}
