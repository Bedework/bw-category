/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.category.web;

import org.bedework.category.common.CatUtil;
import org.bedework.category.common.Category;
import org.bedework.category.common.CategoryConfigProperties;
import org.bedework.category.common.CategoryException;
import org.bedework.category.common.SearchResult;
import org.bedework.category.common.SearchResultItem;
import org.bedework.category.impl.CategoryChildImpl;
import org.bedework.category.impl.CategoryIndex;
import org.bedework.category.impl.HrefElementImpl;
import org.bedework.util.opensearch.IndexProperties;
import org.bedework.util.http.Headers;
import org.bedework.util.http.HttpUtil;
import org.bedework.util.misc.Util;
import org.bedework.util.servlet.MethodBase;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlEmit.NameSpace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Base class for all webdav servlet methods.
 */
public abstract class CategoryMethodBase extends MethodBase {
  protected CategoryConfigProperties config;
  
  protected IndexProperties idxProps;
  protected CategoryIndex index;
  
  private ObjectMapper objectMapper;
  
  private static CloseableHttpClient client;
  
  private XmlEmit rdfEmit;

  private XmlEmit htmlEmit;
  
  public static final String rdfNamespace = 
          "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

  public static final String skosNamespace =
          "http://www.w3.org/2004/02/skos/core#";
  
  private static final NameSpace[] rdfNamespaces = {
          new NameSpace(rdfNamespace,
                        "rdf"),
          new NameSpace(skosNamespace,
                        "skos")
  };

  /**   */
  public static final QName rdf = new QName(rdfNamespace,
                                            "RDF");

  /**   */
  public static final QName broader = new QName(skosNamespace,
                                                  "broader");

  /**   */
  public static final QName concept = new QName(skosNamespace,
                                                "Concept");

  /**   */
  public static final QName definition = new QName(skosNamespace,
                                                "definition");

  /**   */
  public static final QName narrower = new QName(skosNamespace,
                                             "narrower");

  /**   */
  public static final QName note = new QName(skosNamespace,
                                                "note");

  /**   */
  public static final QName related = new QName(skosNamespace,
                                             "related");

  /**   */
  public static final QName prefLabel = new QName(skosNamespace,
                                                    "prefLabel");

  /* HTML tags */

  /**   */
  public static final QName address = new QName(null, "a");

  /**   */
  public static final QName body = new QName(null, "body");

  /**   */
  public static final QName div = new QName(null, "div");

  /**   */
  public static final QName html = new QName(null, "html");
  
  /**   */
  public static final QName head = new QName(null, "head");

  /**   */
  public static final QName header2 = new QName(null, "h2");

  /**   */
  public static final QName li = new QName(null, "li");

  /**   */
  public static final QName para = new QName(null, "p");

  /**   */
  public static final QName title = new QName(null, "title");

  /**   */
  public static final QName ul = new QName(null, "ul");

  private static final Headers defaultHeaders;

  static {
    defaultHeaders = new Headers();
    defaultHeaders.add("Accept", "application/json");
  }

  @Override
  public void init() throws ServletException {
    
  }

  /** Called at each request
   *
   * @param idxProps indexer config
   * @param config configuration
   * @param dumpContent true to wrap and dump content for debugging
   * @throws ServletException on error
   */
  public void init(final IndexProperties idxProps,
                   final CategoryConfigProperties config,
                   final boolean dumpContent) throws ServletException {
    this.idxProps = idxProps;
    this.config = config;
    this.dumpContent = dumpContent;

    if (client == null) {
      try {
        client  = HttpClients.createDefault();
      } catch (final Throwable t) {
        error(t);
        throw new ServletException(t);
      }
    }

    init();
  }
  
  public ObjectMapper getMapper() {
    if (objectMapper != null) {
      return objectMapper;
    }

    objectMapper = new ObjectMapper();
    
    final SimpleModule sm = new SimpleModule();
    
    sm.addAbstractTypeMapping(Category.CategoryChild.class, 
            CategoryChildImpl.class);
    sm.addAbstractTypeMapping(Category.HrefElement.class,
                              HrefElementImpl.class);
    
    objectMapper.registerModule(sm);

    return objectMapper;
  }

