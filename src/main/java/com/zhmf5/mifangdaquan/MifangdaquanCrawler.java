package com.zhmf5.mifangdaquan;

import common.crawler.CrawlerBase;
import common.crawler.Link;
import common.crawler.PageException;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xava.server.contentservice.entity.Category;
import org.xava.server.contentservice.entity.XDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MifangdaquanCrawler extends CrawlerBase {
    private String baseurl = "http://www.zhmf5.com";

    public MifangdaquanCrawler(String cachedir) {
        super(cachedir);
    }

    public void dump() throws Exception {
        List<Category> categories = getTopCategories();
        int cnt = 0;
        for(Category category : categories) {
            setCacheResult(baseurl+category.url, category);
            List<XDocument> documents = getDocumentsForCategory(category);
            for(XDocument xd : documents) {
                try {
                    XDocument details = getXdocumentDetails(xd);
                    cnt++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        System.err.println("total docs " + cnt);
    }

    private XDocument getXdocumentDetails(XDocument xd) throws Exception {
        String url = xd.url;
        XDocument details = getDocumentDetailsFromUrl(url);
        details.id = xd.id;
        details.categoryId = xd.categoryId;
        details.url = xd.url;
        for(int i=2;;i++) {
            url = xd.url.replaceAll(".html", "_"+i+".html");
            try {
                XDocument next = getDocumentDetailsFromUrl(url);
                details.description += "\n" + next.description;
            }
            catch (Exception e) {
                System.err.println("set PageException " + url);
                setCache(baseurl+url, PageException.class.getSimpleName());
                break;
            }
        }
        setCacheResult(baseurl+details.url, details);
        return details;
    }

    private XDocument getDocumentDetailsFromUrl(String url) throws Exception {
        XDocument xd = new XDocument();
        xd.url = url;
        System.err.println("get document details from " + url);
        Document document = getDocument(baseurl + url);
        Element element = selectElement(document, "div", "id", "Cnt-Main-Article-QQ");
        List<Element> elements = selectElements(element, "p");
        for(Element e : elements) {
            String txt = getText(e).replaceAll("[\\n\\r]+", "");
            if(txt.contains("还有下一页"))
                break;
            if(xd.description == null)
                xd.description = txt;
            else
                xd.description += "\n" + txt;
        }
        System.err.println(xd.description);
        return xd;
    }

    private List<XDocument> getDocumentsForCategory(Category category) throws Exception {
        List<XDocument> documents = new ArrayList<XDocument>();
        String url = category.url;
        Collection<? extends XDocument> docs = getDocumentUrlFromUrl(url, category.id);
        documents.addAll(docs);
        for(int i=2;;i++) {
            url = category.url + "index_" + i + ".html";
            try {
                docs = getDocumentUrlFromUrl(url, category.id);
                documents.addAll(docs);
            } catch (Exception e) {
                setCache(baseurl+url, PageException.class.getSimpleName());
                break;
            }
        }
        return  documents;
    }

    private List<XDocument> getDocumentUrlFromUrl(String url, String categoryId) throws Exception {
        System.err.println("get document url from " + url);
        Document document = getDocument(baseurl + url);
        List<Element> elements = selectElements(document, "h6");
        List<XDocument> documents = new ArrayList<XDocument>();
        for(Element e : elements) {
            XDocument doc = documentFromElememt(selectElement(e, "a"));
            doc.id = UUID.randomUUID().toString();
            doc.categoryId = categoryId;
            documents.add(doc);
        }
        return documents;
    }

    private XDocument documentFromElememt(Element e) {
        XDocument c = new XDocument();
        Link link = getLink(e);
        c.title = link.title;
        c.url = link.href;
        System.err.println("found doc " + c.title + ", " + c.url);
        return c;
    }

    private Category categoryFromElememt(Element element) {
        Link link = getLink(element);
        Category c = new Category();
        c.name = link.title;
        c.url = link.href;
        return c;
    }

    public List<Category> getTopCategories() throws Exception {
        Document document = getDocument(baseurl + "/mifangdaquan/");
        Element element = selectElement(document, "div", "class", "menu");
        List<Element> elements = selectElements(element, "a");
        List<Category> categories = new ArrayList<Category>();
        for(Element e : elements) {
            Category category = categoryFromElememt(e);
            category.id = UUID.randomUUID().toString();
            if(category.url.indexOf("/mifangdaquan/")==-1)
                continue;
            if(category.url.equals("/mifangdaquan/"))
                continue;
            categories.add(category);
        }
        return categories;
    }

    public static void main(String[] args) throws Exception {
        MifangdaquanCrawler crawler = new MifangdaquanCrawler("/Users/zhangk5/Documents/bsc/projects/gaoshin/documents/zhmf5.com/mifangdaquan");
        crawler.dump();
//        crawler.getDocumentDetailsFromUrl("/2011/02/4960_2.html");
    }
}
