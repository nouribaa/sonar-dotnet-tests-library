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
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenCoverReportParser implements CoverageParser {

  private static final Logger LOG = LoggerFactory.getLogger(OpenCoverReportParser.class);

  @Override
  public void parse(File file, Coverage coverage) {
    LOG.info("Parsing the OpenCover report " + file.getAbsolutePath());
    new Parser(file, coverage).parse();
  }

  private static class Parser {

    private final File file;
    private XmlParserHelper xmlParserHelper;
    private final Map<String, String> files = Maps.newHashMap();
    private final Coverage coverage;
    private String fileRef;

    public Parser(File file, Coverage coverage) {
      this.file = file;
      this.coverage = coverage;
    }

    public void parse() {
      try {
        xmlParserHelper = new XmlParserHelper(file);
        xmlParserHelper.checkRootTag("CoverageSession");
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
        if ("File".equals(tagName)) {
          handleFileTag();
        } else if ("FileRef".equals(tagName)) {
          handleFileRef();
        } else if ("SequencePoint".equals(tagName)) {
          handleSegmentPointTag();
        }
      }
    }

    private void handleFileRef() {
      this.fileRef = xmlParserHelper.getRequiredAttribute("uid");
    }

    private void handleFileTag() {
      String uid = xmlParserHelper.getRequiredAttribute("uid");
      String fullPath = xmlParserHelper.getRequiredAttribute("fullPath");

      try {
        files.put(uid, new File(fullPath).getCanonicalPath());
      } catch (IOException e) {
        LOG.debug("Skipping the import of OpenCover code coverage for the invalid file path: " + fullPath
          + " at line " + xmlParserHelper.stream().getLocation().getLineNumber(), e);
      }
    }

    private void handleSegmentPointTag() {
      int line = xmlParserHelper.getRequiredIntAttribute("sl");
      int vc = xmlParserHelper.getRequiredIntAttribute("vc");

      String fileId = xmlParserHelper.getAttribute("fileid");
      if (fileId == null) {
        fileId = fileRef;
      }

      if (files.containsKey(fileId)) {
        coverage.addHits(files.get(fileId), line, vc);
      }
    }

  }

}
