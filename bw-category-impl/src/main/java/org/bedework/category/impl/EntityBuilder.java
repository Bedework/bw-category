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
package org.bedework.category.impl;

import org.bedework.category.common.Category;
import org.bedework.category.common.CategoryChild;
import org.bedework.util.opensearch.EntityBuilderBase;
import org.bedework.util.misc.Util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
                final long version) {
    super(fields, version);
  }


  Category makeCategory() {
    final Category c = new Category();

    c.setHref(getString("href"));
    c.setCatId(getInt("catId"));
    c.setNamespaceAbbrev(getString("namespaceAbbrev"));
    c.setLast(getString("last"));
    c.setLowerLast(getString("lowerLast"));

    getFieldValues("hrefElements").forEach(
            hrefEl -> c.addHrefElement((Category.HrefElement)hrefEl));

    c.setHrefDepth(getInt("hrefDepth"));
    c.setLastUpdate(getString("lastUpdate"));
    c.setDescription(getString("description"));
    c.setChildren(makeCatChildren());

    return c;
  }

  private Set<CategoryChild> makeCatChildren() {
    final List<Object> vals = getFieldValues("children");

    if (Util.isEmpty(vals)) {
      return null;
    }

    final Set<CategoryChild> ccs = new TreeSet<>();

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
