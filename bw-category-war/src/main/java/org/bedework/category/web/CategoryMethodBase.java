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
import org.bedework.category.impl.CategoryIndex;
import org.bedework.util.elasticsearch.IndexProperties;
import org.bedework.util.misc.Util;
import org.bedework.util.servlet.MethodBase;
import org.bedework.util.xml.XmlEmit;
import org.bedework.util.xml.XmlEmit.NameSpace;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Base class for all webdav servlet methods.
 */
public abstract class CategoryMethodBase extends MethodBase {
  protected CategoryConfigProperties config;
  
  protected IndexProperties idxProps;
  protected CategoryIndex index;
  
  private ObjectMapper objectMapper = new ObjectMapper();
  
  private XmlEmit rdfEmit;
  
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

    init();
  }
  
  public ObjectMapper getMapper() {
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
}

