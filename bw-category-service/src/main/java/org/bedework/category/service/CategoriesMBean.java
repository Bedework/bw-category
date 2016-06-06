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

import org.bedework.category.common.CategoryConfigProperties;
import org.bedework.util.jmx.ConfBaseMBean;
import org.bedework.util.jmx.MBeanInfo;

/** Run the Bedework category service
 *
 * @author douglm
 */
public interface CategoriesMBean extends ConfBaseMBean,
        CategoryConfigProperties {

  /* ========================================================================
   * Operations
   * ======================================================================== */

  /** Reindex the data
   *
   */
  @MBeanInfo("Reindex the current data")
  void reindex();

  /** Index the data
   *
   */
  @MBeanInfo("Index the dmoz data file")
  void index();

  /** Stop any running index of data
   *
   */
  @MBeanInfo("Stop indexing the data")
  void stopIndex();

  /** List indexes
   *
   */
  @MBeanInfo("list all indexes")
  String listIndexes();

  /** Purge category indexes
   *
   */
  @MBeanInfo("Purge category indexes")
  String purgeIndexes();
}
