package org.xava.server.contentservice.entity;

import java.util.List;

public class XDocument {
    public String id;
    public long created;
    public String title;
    public String description;
    public String categoryId;
    public String publisherId;
    public String applicationId;
    public String type;
    public List<String> tags;
    public String url;
}
