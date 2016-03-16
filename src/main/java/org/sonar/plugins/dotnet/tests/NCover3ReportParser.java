/*
 * SonarQube .NET Tests Library
 * Copyright (C) 2014-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.dotnet.tests;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class NCover3ReportParser implements CoverageParser {

  private static final Logger LOG = LoggerFactory.getLogger(NCover3ReportParser.class);

  @Override
  public void parse(File file, Coverage coverage) {
    LOG.info("Parsing the NCover3 report " + file.getAbsolutePath());
    new Parser(file, coverage).parse();
  }

  private static class Parser {

    private final File file;
    private XmlParserHelper xmlParserHelper;
    private final Map<String, String> documents = Maps.newHashMap();
    private final Coverage coverage;

    public Parser(File file, Coverage coverage) {
      this.file = file;
      this.coverage = coverage;
    }

    public void parse() {
      try {
        xmlParserHelper = new XmlParserHelper(file);
        checkRootTag();
        dispatchTags();
      } finally {
        if (xmlParserHelper != null) {
          xmlParserHelper.close();
        }
      }
    }

    private void dispatchTags() {
      String tagName;
      while ((tagName = xmlParserHelper.nextStartTag()) != null) {
        if ("doc".equals(tagName)) {
          handleDocTag();
        } else if ("seqpnt".equals(tagName)) {
          handleSegmentPointTag();
        }
      }
    }

    private void handleDocTag() {
      String id = xmlParserHelper.getRequiredAttribute("id");
      String url = xmlParserHelper.getRequiredAttribute("url");

      if (!isExcludedId(id)) {
        try {
          documents.put(id, new File(url).getCanonicalPath());
        } catch (IOException e) {
          LOG.debug("Skipping the import of NCover3 code coverage for the invalid file path: " + url
            + " at line " + xmlParserHelper.stream().getLocation().getLineNumber(), e);
        }
      }
    }

    private static boolean isExcludedId(String id) {
      return "0".equals(id);
    }

    private void handleSegmentPointTag() {
      String doc = xmlParserHelper.getRequiredAttribute("doc");
      int line = xmlParserHelper.getRequiredIntAttribute("l");
      int vc = xmlParserHelper.getRequiredIntAttribute("vc");

      if (documents.containsKey(doc) && !isExcludedLine(line)) {
        coverage.addHits(documents.get(doc), line, vc);
      }
    }

    private static boolean isExcludedLine(Integer line) {
      return 0 == line;
    }

    private void checkRootTag() {
      xmlParserHelper.checkRootTag("coverage");
      xmlParserHelper.checkRequiredAttribute("exportversion", 3);
    }

  }

}
