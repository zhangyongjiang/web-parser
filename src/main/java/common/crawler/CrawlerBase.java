package common.crawler;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.cyberneko.html.parsers.DOMParser;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.DOMReader;
import org.dom4j.io.SAXReader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class CrawlerBase {
	private final Logger logger = Logger.getLogger(CrawlerBase.class);
	
    protected DefaultHttpClient httpClient;

    private String cachedir;
    
    public CrawlerBase(String cachedir) {
        this(1, cachedir);
    }
    
    public CrawlerBase(int maxConnections, String cachedir) {
        this.cachedir = cachedir;
        httpClient = new DefaultHttpClient();
        if(!cachedir.endsWith("/"))
            cachedir += "/";
        File file = new File(cachedir);
        if(!file.exists())
            file.mkdirs();
    }
    
    public String getContent(String url) throws IOException, PageException {
        String cached = getCached(url);
        if(cached != null)
            return cached;
        InputStream stream = new URL(url).openStream();
        String content = IOUtils.toString(stream);
        setCache(url, content);
        return content;
    }

    private String getFileNameForUrl(String url) {
        return url.replaceAll("[^a-zA-Z0-9 -]", "");
    }

    private String getFilePathForUrl(String url) {
        url = getFileNameForUrl(url);
        if(url.length()<10)
            return cachedir + "/" + url;
        if(url.length()<20)
            return cachedir + "/" + url.substring(0, 10) + "/" + url.substring(10);

        return cachedir + "/" + url.substring(0, 10) + "/" + url.substring(10, 20) + '/' + url.substring(20);
    }

    protected void setCache(String url, String content) throws IOException {
        String path = getFilePathForUrl(url);
        String dir = path.substring(0, path.lastIndexOf("/"));
        File file = new File(dir);
        if(!file.exists())
            file.mkdirs();
        FileWriter fw = new FileWriter(path);
        fw.write(content);
        fw.close();
    }

    ObjectMapper objectMapper = new ObjectMapper();
    protected void setCacheResult(String url, Object content) throws IOException {
        String path = getFilePathForUrl(url);
        String dir = path.substring(0, path.lastIndexOf("/"));
        File file = new File(dir);
        if(!file.exists())
            file.mkdirs();
        FileWriter fw = new FileWriter(path+".json");
        fw.write(objectMapper.writeValueAsString(content));
        fw.close();
    }

    protected String getCached(String url) throws IOException, PageException {
        String path = getFilePathForUrl(url);
        File file = new File(path);
        if(!file.exists())
            return null;
        System.err.println("=== found cache for " + url);
        String cache = IOUtils.toString(new FileReader(file));
        if(PageException.class.getSimpleName().equals(cache)) {
            System.err.println("=== PageException " + url);
            throw new PageException();
        }
        return cache;
    }

    public String getContentFromUrl(String url) throws Exception {
        String cache = getCached(url);
        if(cache != null)
            return cache;
        HttpGet get = new HttpGet(url);
        HttpResponse response = httpClient.execute(get);
        InputStream inputStream = response.getEntity().getContent();
        String content = IOUtils.toString(inputStream);
        inputStream.close();
        setCache(url, content);
        return content;
    }

    public String getContentFromUrl(String url, String username, String password) throws Exception {
        String cacheurl = url + username + password;
        String cache = getCached(cacheurl);
        if(cache != null)
            return cache;

        String encoding = new Base64().encode((username + ":" + password).getBytes());
//    	httpGet.addHeader("Authorization", "Basic " + Base64.encodeToString((user + ":" + pwd).getBytes(), Base64.NO_WRAP));
        HttpGet get = new HttpGet(url);
    	get.setHeader("Authorization", "Basic " + encoding);
        HttpResponse response = httpClient.execute(get);
        InputStream inputStream = response.getEntity().getContent();
        String content = IOUtils.toString(inputStream);
        inputStream.close();

        setCache(cacheurl, content);
        return content;
    }

    public org.dom4j.Document getDocument(String url) throws Exception {
        String cache = getCached(url);
        if(cache != null) {
            SAXReader reader = new SAXReader();
            return reader.read(new StringReader(cache));
        }

        DOMParser parser = new DOMParser();
        parser.setFeature("http://xml.org/sax/features/namespaces", false);
        parser.parse(url);
        Document document = parser.getDocument();
        DOMReader reader = new DOMReader();
        org.dom4j.Document doc = reader.read(document);

        String xml = doc.asXML();
        setCache(url, xml);
        return doc;
    }

    public org.dom4j.Document getDocument(InputStream is) throws Exception {
        DOMParser parser = new DOMParser();
        parser.setFeature("http://xml.org/sax/features/namespaces", false);
        parser.parse(new InputSource(is));
        Document document = parser.getDocument();
        DOMReader reader = new DOMReader();
        org.dom4j.Document doc = reader.read(document);
        return doc;
    }
    
    public List<Element> selectElements(org.dom4j.Node doc, String tag, String attrName, String attrValue) {
        return doc.selectNodes(".//" + tag.toUpperCase() + "[@" + attrName + "='" + attrValue + "']");
    }
    
    public List<Element> selectElements(org.dom4j.Node doc, String tag) {
        return doc.selectNodes(".//" + tag.toUpperCase());
    }
    
    public Element selectElement(org.dom4j.Node doc, String tag, String attrName, String attrValue) {
        return (Element) doc.selectSingleNode(".//" + tag.toUpperCase() + "[@" + attrName + "='" + attrValue + "']");
    }
    
    public Element selectElement(org.dom4j.Node doc, String tag) {
        return (Element) doc.selectSingleNode(".//" + tag.toUpperCase());
    }
    
    public Link getLink(Node node) {
        return getLink((Element) node);
    }
    
    public String getImg(Element ell) {
        return ell.attributeValue("src");
    }
    
    public String getImg(Node node) {
        return getImg((Element)node);
    }
    
    public Link getLink(Element ele) {
        Link link = new Link();
        link.title = ele.getStringValue().trim();
        link.href = ele.attributeValue("href");
        return link;
    }
    
    public String formatPhone(String phone) {
        if(phone == null || phone.trim().length() == 0)
            return null;
        phone = phone.replaceAll("[\\+\\.\\(\\) \\-]+", "");
        if(phone.length() == 0 || "NULL".equalsIgnoreCase(phone)) {
            return null;
        }
        
        phone = phone.replaceAll("[a|A|b|B|c|C]", "2");
        phone = phone.replaceAll("[d|e|f|D|E|F]", "3");
        phone = phone.replaceAll("[g|h|i|G|H|I]", "4");
        phone = phone.replaceAll("[j|k|l|J|K|L]", "5");
        phone = phone.replaceAll("[m|n|o|M|N|O]", "6");
        phone = phone.replaceAll("[p|q|r|s|P|Q|R|S]", "7");
        phone = phone.replaceAll("[t|u|v|T|U|V]", "8");
        phone = phone.replaceAll("[w|x|y|z|W|X|Y|Z]", "9");
                
        if(phone.startsWith("1") && phone.length() == 11) {
            return phone;
        }
        if(phone.length() == 10) {
            return "1" + phone;
        }
        
        if(!phone.startsWith("1")) {
            phone = "1" + phone;
        }
        return phone.length() > 11 ? phone.substring(0,11) : phone;
    }

    public String[] getStateAndZip(String stateZip) {
        stateZip = stateZip.toUpperCase().replaceAll("[^A-Z0-9]+", "");
        String[] sz = new String[2];
        sz[0] = stateZip.substring(0,2);
        sz[1] = stateZip.substring(2);
        return sz;
    }
    
    public void shutdown() {
        httpClient.getConnectionManager().shutdown();
    }
    
    public Map<String, String> getCityStateZip(String line) {
        line = line.trim();
        
        Map<String, String> data = new HashMap<String, String>();
        int pos = line.lastIndexOf(" ");
        String zip = line.substring(pos+1).trim();
        data.put("zipcode", zip);
        
        String storeState = line.substring(pos-2, pos);
        data.put("state", storeState);
        
        String storeCity = line.substring(0, pos - 4).trim();
        data.put("city", storeCity);
        
        return data;
    }
    
    public org.dom4j.Document post(String url, String contentType, String accept, String data) throws Exception {
        try {
	        HttpPost post = new HttpPost(url);
	        if(contentType != null)
	        	post.setHeader("Content-Type", contentType);
	        if(accept != null)
	        	post.setHeader("Accept", accept);
	        post.setEntity(new StringEntity(data));
	        HttpResponse response = httpClient.execute(post);
	        InputStream inputStream = response.getEntity().getContent();
	        org.dom4j.Document document = getDocument(inputStream);
	        inputStream.close();
	        return document;
        } catch (Exception e) {
        	logger.error("cannot retrieve info from " + url + "\n" + contentType + "\naccept: " + accept + "\n" + data);
        	throw e;
        }
    }

	public String getText(Node node) {
		StringBuilder sb = new StringBuilder();
		getText(node, sb);
		return sb.toString().replaceAll("[ \t]+", " ").replaceAll("\n ", "\n").replaceAll("[\n]+", "\n");
	}
	
	public void getText(Node node, StringBuilder sb) {
		if(node.getNodeType() == Node.ELEMENT_NODE) {
			Element ele = (Element) node;
			if(ele.isTextOnly()) {
				sb.append(ele.getTextTrim()).append("\n");
			}
			else {
				List contents = ele.content();
				getText(contents, sb);
			}
		}	
		else {
			sb.append(node.getText()).append("\n");
		}
	}
	
	public void getText(List<Node> nodes, StringBuilder sb) {
		for(Node node : nodes) {
			getText(node, sb);
		}
	}
	
	public String getText(List<Node> nodes) {
		StringBuilder sb = new StringBuilder();
		getText(nodes, sb);
		return sb.toString().replaceAll("[ \t]+", " ").replaceAll("\n ", "\n").replaceAll("[\n]+", "\n");
	}
}