  private final SimpleDateFormat httpDateFormatter =
      new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss ");

  protected CategoryIndex getIndex() throws CategoryException {
    if (index != null) {
      return index;
    }

    index = new CategoryIndex(idxProps, config);

    return index;
  }

  protected XmlEmit getRdfEmit() {
    if (rdfEmit != null) {
      return rdfEmit;
    }

    rdfEmit = new XmlEmit(false);

    Arrays.stream(rdfNamespaces).
            forEach(ns -> rdfEmit.addNs(ns, false));

    return rdfEmit;
  }

  protected XmlEmit getHtmlEmit() {
    if (htmlEmit != null) {
      return htmlEmit;
    }

    htmlEmit = XmlEmit.getHtmlEmitter();

    return htmlEmit;
  }
  
  protected void writeRdf(final Category cat,
                          final HttpServletResponse resp) throws ServletException {
    final XmlEmit xml = getRdfEmit();

    try {
      xml.startEmit(resp.getWriter());
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe);
    }
      
    xml.openTag(rdf);

    xml.openTag(concept, "rdf:about", cat.getHref());
      
    xml.property(prefLabel, cat.getTitle());
    xml.property(definition, cat.getDescription());
      
    if (!Util.isEmpty(cat.getChildren())) {
      cat.getChildren().forEach(
              ch -> xml.emptyTag(narrower,
                               "rdf:resource",
                               ch.getHref()));
    }
    xml.closeTag(concept);

