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
import org.bedework.util.config.ConfInfo;
import org.bedework.util.config.ConfigBase;
import org.bedework.util.misc.ToString;

import java.util.List;

/** These are the system properties that the server needs to know about, either
 * because it needs to apply these limits or just to report them to clients.
 *
 * @author douglm
 *
 */
@ConfInfo(elementName = "category-properties",
        type = "org.bedework.category.CategoryConfigPropertiesImpl")
public class CategoryConfigPropertiesImpl<T extends CategoryConfigPropertiesImpl>
        extends ConfigBase<T>
        implements CategoryConfigProperties {
  private String indexName;

  private String structureDataPath;
  
  private String exclusions;

  private String outDataPath;

  private String indexMapping;

  private List<String> namespaces;

  @Override
  public String getIndexName() {
    return indexName;
  }

  @Override
  public void setIndexName(final String val) {
    indexName = val;
  }

  @Override
  public String getDataPath() {
    return structureDataPath;
  }

  @Override
  public void setDataPath(final String val) {
    structureDataPath = val;
  }

  @Override
  public String getExclusions() {
    return exclusions;
  }

  @Override
  public void setExclusions(final String val) {
    exclusions = val;
  }

  @Override
  public String getOutDataPath() {
    return outDataPath;
  }

  @Override
  public void setOutDataPath(final String val) {
    outDataPath = val;
  }

  @Override
  public void setIndexMapping(final String val) {
    indexMapping = val;
  }

  @Override
  public String getIndexMapping() {
    return indexMapping;
  }

  @Override
  public void setNamespaces(final List<String> val) {
    namespaces = val;
  }

  @Override
  @ConfInfo(collectionElementName = "namespaces" ,
          elementType = "java.lang.String")
  public List<String> getNamespaces() {
    return namespaces;
  }

  @Override
  public void addNamespace(final String abbrev,
                           final String val) {
    setNamespaces(addListProperty(getNamespaces(),
                                  abbrev, val));
  }

  @Override
  @ConfInfo(dontSave = true)
  public String getNamespace(final String abbrev) {
    return getProperty(getNamespaces(), abbrev);
  }

  @Override
  public void removeNamespace(final String abbrev) {
    removeProperty(getNamespaces(), abbrev);
  }

  @Override
  @ConfInfo(dontSave = true)
  public void setNamespace(final String abbrev,
                           final String uri) {
    setNamespaces(setListProperty(getNamespaces(),
                                  abbrev, uri));
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    ts.append("indexName", getIndexName());
    ts.append("dataPath", getDataPath());
    ts.append("exclusions", getExclusions());
    ts.append("outDataPath", getOutDataPath());
    ts.append("indexMapping", getIndexMapping());
    ts.append("namespaces", getNamespaces());

    return ts.toString();
  }
}
