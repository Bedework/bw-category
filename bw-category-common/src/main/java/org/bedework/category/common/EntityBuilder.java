/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.category.common;

import org.bedework.category.common.Category.CategoryChild;
import org.bedework.util.elasticsearch.EntityBuilderBase;
import org.bedework.util.indexing.IndexException;
import org.bedework.util.misc.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: mike Date: 3/14/16 Time: 16:31
 */
public class EntityBuilder extends EntityBuilderBase {
  /**
   * Constructor - 1 use per entity
   *
   * @param fields  map of fields from index
   * @param version of document
   */
  EntityBuilder(final Map<String, ?> fields,
                final long version) throws IndexException {
    super(fields, version);
  }


  Category makeCategory() throws IndexException {
    final Category c = new Category();

    c.setHref(getString("href"));
    c.setCatId(getInt("catId"));
    c.setNamespaceAbbrev(getString("namespaceAbbrev"));
    c.setLast(getString("last"));
    c.setLowerLast(getString("lowerLast"));
    c.setHrefElements(getFieldValues("hrefElements"));
    c.setHrefDepth(getInt("hrefDepth"));
    c.setLastUpdate(getString("lastUpdate"));
    c.setDescription(getString("description"));
    c.setChildren(makeCatChildren());

    return c;
  }

  private List<CategoryChild> makeCatChildren() throws IndexException {
    final List<Object> vals = getFieldValues("children");

    if (Util.isEmpty(vals)) {
      return null;
    }

    final List<CategoryChild> ccs = new ArrayList<>();

    for (final Object o: vals) {
      try {
        pushFields(o);

        final CategoryChild cc = makeCategoryChild();

        ccs.add(cc);
      } finally {
        popFields();
      }
    }

    return ccs;
  }

  private CategoryChild makeCategoryChild() {
    final CategoryChildImpl cci = new CategoryChildImpl();

    cci.setSort(getInt("sort"));
    cci.setHref(getString("href"));

    return cci;
  }
}
