/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.category.common;

import org.bedework.util.elasticsearch.EsDocInfo;
import org.bedework.util.elasticsearch.EsUtil;
import org.bedework.util.elasticsearch.IndexProperties;
import org.bedework.util.jmx.InfoLines;
import org.bedework.util.misc.Logged;

import org.elasticsearch.action.get.GetResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: mike Date: 3/13/16 Time: 16:11
 */
public class CategoryIndex extends Logged {
  private final IndexProperties idxprops;
  private final CategoryConfigProperties conf;
  
  /** Index we are currently working with */
  private String targetIndex;
  
  public CategoryIndex(final IndexProperties idxprops,
                       final CategoryConfigProperties conf)
          throws CategoryException {
    this.idxprops = idxprops;
    this.conf = conf;
  }

  /** Create a new index and make it current
   * 
   * @return new index name
   * @throws CategoryException on fatal error
   */
  public String newIndex() throws CategoryException {
    try {
      targetIndex = getEsUtil().newIndex(conf.getIndexName(),
                                         conf.getIndexMapping());
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }

    return targetIndex;
  }
  
  public List<String> purgeIndexes() throws CategoryException {
    try {
      return getEsUtil().purgeIndexes(
              Collections.singleton(conf.getIndexName()));
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }


  /** Parse the dmoz data and store it in the current index
   * 
   * @param infoLines for messsages
   * @throws CategoryException on fatal error
   */
  public void parseDmoz(final InfoLines infoLines) throws CategoryException {
    final long startTime = System.currentTimeMillis();

    final DmozStructureParser parser =
            new DmozStructureParser(conf) {
              @Override
              public void saveCategory(final Category cat)
                      throws CategoryException {
                CategoryIndex.this.saveCategory(cat);
              }
            };

    parser.parse();

    parser.stats(infoLines);

    final String times = "Index build: " + elapsed(startTime);
    infoLines.addLn(times);
  }

  /** Save the category object in the current index
   * 
   * @param cat the category
   * @throws CategoryException on fatal error
   */
  public void saveCategory(final Category cat)
          throws CategoryException {
    /* Set up fields derived from href */
    final String hr = cat.getHref();
    cat.setNamespaceAbbrev(hr.substring(0, hr.indexOf('/', 1)));

    final String[] els = hr.split("/");

    final List<Category.HrefElement> hes = new ArrayList<>(els.length);

    // skip empty strings - should have one at start

    for (final String s: els) {
      if (s.length() == 0) {
        continue;
      }

      hes.add(new HrefElementImpl(s.replace('_', ' ')));
    }

    cat.setHrefElements(Collections.unmodifiableList(hes));
    cat.setHrefDepth(cat.getHrefElements().size());

    cat.setLast(hes.get(cat.getHrefDepth() - 1).getDisplayName());
    cat.setLowerLast(cat.getLast().toLowerCase());

    EsDocInfo di = new DocBuilder().makeDoc(cat);
    
    try {
      getEsUtil().indexDoc(di, targetIndex);
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }
  
  public Category getCategory(final String href) throws CategoryException {
    if (debug) {
      debug("getCategory: target=" + targetIndex + " href=" + href);
    }

    final GetResponse gr;
    try {
      gr = getEsUtil().get(targetIndex,
                           Category.docType,
                           href);

      if (gr == null) {
        return null;
      }

      return new EntityBuilder(gr.getSourceAsMap(),
                               gr.getVersion()).makeCategory();
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }
  
  public void makeProduction() throws CategoryException {
    try {
      getEsUtil().swapIndex(targetIndex, conf.getIndexName());
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }

  private EsUtil getEsUtil() throws Throwable {
    return new EsUtil(idxprops);
  }

  private String elapsed(final long start) {
    final long millis = System.currentTimeMillis() - start;
    long seconds = millis / 1000;
    final long minutes = seconds / 60;
    seconds -= (minutes * 60);

    return "Elapsed time: " + minutes + ":" +
            twoDigits(seconds);
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
}
