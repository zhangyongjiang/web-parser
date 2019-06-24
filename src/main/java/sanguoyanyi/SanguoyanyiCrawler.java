package sanguoyanyi;

import common.crawler.CrawlerBase;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.dom4j.Document;
import org.dom4j.Element;
import org.xava.server.contentservice.entity.XDocument;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;

public class SanguoyanyiCrawler extends CrawlerBase {
    private String bookName;
    private String webdir;
    private int chapters;

    public SanguoyanyiCrawler(String bookName, String webdir, int chapters) {
        super("/Users/zhangk5/Documents/bsc/projects/gaoshin/documents/"+webdir);
        this.bookName = bookName;
        this.webdir = webdir;
        this.chapters = chapters;
    }

    public XDocument getChapter(String url) throws Exception {
        XDocument xd = new XDocument();
        Document doc = getDocument(url, "gbk");
        Element titleElem = selectElement(doc, "title");
        String title = titleElem.getTextTrim()
                .replaceAll("《" + bookName + "》", "")
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
        String base = "./src/main/zhongguowenxue/小说/"+bookName+"/";
        ObjectMapper om = new ObjectMapper();
        File dir = new File(base);
        if(!dir.exists())
            dir.mkdirs();
        for(int i=1; i<=chapters; i++) {
            String url = String.format("http://www.purepen.com/%s/%03d.htm", webdir, i);
            XDocument xd = getChapter(url);
            xd.title = String.format("%03d %s", i, xd.title);
            FileWriter fw = new FileWriter(base + xd.title);
            om.writeValue(fw, xd);
        }
    }

    public static void main(String[] args) throws Exception {
//        SanguoyanyiCrawler crawler = new SanguoyanyiCrawler("三国演义","sgyy", 120);
//        SanguoyanyiCrawler crawler = new SanguoyanyiCrawler("红楼梦","hlm", 120);
//        SanguoyanyiCrawler crawler = new SanguoyanyiCrawler("西游记","xyj", 100);
        SanguoyanyiCrawler crawler = new SanguoyanyiCrawler("水浒传","shz", 120);
        crawler.dump();
    }
}
