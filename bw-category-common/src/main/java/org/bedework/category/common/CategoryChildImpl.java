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

import org.bedework.util.misc.ToString;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * User: mike
 */
public class CategoryChildImpl implements Category.CategoryChild,
        Comparable<Category.CategoryChild> {
  private int sort;
  private String href;

  @Override
  public int getSort() {
    return sort;
  }

  public void setSort(final int val) {
    sort = val;
  }

  @Override
  public String getHref() {
    return href;
  }

  public void setHref(final String val) {
    href = val;
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    ts.append("sort", getSort());
    ts.append("href", getHref());

    return ts.toString();
  }

  @Override
  public boolean equals(final Object that) {
    if (!(that instanceof CategoryChildImpl)) {
      return false;
    }

    return compareTo((Category.CategoryChild)that) == 0;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(19, 31)
            .append(getSort()).
                    append(getHref()).
                    toHashCode();
  }

  @Override
  public int compareTo(final Category.CategoryChild that) {
    if (this == that) {
      return 0;
    }

    return new CompareToBuilder()
            .append(getSort(), that.getSort())
            .append(getHref(), that.getHref())
            .toComparison();
  }
}
