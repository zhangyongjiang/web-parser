package sanguoyanyi;

import common.crawler.CrawlerBase;
import org.codehaus.jackson.map.ObjectMapper;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xava.server.contentservice.entity.XDocument;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.StringReader;

public class SanguoyanyiCrawler extends CrawlerBase {
    private String baseurl = "http://www.zhmf5.com";

    public SanguoyanyiCrawler(String cachedir) {
        super(cachedir);
    }

    public XDocument getChapter(String url) throws Exception {
        XDocument xd = new XDocument();
        Document doc = getDocument(url, "gbk");
        Element titleElem = selectElement(doc, "title");
        String title = titleElem.getTextTrim()
                .replaceAll("《三国演义》", "")
                .replaceAll("\\(纯文学网站\\)", "")
                .trim();
        xd.title = title;
        Element element = selectElement(doc, "font", "face", "宋体");
        String text = element.getText();
        BufferedReader br = new BufferedReader(new StringReader(text));
        StringBuilder sb = new StringBuilder();
        while(true) {
            String line = br.readLine();
            if(line == null)
                break;
            if(line.length()==0)
                sb.append("\n");
            else
                sb.append(line);
        }
        text = sb.toString();
        xd.description = text;
        return xd;
    }

    public void dump() throws Exception {
        String base = "./src/main/三国演义/";
        ObjectMapper om = new ObjectMapper();
        for(int i=1; i<=120; i++) {
            String url = String.format("http://www.purepen.com/sgyy/%03d.htm", i);
            XDocument xd = getChapter(url);
            xd.title = String.format("%03d %s", i, xd.title);
            FileWriter fw = new FileWriter(base + xd.title);
            om.writeValue(fw, xd);
        }
    }

    public static void main(String[] args) throws Exception {
        SanguoyanyiCrawler crawler = new SanguoyanyiCrawler("/Users/zhangk5/Documents/bsc/projects/gaoshin/documents/sanguoyanyi");
//        crawler.getChapter("http://www.purepen.com/sgyy/001.htm");
        crawler.dump();
    }
}
