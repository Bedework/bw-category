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
package org.bedework.category.common;

import org.bedework.util.elasticsearch.EsDocInfo;
import org.bedework.util.misc.Util;

import org.apache.log4j.Logger;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.bedework.category.common.Category.*;

/** Build documents for ElasticSearch
 *
 * @author Mike Douglass douglm - rpi.edu
 *
 */
public class DocBuilder {
  static final String docTypeUpdateTracker = "updateTracker";

  static final String updateTrackerId = "updateTracker";

  private transient Logger log;

  private boolean debug;

  private XContentBuilder builder;

  /**
   *
   */
  DocBuilder() throws CategoryException {
    debug = getLog().isDebugEnabled();
    builder = newBuilder();
  }

  /* ===================================================================
   *                   package private methods
   * =================================================================== */

  private XContentBuilder newBuilder() throws CategoryException {
    try {
      XContentBuilder builder = XContentFactory.jsonBuilder();

      if (debug) {
        builder = builder.prettyPrint();
      }

      return builder;
    } catch (Throwable t) {
      throw new CategoryException(t);
    }
  }

  static class UpdateInfo {
    private String dtstamp;
    private Long count = 0l;

    /* Set this true if we write something to the index */
    private boolean update;

    UpdateInfo() {
    }

    UpdateInfo(final String dtstamp,
               final Long count) {
      this.dtstamp = dtstamp;
      this.count = count;
    }

    /**
     * @return dtstamp last time this object type saved
     */
    public String getDtstamp() {
      return dtstamp;
    }

    /**
     * @return count of updates
     */
    public Long getCount() {
      return count;
    }

    /**
     * @param update true to indicate update occurred
     */
    public void setUpdate(final boolean update) {
      this.update = update;
    }

    /**
     * @return true to indicate update occurred
     */
    public boolean isUpdate() {
      return update;
    }

    /**
     * @return a change token for the index.
     */
    public String getChangeToken() {
      return dtstamp + ";" + count;
    }
  }

  private void startObject() throws CategoryException {
    try {
      builder.startObject();
    } catch (Throwable t) {
      throw new CategoryException(t);
    }
  }

  private void endObject() throws CategoryException {
    try {
      builder.endObject();
    } catch (Throwable t) {
      throw new CategoryException(t);
    }
  }

  /* Return the docinfo for the indexer */
  EsDocInfo makeDoc(final UpdateInfo ent) throws CategoryException {
    try {
      startObject();

      builder.field("count", ent.getCount());

      endObject();

      return new EsDocInfo(builder,
                           docTypeUpdateTracker, 0,
                           updateTrackerId);
    } catch (final CategoryException cfe) {
      throw cfe;
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }

  /* Return the docinfo for the indexer */
  EsDocInfo makeDoc(final Category ent) throws CategoryException {
    try {
      startObject();

      makeField("href", ent.getHref());
      makeField("catId", ent.getCatId());
      makeField("namespaceAbbrev", ent.getNamespaceAbbrev());
      makeField("last", ent.getLast());
      makeField("lowerLast", ent.getLowerLast());
      makeField("hrefElements", ent.getHrefElements());
      makeField("hrefDepth", ent.getHrefDepth());
      makeField("lastUpdate", ent.getLastUpdate());
      makeField("title", ent.getTitle());
      makeField("description", ent.getDescription());

      makeCatChildren(ent.getChildren());

      return new EsDocInfo(builder,
                           docType, 0, ent.getHref());
    } catch (final CategoryException cfe) {
      throw cfe;
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }

  /* ========================================================================
   *                   private methods
   * ======================================================================== */

  private void makeCatChildren(final List<CategoryChild> vals) throws CategoryException {
    try {
      if (Util.isEmpty(vals)) {
        return;
      }

      builder.startArray("children");

      for (final CategoryChild cc: vals) {
        makeCategoryChild(cc);
      }

      builder.endArray();
    } catch (final IOException e) {
      throw new CategoryException(e);
    }
  }

  private void makeCategoryChild(final CategoryChild val) throws CategoryException {
    try {
      builder.startObject();

      makeField("sort", val.getSort());
      makeField("href", val.getHref());

      endObject();
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }

  private void makeField(final String name,
                         final String val) throws CategoryException {
    if (val == null) {
      return;
    }

    try {
      builder.field(name, val);
    } catch (IOException e) {
      throw new CategoryException(e);
    }
  }

  private void makeField(final String name,
                         final Object val) throws CategoryException {
    if (val == null) {
      return;
    }

    try {
      builder.field(name, String.valueOf(val));
    } catch (IOException e) {
      throw new CategoryException(e);
    }
  }

  private void makeField(final String name,
                         final Set<String> vals) throws CategoryException {
    try {
      if (Util.isEmpty(vals)) {
        return;
      }

      builder.startArray(name);

      for (String s: vals) {
        builder.value(s);
      }

      builder.endArray();
    } catch (IOException e) {
      throw new CategoryException(e);
    }
  }

  protected Logger getLog() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void info(final String msg) {
    getLog().info(msg);
  }

  protected void debug(final String msg) {
    getLog().debug(msg);
  }

  protected void warn(final String msg) {
    getLog().warn(msg);
  }

  protected void error(final String msg) {
    getLog().error(msg);
  }

  protected void error(final Throwable t) {
    getLog().error(this, t);
  }
}
