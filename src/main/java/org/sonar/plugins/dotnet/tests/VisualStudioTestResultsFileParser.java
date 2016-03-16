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

import com.google.common.base.Preconditions;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisualStudioTestResultsFileParser implements UnitTestResultsParser {

  private static final Logger LOG = LoggerFactory.getLogger(VisualStudioTestResultsFileParser.class);

  @Override
  public void parse(File file, UnitTestResults unitTestResults) {
    LOG.info("Parsing the Visual Studio Test Results file " + file.getAbsolutePath());
    new Parser(file, unitTestResults).parse();
  }

  private static class Parser {

    private final File file;
    private XmlParserHelper xmlParserHelper;
    private final UnitTestResults unitTestResults;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private Pattern millisecondsPattern = Pattern.compile("(\\.([0-9]{0,3}))[0-9]*+");

    private boolean foundCounters;

    public Parser(File file, UnitTestResults unitTestResults) {
      this.file = file;
      this.unitTestResults = unitTestResults;
    }

    public void parse() {
      try {
        xmlParserHelper = new XmlParserHelper(file);
        checkRootTag();
        dispatchTags();
        Preconditions.checkArgument(foundCounters, "The mandatory <Counters> tag is missing in " + file.getAbsolutePath());
      } finally {
        if (xmlParserHelper != null) {
          xmlParserHelper.close();
        }
      }
    }

    private void dispatchTags() {
      String tagName;
      while ((tagName = xmlParserHelper.nextStartTag()) != null) {
        if ("Counters".equals(tagName)) {
          handleCountersTag();
        } else if ("Times".equals(tagName)) {
          handleTimesTag();
        }
      }
    }

    private void handleCountersTag() {
      foundCounters = true;

      int passed = xmlParserHelper.getIntAttributeOrZero("passed");
      int failed = xmlParserHelper.getIntAttributeOrZero("failed");
      int errors = xmlParserHelper.getIntAttributeOrZero("error");
      int timeout = xmlParserHelper.getIntAttributeOrZero("timeout");
      int aborted = xmlParserHelper.getIntAttributeOrZero("aborted");

      int inconclusive = xmlParserHelper.getIntAttributeOrZero("inconclusive");

      int tests = passed + failed + errors + timeout + aborted;
      int skipped = inconclusive;
      int failures = timeout + failed + aborted;

      unitTestResults.add(tests, passed, skipped, failures, errors, null);
    }

    private void handleTimesTag() {
      Date start = getRequiredDateAttribute("start");
      Date finish = getRequiredDateAttribute("finish");
      double duration = finish.getTime() - start.getTime();

      unitTestResults.add(0, 0, 0, 0, 0, duration);
    }

    private Date getRequiredDateAttribute(String name) {
      String value = xmlParserHelper.getRequiredAttribute(name);
      try {
        value = keepOnlyMilliseconds(value);
        return dateFormat.parse(value);
      } catch (ParseException e) {
        throw xmlParserHelper.parseError("Expected an valid date and time instead of \"" + value + "\" for the attribute \"" + name + "\". " + e.getMessage());
      }
    }

    private String keepOnlyMilliseconds(String value) {
      StringBuffer sb = new StringBuffer();

      Matcher matcher = millisecondsPattern.matcher(value);
      while (matcher.find()) {
        String milliseconds = matcher.group(2);
        String trailingZeros = "";
        for (int i = 0; i < 3 - milliseconds.length(); i++) {
          trailingZeros += "0";
        }
        matcher.appendReplacement(sb, "$1" + trailingZeros);
      }
      matcher.appendTail(sb);

      return sb.toString();
    }

    private void checkRootTag() {
      xmlParserHelper.checkRootTag("TestRun");
    }

  }

}
