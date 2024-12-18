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

import org.bedework.category.impl.CategoryIndex;
import org.bedework.util.jmx.ConfBase;
import org.bedework.util.jmx.InfoLines;
import org.bedework.util.misc.AbstractProcessorThread;
import org.bedework.util.misc.Util;
import org.bedework.util.opensearch.IndexCtlMBean;
import org.bedework.util.opensearch.OschUtil;

import java.util.List;

/**
 * @author douglm
 *
 */
public class Categories extends ConfBase<CategoryConfigPropertiesImpl>
        implements CategoriesMBean {
  /* Name of the directory holding the config data */
  private static final String confDirName = "categories";

  private final static String nm = "config";

  InfoLines infoLines = new InfoLines();
  
  /**
   * The thread that runs the feed
   */
  private class Processor extends AbstractProcessorThread {
    /**
     * @param name - for the thread
     */
    public Processor(final String name) {
      super(name);
    }

    @Override
    public void runInit() {
    }

    @Override
    public void end(final String msg) {
    }

    @Override
    public void close() {
    }

    @Override
    public void runProcess() {
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

    private boolean buildIndex() {
      final CategoryIndex indexer;
      final Categories cats = Categories.this;
      
      try {
        indexer = new CategoryIndex(getIndexCtl(), cats);

        final String targetIndex = indexer.newIndex();
        info("Indexing to " + targetIndex);
        
        indexer.parseDmoz(infoLines, targetIndex);
        
        indexer.makeProduction(targetIndex);
        
        return true;
      } catch (final Throwable t) {
        error("Unable to reindex");
        error(t);
        setStatus(statusFailed);
        return false;
      }
    }
  }
  
  private Processor indexer;

  /**
   */
  public Categories() {
    super(getServiceName(nm), confDirName, nm);
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

  @Override
  public void setPrimaryServer(final boolean val) {
    getConfig().setPrimaryServer(val);
  }

  @Override
  public boolean getPrimaryServer() {
    return getConfig().getPrimaryServer();
  }

  @Override
  public void setToken(final String val) {
    getConfig().setToken(val);
  }

  @Override
  public String getToken() {
    return getConfig().getToken();
  }

  @Override
  public void setServers(final String val) {
    getConfig().setServers(val);
  }

  @Override
  public String getServers() {
    return getConfig().getServers();
  }

  @Override
  public List<String> getServerList() {
    return getConfig().getServerList();
  }

  /* ========================================================================
   * Operations
   * ======================================================================== */

  @Override
  public void reindex() {
    if (!rebuildIndex()) {
      error("Failed to rebuild");
    }
  }

  @Override
  public void index() {
    final String st = AbstractProcessorThread.checkStarted(indexer);
    if (statusRunning.equals(st)) {
      error("Already started");
      return;
    }

    indexer = new Processor("dmz-indexer");
    indexer.start();
  }

  @Override
  public void createIndex() {
    final CategoryIndex indexer;
    final Categories cats = Categories.this;

    try {
      indexer = new CategoryIndex(getIndexCtl(), cats);

      final String targetIndex = indexer.newIndex();
      info("New index " + targetIndex);

      indexer.makeProduction(targetIndex);
    } catch (final Throwable t) {
      error("Unable to create index");
      error(t);
      setStatus(statusFailed);
    }
  }

  @Override
  public void stopIndex() {
    if (indexer == null) {
      return;
    }

    AbstractProcessorThread.stopProcess(indexer);
    indexer = null;
  }

  @Override
  public String listIndexes() {
    try {
      return getIndexCtl().listIndexes();
    } catch (final Throwable t) {
      t.printStackTrace();
      return t.getLocalizedMessage();
    }
  }

  @Override
  public String purgeIndexes() {
    try {
      final List<String> is = new CategoryIndex(getIndexCtl(),
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
    } catch (final Throwable t) {
      t.printStackTrace();
      return t.getLocalizedMessage();
    }
  }

  @Override
  public String getStatus() {
    if (indexer == null) {
      return statusStopped;
    }
    
    return indexer.getStatus();
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

  private boolean rebuildIndex() {
    final CategoryIndex indexer;
    final Categories cats = Categories.this;

    try {
      indexer = new CategoryIndex(getIndexCtl(), cats);

      final String targetIndex = indexer.newIndex();
      info("Reindexing to " + targetIndex);

      indexer.reIndex(targetIndex);

      indexer.makeProduction(targetIndex);

      return true;
    } catch (final Throwable t) {
      error("Unable to reindex");
      error(t);
      setStatus(statusFailed);
      return false;
    }
  }

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
   * @return Mbean to configure our local copy of OschUtil
   * @throws Throwable on fatal error
   */
  private IndexCtlMBean getIndexCtl() throws Throwable {
    return OschUtil.getIndexCtl();
  }
}
