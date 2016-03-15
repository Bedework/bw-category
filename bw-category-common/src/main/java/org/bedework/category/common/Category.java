/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.category.common;

import org.bedework.util.misc.ToString;

import java.util.ArrayList;
import java.util.List;

/** Representation of dmoz topic.
 *
 * User: mike
 */
public class Category {
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

  interface CategoryChild {
    /**
     * @return 0, 1 or 2 - dmoz puts 2 at top
     */
    int getSort();

    String getHref();
  }

  private List<CategoryChild> children = new ArrayList<>();

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

  /**
   *
   * @return list of child hrefs - each prefixed by 0: 1: 2: corresponding to
   *  narrow, narrow1, narrow2
   */
  public List<CategoryChild> getChildren() {
    return children;
  }

  public void setChildren(final List<CategoryChild> val) {
    children = val;
  }

  public void addChild(final CategoryChild val) {
    children.add(val);
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
