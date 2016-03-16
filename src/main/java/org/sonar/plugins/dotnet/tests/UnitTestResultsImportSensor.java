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

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;

public class UnitTestResultsImportSensor implements Sensor {

  private final WildcardPatternFileProvider wildcardPatternFileProvider = new WildcardPatternFileProvider(new File("."), File.separator);
  private final UnitTestResultsAggregator unitTestResultsAggregator;

  public UnitTestResultsImportSensor(UnitTestResultsAggregator unitTestResultsAggregator) {
    this.unitTestResultsAggregator = unitTestResultsAggregator;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return unitTestResultsAggregator.hasUnitTestResultsProperty();
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    if (project.isRoot()) {
      analyze(context, new UnitTestResults());
    }
  }

  @VisibleForTesting
  void analyze(SensorContext context, UnitTestResults unitTestResults) {
    UnitTestResults aggregatedResults = unitTestResultsAggregator.aggregate(wildcardPatternFileProvider, unitTestResults);

    context.saveMeasure(CoreMetrics.TESTS, aggregatedResults.tests());
    context.saveMeasure(CoreMetrics.TEST_ERRORS, aggregatedResults.errors());
    context.saveMeasure(CoreMetrics.TEST_FAILURES, aggregatedResults.failures());
    context.saveMeasure(CoreMetrics.SKIPPED_TESTS, aggregatedResults.skipped());

    Double executionTime = aggregatedResults.executionTime();
    if (executionTime != null) {
      context.saveMeasure(CoreMetrics.TEST_EXECUTION_TIME, executionTime);
    }

    if (aggregatedResults.tests() > 0) {
      context.saveMeasure(CoreMetrics.TEST_SUCCESS_DENSITY, aggregatedResults.passedPercentage());
    }
  }

}
