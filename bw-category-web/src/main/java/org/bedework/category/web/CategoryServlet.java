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
package org.bedework.category.web;

import org.bedework.category.service.Categories;
import org.bedework.category.service.CategoryConfigPropertiesImpl;
import org.bedework.util.opensearch.EsCtl;
import org.bedework.util.opensearch.EsCtlMBean;
import org.bedework.util.jmx.ConfBase;
import org.bedework.util.jmx.MBeanUtil;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.servlet.MethodBase;
import org.bedework.util.servlet.ServletBase;

import javax.management.ObjectName;
import javax.servlet.ServletException;

import static org.bedework.util.servlet.MethodBase.MethodInfo;

/** This servlet handles the categories reuests and respondes.
 *
 * @author Mike Douglass   mdouglass@sphericalcowgroup.com
 * @version 1.0
 */
public class CategoryServlet extends ServletBase {
  private static EsCtlMBean esCtl;
  
  @Override
  protected void addMethods() {
    addMethod("POST", 
              new MethodInfo(PostMethod.class, true));
    methods.put("GET", new MethodInfo(GetMethod.class, false));
    /*
    methods.put("ACL", new MethodInfo(AclMethod.class, false));
    methods.put("COPY", new MethodInfo(CopyMethod.class, false));
    methods.put("HEAD", new MethodInfo(HeadMethod.class, false));
    methods.put("OPTIONS", new MethodInfo(OptionsMethod.class, false));
    methods.put("PROPFIND", new MethodInfo(PropFindMethod.class, false));

    methods.put("DELETE", new MethodInfo(DeleteMethod.class, true));
    methods.put("MKCOL", new MethodInfo(MkcolMethod.class, true));
    methods.put("MOVE", new MethodInfo(MoveMethod.class, true));
    methods.put("POST", new MethodInfo(PostMethod.class, true));
    methods.put("PROPPATCH", new MethodInfo(PropPatchMethod.class, true));
    methods.put("PUT", new MethodInfo(PutMethod.class, true));
    */

    //methods.put("LOCK", new MethodInfo(LockMethod.class, true));
    //methods.put("UNLOCK", new MethodInfo(UnlockMethod.class, true));
  }
  
  protected void initMethodBase(final MethodBase mb,
                                final ConfBase conf,
                                final boolean dumpContent) throws ServletException {
    final Configurator cfg = (Configurator)conf;
    final CategoryMethodBase cmb = (CategoryMethodBase)mb;

    try {
      cmb.init(getEsCtl(), cfg.categories.getConfig(),
               dumpContent);
    } catch (final Throwable t) {
      throw new ServletException(t);
    }
  }

  /**
   *
   * @return Mbean to configure our local copy of EsUtil
   * @throws Throwable on fatal error
   */
  private EsCtlMBean getEsCtl() throws Throwable {
    if (esCtl != null) {
      return esCtl;
    }

    esCtl = (EsCtlMBean)MBeanUtil.getMBean(EsCtlMBean.class,
                                           EsCtlMBean.serviceName);

    return esCtl;
  }

  /* -----------------------------------------------------------------------
   *                         JMX support
   */

  static class Configurator
          extends ConfBase<CategoryConfigPropertiesImpl<?>> {
    Categories categories;
    EsCtl esCtl;

    Configurator() {
      super("org.bedework.categories:service=Categories");
    }

    @Override
    public String loadConfig() {
      return null;
    }

    @Override
    public void start() {
      try {
        getManagementContext().start();

        categories = new Categories();
        register(new ObjectName(categories.getServiceName()),
                 categories);

        categories.loadConfig();
//        categories.start();
        
        esCtl = new EsCtl();
        register(new ObjectName(esCtl.getServiceName()),
                 esCtl);

        esCtl.loadConfig();
      } catch (final Throwable t){
        t.printStackTrace();
      }
    }

    @Override
    public void stop() {
      try {
//        categories.stop();
        getManagementContext().stop();
      } catch (final Throwable t){
        t.printStackTrace();
      }
    }
  }
  
  private static final Configurator conf = new Configurator();
  
  protected ConfBase<?> getConfigurator() {
    return conf;
  }

  /* ==============================================================
   *                   Logged methods
   * ============================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
