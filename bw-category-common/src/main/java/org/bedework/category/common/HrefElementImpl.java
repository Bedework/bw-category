/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.category.common;

import org.bedework.util.misc.ToString;

/**
 * User: mike
 * Date: 5/6/15
 * Time: 3:38 PM
 */
class HrefElementImpl implements Category.HrefElement {
    private String displayName;

    /** for Json
     *
     */
    public HrefElementImpl() {
    }

    public HrefElementImpl(final String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public String toString() {
        final ToString ts = new ToString(this);

        ts.append("displayName", getDisplayName());

        return ts.toString();
    }
}
