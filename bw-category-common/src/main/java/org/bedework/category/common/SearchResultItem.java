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
import org.bedework.base.response.Response;

/**
 * User: mike Date: 3/9/16 Time: 23:54
 */

public class SearchResultItem
        extends Response<SearchResultItem>
        implements Comparable<SearchResultItem> {
  private Category category;
  private String href;
  private float score;

  public SearchResultItem() {
  }

  public SearchResultItem(final Status status) {
    setStatus(status);
  }

  public SearchResultItem(final Status status,
                          final String message) {
    setStatus(status);
    setMessage(message);
  }

  public SearchResultItem(final String href, final float score) {
    this.href = href;
    this.score = score;
  }

  public SearchResultItem(final Category category, final float score) {
    this.category = category;
    this.score = score;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(final Category val) {
    category = val;
  }

  public String getHref() {
    return href;
  }

  public void setHref(final String val) {
    href = val;
  }

  public float getScore() {
    return score;
  }

  public void setScore(final float val) {
    score = val;
  }

  public ToString toStringSegment(final ToString ts) {
    return super.toStringSegment(ts)
                .append("category", getCategory())
                .append("href", getHref())
                .append("score", getScore());
  }

  @Override
  public int compareTo(final SearchResultItem that) {
    // TreeSet sorts by increasing order - we want highest first
    if (this == that) {
      return 0;
    }
    
    if (getScore() < that.getScore()) {
      return 1;
    }

    if (getScore() > that.getScore()) {
      return -1;
    }

    if (href != null) {
      return -getHref().compareTo(that.getHref());
    }

    if (getCategory() == null) {
      if (that.getCategory() != null) {
        return 1;
      }
      
      return 0;
    }
    
    return -getCategory().compareTo(that.getCategory());
  }
} 
