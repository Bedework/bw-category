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
package org.bedework.category.service;

import org.bedework.category.common.CategoryException;
import org.bedework.category.common.CategoryIndex;
import org.bedework.util.elasticsearch.EsCtlMBean;
import org.bedework.util.elasticsearch.EsUtil;
import org.bedework.util.jmx.ConfBase;
import org.bedework.util.jmx.InfoLines;
import org.bedework.util.misc.Util;

import java.util.List;

/**
 * @author douglm
 *
 */
public class Categories extends ConfBase<CategoryConfigPropertiesImpl>
        implements CategoriesMBean {
  /* Name of the property holding the location of the config data */
  private static final String datauriPname = "org.bedework.categories.confuri";

  private final static String nm = "config";

  InfoLines infoLines = new InfoLines();
  
  /**
   * The thread that runs the feed
   */
  private class Processor extends ProcessorThread {
    /**
     * @param name - for the thread
     */
    public Processor(final String name) {
      super(name);
    }

    @Override
    public void error(final String msg) {
      Categories.this.error(msg);
    }

    @Override
    public void error(final Throwable t) {
      Categories.this.error(t);
    }

    @Override
    public void info(final String msg) {
      Categories.this.info(msg);
    }

    @Override
    public void warn(final String msg) {
      Categories.this.warn(msg);
    }

    @Override
    public void end(final String msg) {
    }

    @Override
    public void run() {
      infoLines = new InfoLines();

      try {
        info("************************************************************");
        info(" * Starting dmoz indexing");

        setRunning(true);
        setStatus(statusRunning);

        buildIndex();

        for (final String s: infoLines) {
          info(s);
        }

        end("Ending dmoz feed");

        try {
          //getApi().makeProduction(index);
          setStatus(statusDone);
        } catch (final Throwable t) {
          error("Unable to create new index");
          error(t);
          setStatus(statusFailed);
        }
      } catch (final Throwable t) {
        error(t);
        setStatus(statusFailed);
      } finally {
        setRunning(false);
        //closeApi();
      }
    }

    private boolean buildIndex() throws CategoryException {
      final CategoryIndex indexer;
      final Categories cats = Categories.this;
      
      try {
        indexer = new CategoryIndex(getEsCtl(), cats);

        final String targetIndex = indexer.newIndex();
        info("Indexing to " + targetIndex);
        
        indexer.parseDmoz(infoLines);
        
        indexer.makeProduction();
        
        return true;
      } catch (final Throwable t) {
        error("Unable to reindex");
        error(t);
        setStatus(statusFailed);
        return false;
      }
    }
  }
  
  private Processor reindexer;

  /**
   */
  public Categories() {
    super(getServiceName(nm));

    setConfigName(nm);
    setConfigPname(datauriPname);
  }

  /**
   * @param name of service
   * @return object name value for the mbean with this name
   */
  public static String getServiceName(final String name) {
    return "org.bedework.categories:service=" + name;
  }

  /* ========================================================================
   * Attributes
   * ======================================================================== */

  /* ========================================================================
   * properties
   * ======================================================================== */

  @Override
  public void setNamespaces(final List<String> val) {
    getConfig().setNamespaces(val);
  }

  @Override
  public List<String> getNamespaces() {
    return getConfig().getNamespaces();
  }

  @Override
  public void removeNamespace(final String abbrev) {
    getConfig().removeNamespace(abbrev);
  }

  @Override
  public void addNamespace(final String abbrev,
                                   final String uri) {
    getConfig().addNamespace(abbrev, uri);
  }

  @Override
  public String getNamespace(final String abbrev) {
    return getConfig().getNamespace(abbrev);
  }

  @Override
  public void setNamespace(final String abbrev,
                           final String uri) {
    getConfig().setNamespace(abbrev, uri);
  }

  /* ========================================================================
   * Mbean attributes
   * ======================================================================== */

  @Override
  public String getIndexName() {
    return getConfig().getIndexName();
  }

  @Override
  public void setIndexName(final String val) {
    getConfig().setIndexName(val);
  }

  @Override
  public void setDataPath(final String val) {
    getConfig().setDataPath(val);
  }

  @Override
  public String getExclusions() {
    return getConfig().getExclusions();
  }

  @Override
  public void setExclusions(final String val) {
    getConfig().setExclusions(val);
  }

  @Override
  public String getDataPath() {
    return getConfig().getDataPath();
  }

  @Override
  public void setOutDataPath(final String val) {
    getConfig().setOutDataPath(val);
  }

  @Override
  public String getOutDataPath() {
    return getConfig().getOutDataPath();
  }

  @Override
  public void setIndexMapping(final String val) {
    getConfig().setIndexMapping(val);
  }

  @Override
  public String getIndexMapping() {
    return getConfig().getIndexMapping();
  }

  /* ========================================================================
   * Operations
   * ======================================================================== */

  @Override
  public void reindex() {
    final String st = ProcessorThread.checkStarted(reindexer);
    if (statusRunning.equals(st)) {
      error("Already started");
      return;
    }

    reindexer = new Processor("dmz-reindexer");
    reindexer.start();
  }

  @Override
  public void stopReindex() {
    if (reindexer == null) {
      return;
    }

    ProcessorThread.stop(reindexer);
    reindexer = null;
  }

  @Override
  public String listIndexes() {
    try {
      return getEsCtl().listIndexes();
    } catch (Throwable t) {
      t.printStackTrace();
      return t.getLocalizedMessage();
    }
  }

  @Override
  public String purgeIndexes() {
    try {
      final List<String> is = new CategoryIndex(getEsCtl(),
                                                this).purgeIndexes();

      if (Util.isEmpty(is)) {
        return "No indexes purged";
      }

      final StringBuilder res = new StringBuilder("Purged indexes");

      res.append("------------------------\n");

      for (final String i: is) {
        res.append(i);
        res.append("\n");
      }

      return res.toString();
    } catch (Throwable t) {
      t.printStackTrace();
      return t.getLocalizedMessage();
    }
  }

  @Override
  public String getStatus() {
    if (reindexer == null) {
      return statusStopped;
    }
    
    return reindexer.getStatus();
  }

  @Override
  public String loadConfig() {
    return loadConfig(CategoryConfigPropertiesImpl.class);
  }

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  /**
   * @param val number
   * @return 2 digit val
   */
  private static String twoDigits(final long val) {
    if (val < 10) {
      return "0" + val;
    }

    return String.valueOf(val);
  }

  /**
   * 
   * @return Mbean to configure our local copy of EsUtil
   * @throws Throwable
   */
  private EsCtlMBean getEsCtl() throws Throwable {
    return EsUtil.getEsCtl();
  }
}
