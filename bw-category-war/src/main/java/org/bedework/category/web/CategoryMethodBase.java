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

import org.bedework.category.common.Category;
import org.bedework.category.common.CategoryConfigProperties;
import org.bedework.category.common.CategoryException;
import org.bedework.category.common.SearchResultItem;
import org.bedework.category.impl.CategoryChildImpl;
import org.bedework.category.impl.CategoryIndex;
import org.bedework.category.impl.HrefElementImpl;
import org.bedework.util.elasticsearch.IndexProperties;
import org.bedework.util.http.BasicHttpClient;
import org.bedework.util.misc.Util;
import org.bedework.util.servlet.MethodBase;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlEmit.NameSpace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
  
  private static BasicHttpClient client; 
  
  private XmlEmit rdfEmit;

  private XmlEmit htmlEmit;
  
  public static final String rdfNamespace = 
          "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

  public static final String skosNamespace =
          "http://www.w3.org/2004/02/skos/core#";
  
  private static NameSpace[] rdfNamespaces = {
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

    debug = getLogger().isDebugEnabled();
    
    if (client == null) {
      try {
        client = new BasicHttpClient(1000 * 30);
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

  private SimpleDateFormat httpDateFormatter =
      new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss ");

  protected CategoryIndex getIndex() throws CategoryException {
    if (index != null) {
      return index;
    }

    index = new CategoryIndex(idxProps, config);

    return index;
  }

  protected XmlEmit getRdfEmit() throws ServletException {
    if (rdfEmit != null) {
      return rdfEmit;
    }

    rdfEmit = new XmlEmit(false);

    try {
      for (final NameSpace ns : rdfNamespaces) {
        rdfEmit.addNs(ns, false);
      }
    } catch (Throwable t) {
      throw new ServletException(t);
    }

    return rdfEmit;
  }

  protected XmlEmit getHtmlEmit() throws ServletException {
    if (htmlEmit != null) {
      return htmlEmit;
    }

    htmlEmit = XmlEmit.getHtmlEmitter();

    return htmlEmit;
  }
  
  protected void writeRdf(final Category cat,
                          final HttpServletResponse resp) throws ServletException {
    try {
      XmlEmit xml = getRdfEmit();
    
      xml.startEmit(resp.getWriter());
      
      xml.openTag(rdf);

      xml.openTag(concept, "rdf:about", cat.getHref());
      
      xml.property(prefLabel, cat.getTitle());
      xml.property(definition, cat.getDescription());
      
      if (!Util.isEmpty(cat.getChildren())) {
        for (final Category.CategoryChild ch: cat.getChildren()) {
          xml.emptyTag(narrower, "rdf:resource", ch.getHref());
        }
      }
      xml.closeTag(concept);

      xml.closeTag(rdf);
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  protected void writeHtml(final Category cat,
                           final HttpServletResponse resp) throws ServletException {
    try {
      resp.setContentType("text/html");

      XmlEmit xml = getHtmlEmit();

      xml.startEmit(resp.getWriter());

      xml.openTag(html);
      xml.openTag(head);
      xml.property(title, cat.getTitle());
      xml.closeTag(head);

      xml.openTag(body);

      writeHtmlCat(xml, cat);
      
      xml.closeTag(body);
      xml.closeTag(html);
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  protected void writeHtml(final List<SearchResultItem> sris,
                           final HttpServletResponse resp,
                           final boolean hrefOnly) throws ServletException {
    try {
      resp.setContentType("text/html");
      
      XmlEmit xml = getHtmlEmit();

      xml.startEmit(resp.getWriter());

      xml.openTag(html);
      xml.openTag(head);
      xml.property(title, "Search result");
      xml.closeTag(head);

      xml.openTag(body);
      xml.property(para, "Found: " + sris.size());

      if (hrefOnly) {
        xml.openTag(ul);
      }
      
      for (final SearchResultItem sri: sris) {
        final Category cat = sri.getCategory();
        
        if (!hrefOnly) {
          writeHtmlCat(xml, cat);
        } else {
          xml.openTag(li);
          xml.openTag(address, "href", catHref(cat.getHref()) + "?format=html");
          xml.value(catLabel(cat));
          xml.closeTag(address);
          xml.closeTag(li);
        }
      }

      if (hrefOnly) {
        xml.closeTag(ul);
      }

      xml.closeTag(body);
      xml.closeTag(html);
    } catch (Throwable t) {
      throw new ServletException(t);
    }
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
                              final Category cat) throws ServletException {
    try {
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
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  protected Category getRemote(final String href) 
          throws ServletException {
    final List<String> servers = config.getServerList();

    if (Util.isEmpty(servers)) {
      return null;
    }

    try {
      final BasicHttpClient cl = getClient();

      final List<Header> headers = new ArrayList<>();
      headers.add(new BasicHeader("Accept", "application/json"));

      for (final String server: servers) {
        final String urlStr =
                endingSlash(server) +
                        "category/" + 
                        href;

        final int status = cl.sendRequest("GET", urlStr, headers);
        if ((status / 100) != 2) {
          continue; // Try elsewhere
        }

        return readJsonCat(cl.getResponseBodyAsStream());
      }

      // Nowhere left to go
      return null;
    } catch (final ServletException se) {
      throw se;
    } catch(final Throwable t) {
      throw new ServletException(t);
    }
  }

  protected List<SearchResultItem> findRemote(final String q,
                                              final String pfx)
          throws ServletException {
    final List<String> servers = config.getServerList();

    if (Util.isEmpty(servers)) {
      return null;
    }

    try {
      final BasicHttpClient cl = getClient();

      final List<Header> headers = new ArrayList<>();
      headers.add(new BasicHeader("Accept", "application/json"));

      for (final String server: servers) {
        final String urlStr = 
                endingSlash(server) +
                        "categories/" +
                        "?q=" + URLEncoder.encode(q, "UTF-8") +
                        "&pfx=" + URLEncoder.encode(pfx, "UTF-8");

        final int status = cl.sendRequest("GET", urlStr, headers);
        if ((status % 100) != 2) {
          continue; // Try elsewhere
        }

        return getMapper()
                .readValue(cl.getResponseBodyAsStream(),
                           new TypeReference<List<SearchResultItem>>(){});
      }

      // Nowhere left to go
      return null;
    } catch(final Throwable t) {
      throw new ServletException(t);
    }
  }
  
  private String endingSlash(final String val) {
    if (val.endsWith("/")) {
      return val;
    }
    
    return val + "/";
  }
  
  protected Category readJsonCat(final InputStream str) throws ServletException {
    try {
      return getMapper().readValue(str, Category.class);
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }
  
  protected BasicHttpClient getClient() {
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