    xml.closeTag(rdf);
  }

  protected void writeHtml(final Category cat,
                           final HttpServletResponse resp) throws ServletException {
    resp.setContentType("text/html");

    final XmlEmit xml = getHtmlEmit();

    try {
      xml.startEmit(resp.getWriter());
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe);
    }

    xml.openTag(html);
    xml.openTag(head);
    xml.property(title, cat.getTitle());
    xml.closeTag(head);

    xml.openTag(body);

    writeHtmlCat(xml, cat);
      
    xml.closeTag(body);
    xml.closeTag(html);
  }

  protected void writeHtml(final SearchResult sr,
                           final HttpServletResponse resp,
                           final boolean hrefOnly) throws ServletException {
      resp.setContentType("text/html");
      
      final XmlEmit xml = getHtmlEmit();

    try {
      xml.startEmit(resp.getWriter());
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe);
    }

    xml.openTag(html);
    xml.openTag(head);
    xml.property(title, "Search result");
    xml.closeTag(head);

    xml.openTag(body);
    xml.property(para, "Found: " + sr.getFound());
    xml.property(para, "Returned: " + sr.getItems().size());

    if (hrefOnly) {
      xml.openTag(ul);
    }
      
    for (final SearchResultItem sri: sr.getItems()) {
      if (!hrefOnly) {
        writeHtmlCat(xml, sri.getCategory());
      } else {
        xml.openTag(li);
        xml.openTag(address, "href", catHref(sri.getHref()) + "?format=html");
        xml.value(sri.getHref() + " (" + sri.getScore() + ")");
        xml.closeTag(address);
        xml.closeTag(li);
      }
    }

    if (hrefOnly) {
      xml.closeTag(ul);
    }

    xml.closeTag(body);
    xml.closeTag(html);
  }

  protected String catHref(final String href) {
    return Util.buildPath(true, "/bwcat/category/",
                          href); // TODO - use context in request
  }
  
  protected String catLabel(final Category cat) {
    if (cat.getTitle() != null) {
      return cat.getTitle();
    }
    
    return cat.getHref();
  }

  protected void writeHtmlCat(final XmlEmit xml,
                              final Category cat) {
    xml.openTag(div);
    xml.property(header2, cat.getTitle());

    xml.openTag(para);
    xml.openTag(address, "href", catHref(cat.getHref()));
    xml.value("RDF format");
    xml.closeTag(address);
    xml.closeTag(para);


    final String parent = parent(cat.getHref());
      
    if (parent != null) {
      xml.openTag(para);
      xml.openTag(address, "href", catHref(parent) + "?format=html");
      xml.value("parent");
      xml.closeTag(address);
      xml.closeTag(para);
    }

    xml.property(para, cat.getDescription());

    if (!Util.isEmpty(cat.getChildren())) {
      xml.openTag(ul);

      for (final Category.CategoryChild ch: cat.getChildren()) {
        xml.openTag(li);
        xml.openTag(address, "href",
                    catHref(ch.getHref()) + "?format=html");
        xml.value(ch.getHref());
        xml.closeTag(address);
        xml.closeTag(li);
      }
        
      xml.closeTag(ul);
    }

    xml.closeTag(div);
  }

  protected Category getRemote(final String href) 
          throws ServletException {
    final List<String> servers = config.getServerList();

    if (Util.isEmpty(servers)) {
      return null;
    }

    try {
      final CloseableHttpClient cl = getClient();

      for (final String server: servers) {
        final URIBuilder urib =
                CatUtil.fromServerUrl(
                        server,
                        Collections.singletonList(href),
                        null);

        try (final CloseableHttpResponse hresp =
                     HttpUtil.doGet(cl,
                                    urib.build(),
                                    this::getDefaultHeaders,
                                    null)) {   // content type
          final int status = HttpUtil.getStatus(hresp);
          if ((status / 100) != 2) {
            continue; // Try elsewhere
          }

          return readJsonCat(hresp.getEntity().getContent());
        }
      }

      // Nowhere left to go
      return null;
    } catch (final ServletException se) {
      throw se;
    } catch(final Throwable t) {
      throw new ServletException(t);
    }
  }

  protected SearchResult findRemote(final String q,
                                    final String pfx,
                                    final boolean href,
                                    final int from,
                                    final int size)
          throws ServletException {
    final List<String> servers = config.getServerList();

    if (Util.isEmpty(servers)) {
      return null;
    }

    try {
      final CloseableHttpClient cl = getClient();

      for (final String server: servers) {
        final URIBuilder urib =
                CatUtil.fromServerUrl(
                        server,
                        null,
                        pfx,
                        new BasicNameValuePair("href",
                                               String.valueOf(href)),
                        new BasicNameValuePair("from",
                                               String.valueOf(from)),
                        new BasicNameValuePair("ct",
                                               String.valueOf(size)),
                        new BasicNameValuePair("q", q));

        try (final CloseableHttpResponse hresp =
                     HttpUtil.doGet(cl,
                                    urib.build(),
                                    this::getDefaultHeaders,
                                    null)) {   // content type
          final int status = HttpUtil.getStatus(hresp);

          if ((status / 100) != 2) {
            continue; // Try elsewhere
          }

          return getMapper()
                  .readValue(hresp.getEntity().getContent(),
                             SearchResult.class);
        }
      }

      // Nowhere left to go
      return null;
    } catch(final Throwable t) {
      throw new ServletException(t);
    }
  }

  private Headers getDefaultHeaders() {
    return defaultHeaders;
  }

  protected Category readJsonCat(final InputStream str) throws ServletException {
    try {
      return getMapper().readValue(str, Category.class);
    } catch (final Throwable t) {
      throw new ServletException(t);
    }
  }
  
  protected CloseableHttpClient getClient() {
    return client;    
  }
  
  private String parent(final String href) {
    if ((href == null) || (href.length() <= 2)) {
      return null;
    }
    
    int index = href.length() - 1;
    
    if (href.charAt(index) == '/') {
      index--;
    }

    index = href.lastIndexOf('/', index);
    
    if (index < 0) {
      return null;
    }
    
    return href.substring(0, index);
  }
}

