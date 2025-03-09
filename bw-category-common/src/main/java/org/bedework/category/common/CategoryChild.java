package org.bedework.category.common;

public interface CategoryChild
        extends Comparable<CategoryChild> {
  /**
   * @return 0, 1 or 2 - dmoz puts 2 at top
   */
  int getSort();

  String getHref();
}
