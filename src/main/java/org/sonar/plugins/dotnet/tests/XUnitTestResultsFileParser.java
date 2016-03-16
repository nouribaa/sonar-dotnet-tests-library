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

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XUnitTestResultsFileParser implements UnitTestResultsParser {

  private static final Logger LOG = LoggerFactory.getLogger(XUnitTestResultsFileParser.class);

  @Override
  public void parse(File file, UnitTestResults unitTestResults) {
    LOG.info("Parsing the XUnit Test Results file " + file.getAbsolutePath());
    new Parser(file, unitTestResults).parse();
  }

  private static class Parser {

    private final File file;
    private XmlParserHelper xmlParserHelper;
    private final UnitTestResults unitTestResults;

    public Parser(File file, UnitTestResults unitTestResults) {
      this.file = file;
      this.unitTestResults = unitTestResults;
    }

    public void parse() {
      try {
        xmlParserHelper = new XmlParserHelper(file);

        String tag = xmlParserHelper.nextStartTag();
        if (!"assemblies".equals(tag) && !"assembly".equals(tag)) {
          throw xmlParserHelper.parseError("Expected either an <assemblies> or an <assembly> root tag, but got <" + tag + "> instead.");
        }

        do {
          if ("assembly".equals(tag)) {
            handleAssemblyTag();
          }
        } while ((tag = xmlParserHelper.nextStartTag()) != null);
      } finally {
        if (xmlParserHelper != null) {
          xmlParserHelper.close();
        }
      }
    }

    private void handleAssemblyTag() {
      int total = xmlParserHelper.getRequiredIntAttribute("total");
      int passed = xmlParserHelper.getRequiredIntAttribute("passed");
      int failed = xmlParserHelper.getRequiredIntAttribute("failed");
      int skipped = xmlParserHelper.getRequiredIntAttribute("skipped");
      int errors = xmlParserHelper.getIntAttributeOrZero("errors");
      double executionTime = xmlParserHelper.getRequiredDoubleAttribute("time") * 1000;

      unitTestResults.add(total, passed, skipped, failed, errors, executionTime);
    }

  }

}
