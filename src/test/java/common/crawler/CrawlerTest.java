package common.crawler;

import org.junit.Test;

public class CrawlerTest {
	@Test
	public void testCrawlerBase() throws Exception {
		CrawlerBase crawler = new CrawlerBase();
		org.dom4j.Document document = crawler.getDocument("https://maven.apache.org/plugins/maven-dependency-plugin/analyze-mojo.html");
		System.out.println(document.asXML());
	}
}
