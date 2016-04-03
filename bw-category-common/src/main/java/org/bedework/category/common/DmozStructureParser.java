/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.category.common;

import org.bedework.util.jmx.InfoLines;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * User: mike
 */
public abstract class DmozStructureParser extends DefaultHandler {
  private Logger log;

  private final static String elTopic = "Topic";
  private final static String elCatid = "catid";
  private final static String elLastUpdate = "lastUpdate";
  private final static String elTitle = "Title";
  private final static String elDescription = "Description";
  private final static String elNarrow = "narrow";
  private final static String elNarrow1 = "narrow1";
  private final static String elNarrow2 = "narrow2";

  private final XMLReader xmlReader;
  private final Path structureDataPath;
  private final CategoryConfigProperties conf;

  private int numTopics;

  private int skippedTopics;
  private int maxDepth;

  private boolean skippingTopic;

  private Category topic;
  private StringBuilder curChars = new StringBuilder();
  
  private Set<String> exclusions;

  public DmozStructureParser(final CategoryConfigProperties conf) throws CategoryException {
    try {
      this.conf = conf;
      structureDataPath = Paths.get(this.conf.getDataPath());

      if (this.conf.getExclusions() != null) {
        Path p = Paths.get(this.conf.getExclusions());
        final File f = p.toFile();

        LineNumberReader lnr = new LineNumberReader(new FileReader(f));
        
        exclusions = new TreeSet<>();
        
        while (true) {
          final String s = lnr.readLine();
          
          if (s == null) {
            break;
          }
          
          if (s.startsWith("#")) {
            continue;
          }
          
          if (s.trim().length() == 0) {
            continue;
          }
          
          exclusions.add(s);
        }
      }

      final SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setNamespaceAware(true);
      final SAXParser saxParser = spf.newSAXParser();
      xmlReader = saxParser.getXMLReader();
      xmlReader.setContentHandler(this);
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }
  
  public abstract void saveCategory(final Category cat)
          throws CategoryException;

  public void parse() throws CategoryException {
    try {
      final File f = structureDataPath.toFile();

      xmlReader.parse(new InputSource(new FileReader(f)));
    } catch (final Throwable t) {
      throw new CategoryException(t);
    }
  }

  public void stats(final InfoLines info) {
    info.add("topics: " + numTopics);
    info.add("skipped topics: " + skippedTopics);
  }

  @Override
  public void startElement (final String uri,
                            final String localName,
                            final String qName,
                            final Attributes attributes) throws SAXException {
    curChars = new StringBuilder();

    if (skippingTopic) {
      return;
    }

    switch (localName) {
      case elTopic:
        numTopics++;
        skippingTopic = false;

        if ((numTopics % 1000) == 0) {
          info("Done " + numTopics + " skipped: " + skippedTopics);
        }

        if (topic != null) {
          warn("Non null topic on entry - discarding");
        }

        final String href = changeTop(attrVal("id", attributes));
        if (href == null) {
          skippingTopic = true;
          skippedTopics++;
          return;
        }

        if (excluded(href)) {
          if (!href.startsWith("/dmoz/World/")) {
            info("Skipping " + href);
          }
          skippingTopic = true;
          skippedTopics++;

          return;
        }

        topic = new Category();

        topic.setHref(href);
        break;

      case elNarrow:
      case elNarrow1:
      case elNarrow2:
        if (topic == null) {
          break;
        }

        /* Use the resource path to indicate depth */
        final String child = changeTop(attrVal("resource", attributes));

        if (child == null) {
          break;
        }

        CategoryChildImpl cci = new CategoryChildImpl();
        cci.setHref(child);

        final String prefix;
        final int sort;
        switch (localName) {
          case elNarrow:
            cci.setSort(0);
            break;
          case elNarrow1:
            cci.setSort(1);
            break;
          default:
            cci.setSort(2);
            break;
        }

        topic.addChild(cci);
        break;
    }
  }

  @Override
  public void endElement (final String uri,
                          final String localName,
                          final String qName) throws SAXException {
    try {
      switch (localName) {
        case elTopic:
          if (skippingTopic) {
            skippingTopic = false;
            topic = null;
            return;
          }

          saveCategory(topic);
          topic = null;
          break;

        case elCatid:
          if (topic != null) {
            topic.setCatId(intVal());
          }
          break;

        case elLastUpdate:
          if (topic != null) {
            topic.setLastUpdate(strVal());
          }
          break;

        case elTitle:
          if (topic != null) {
            topic.setTitle(strVal());
          }
          break;

        case elDescription:
          if (topic != null) {
            topic.setDescription(strVal());
          }
          break;

      }
    } catch (final CategoryException pe) {
      throw new SAXException(pe);
    }
  }

  @Override
  public void characters(final char ch[],
                         final int start,
                         final int length) throws SAXException {
    curChars.append(ch, start, length);
  }

  private boolean excluded(final String href) {
    if (exclusions == null) {
      return false;
    }

    for (final String prefix: exclusions) {
      if (href.startsWith(prefix)) {
        return true;
      }
    }

    return false;
  }

  private String changeTop(final String path) {
    if (path == null) {
      return null;
    }

    if (!path.startsWith("Top/")) {
      warn("Id does not start with \"Top/\" - discarding: " + path);
      return null;
    }

    if (path.endsWith("/")) {
      return "/" + Category.nsabbrevDmoz + path.substring(3);
    }

    return "/" + Category.nsabbrevDmoz + path.substring(3) + "/";
  }

  private int intVal() throws SAXException {
    try {
      return Integer.valueOf(curChars.toString());
    } catch (final Throwable t) {
      throw new SAXException(t.getMessage());
    }
  }

  private String strVal() throws SAXException {
    try {
      return curChars.toString();
    } catch (final Throwable t) {
      throw new SAXException(t.getMessage());
    }
  }

  private String attrVal(final String localName,
                         final Attributes attributes) {
    if (attributes == null) {
      return  null;
    }

    for (int i = 0; i < attributes.getLength(); i++) {
      if (localName.equals(attributes.getLocalName(i))) {
        return attributes.getValue(i);
      }
    }

    return null;
  }

  protected void info(final String msg) {
    getLogger().info(msg);
  }

  protected void warn(final String msg) {
    getLogger().warn(msg);
  }

  protected void debug(final String msg) {
    getLogger().debug(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  protected void error(final String msg) {
    getLogger().error(msg);
  }

  /* Get a logger for messages
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }
}
