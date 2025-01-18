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

import org.bedework.base.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Representation of category. Based on dmoz topic.
 *
 * User: mike
 */
public class Category implements Comparable<Category> {
  public final static String nsabbrevDmoz = "dmoz";

  public final static String docType = "category";

  /**
   * Used in the result type below. 
   */
  public interface HrefElement {
    String getDisplayName();
  }

  private String href;
  private int catId;
  private String lastUpdate;
  private String title;
  private String description;

  public interface CategoryChild 
          extends Comparable<CategoryChild> {
    /**
     * @return 0, 1 or 2 - dmoz puts 2 at top
     */
    int getSort();

    String getHref();
  }

  private Set<CategoryChild> children = new TreeSet<>();

  private int sort;
  
  /* Fields derived from the href */

  private int hrefDepth;

  private String namespaceAbbrev;

  private String last;

  private String lowerLast;

  private List<HrefElement> hrefElements;
  
  public String getHref() {
    return href;
  }

  public void setHref(final String val) {
    href = val;
  }

  public int getCatId() {
    return catId;
  }

  public void setCatId(final int val) {
    catId = val;
  }

  public String getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(final String val) {
    lastUpdate = val;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(final String val) {
    title = val;
  }

  /**
   *
   * @return Dmoz English description.
   */
  public String getDescription() {
    return description;
  }

  public void setDescription(final String val) {
    description = val;
  }

  public int getSort() {
    return sort;
  }

  public void setSort(final int val) {
    sort = val;
  }

  public int getHrefDepth() {
    return hrefDepth;
  }

  public void setHrefDepth(final int val) {
    hrefDepth = val;
  }

  public String getNamespaceAbbrev() {
    return namespaceAbbrev;
  }
  
  public void setNamespaceAbbrev(final String val) {
    namespaceAbbrev = val;
  }

  public String getLast() {
    return last;
  }

  public void setLast(final String val) {
    last = val;
  }

  public String getLowerLast() {
    return lowerLast;
  }

  public void setLowerLast(final String val) {
    lowerLast = val;
  }

  public List<HrefElement> getHrefElements() {
    return hrefElements;
  }
  
  public void setHrefElements(final List<HrefElement> val) {
    hrefElements = val;
  }

  public void addHrefElement(final HrefElement val) {
    if (hrefElements == null) {
      hrefElements = new ArrayList<>();
    }

    hrefElements.add(val);
  }

  /**
   *
   * @return list of child hrefs - each prefixed by 0: 1: 2: corresponding to
   *  narrow, narrow1, narrow2
   */
  public Set<CategoryChild> getChildren() {
    return children;
  }

  public void setChildren(final Set<CategoryChild> val) {
    children = val;
  }

  public void addChild(final CategoryChild val) {
    children.add(val);
  }

  @Override
  public int compareTo(final Category that) {
    if (this == that) {
      return 0;
    }

    return getHref().compareTo(that.getHref());
  }

  public String toString() {
    final ToString ts = new ToString(this);

    ts.append("href", getHref());
    ts.append("catId", getCatId());
    ts.append("lastUpdate", getLastUpdate());
    ts.append("title", getTitle());
    ts.append("description", getDescription());
    ts.append("sort", getSort());
    ts.append("hrefDepth", getHrefDepth());
    ts.append("namespaceAbbrev", getNamespaceAbbrev());
    ts.append("last", getLast());
    ts.append("lowerLast", getLowerLast());
    ts.append("hrefElements", getHrefElements());
    ts.append("children", getChildren());

    return ts.toString();
  }
}
