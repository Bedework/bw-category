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

import org.bedework.category.common.CategoryConfigProperties;
import org.bedework.category.common.CategoryException;
import org.bedework.category.impl.CategoryIndex;
import org.bedework.util.elasticsearch.IndexProperties;
import org.bedework.util.servlet.MethodBase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;

import javax.servlet.ServletException;

/** Base class for all webdav servlet methods.
 */
public abstract class CategoryMethodBase extends MethodBase {
  protected CategoryConfigProperties config;
  
  protected IndexProperties idxProps;
  protected CategoryIndex index;
  
  private ObjectMapper objectMapper = new ObjectMapper();

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

  /** ===================================================================
   *                   Logging methods
   *  =================================================================== */

  /**
   * @return Logger
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void debugMsg(final String msg) {
    getLogger().debug(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  protected void error(final String msg) {
    getLogger().error(msg);
  }

  protected void warn(final String msg) {
    getLogger().warn(msg);
  }

  protected void logIt(final String msg) {
    getLogger().info(msg);
  }

  protected void trace(final String msg) {
    getLogger().debug(msg);
  }
}

