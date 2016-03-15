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

import org.bedework.util.config.ConfInfo;
import org.bedework.util.jmx.MBeanInfo;

import java.util.List;

/** Properties for category service.
 *
 * @author douglm
 *
 */
@ConfInfo(elementName = "category")
public interface CategoryConfigProperties {
  /**
   *
   * @return base name of category index
   */
  @MBeanInfo("base name of category index")
  String getIndexName();

  /** 
   *
   * @param val base name of category index
   */
  void setIndexName(String val);

  /** Get the file path for dmoz structure data
   *
   * @return name
   */
  @MBeanInfo("file path for dmoz structure data")
  String getDataPath();

  /** Set the file path for dmoz structure data
   *
   * @param val the path
   */
  void setDataPath(String val);

  /** Get the file path for generated data
   *
   * @return name
   */
  @MBeanInfo("file path for generated data")
  String getOutDataPath();

  /** Set the file path for generated data
   *
   * @param val the path
   */
  void setOutDataPath(String val);

  /**
   *
   * @param val the index mapping location
   */
  void setIndexMapping(String val);

  /** Get the index mapping location
   *
   * @return the index mapping location
   */
  @MBeanInfo("index mapping location")
  String getIndexMapping();

  /**
   *
   * @param val namespaces, abbrev + uri
   */
  void setNamespaces(List<String> val);

  /**
   *
   * @return namespaces, abbrev + uri
   */
  @MBeanInfo("namespaces, abbrev + uri")
  List<String> getNamespaces();

  /**
   *
   * @param abbrev of namespace, e.g. "dmoz"
   * @param uri e.g. "https://dmoz.org/"
   */
  @MBeanInfo("add namespace, abbrev + uri")
  void addNamespace(String abbrev,
                    String uri);

  /**
   *
   * @param abbrev of namespace
   * @return namespace uri or null
   */
  @MBeanInfo("get namespace given abbrev")
  String getNamespace(String abbrev);

  /**
   *
   * @param abbrev of namespace
   */
  void removeNamespace(String abbrev);

  /**
   *
   * @param abbrev of namespace, e.g. "dmoz"
   * @param uri e.g. "https://dmoz.org/"
   */
  @MBeanInfo("set a namespace, abbrev + uri")
  void setNamespace(String abbrev,
                    String uri);
}
