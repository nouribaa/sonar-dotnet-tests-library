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

import com.google.common.collect.ImmutableList;
import edu.emory.mathcs.backport.java.util.Collections;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UnitTestResultsImportSensorTest {

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);

    when(unitTestResultsAggregator.hasUnitTestResultsProperty()).thenReturn(true);
    assertThat(new UnitTestResultsImportSensor(unitTestResultsAggregator).shouldExecuteOnProject(project)).isTrue();

    when(unitTestResultsAggregator.hasUnitTestResultsProperty()).thenReturn(false);
    assertThat(new UnitTestResultsImportSensor(unitTestResultsAggregator).shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void analyze() {
    UnitTestResults results = mock(UnitTestResults.class);
    when(results.tests()).thenReturn(42.0);
    when(results.passedPercentage()).thenReturn(84.0);
    when(results.skipped()).thenReturn(1.0);
    when(results.failures()).thenReturn(2.0);
    when(results.errors()).thenReturn(3.0);
    when(results.executionTime()).thenReturn(321.0);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    SensorContext context = mock(SensorContext.class);

    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    new UnitTestResultsImportSensor(unitTestResultsAggregator).analyze(context, results);

    verify(unitTestResultsAggregator).aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.eq(results));

    verify(context).saveMeasure(CoreMetrics.TESTS, 42.0);
    verify(context).saveMeasure(CoreMetrics.TEST_SUCCESS_DENSITY, 84.0);
    verify(context).saveMeasure(CoreMetrics.SKIPPED_TESTS, 1.0);
    verify(context).saveMeasure(CoreMetrics.TEST_FAILURES, 2.0);
    verify(context).saveMeasure(CoreMetrics.TEST_ERRORS, 3.0);
    verify(context).saveMeasure(CoreMetrics.TEST_EXECUTION_TIME, 321.0);
  }

  @Test
  public void should_not_save_metrics_with_empty_results() {
    SensorContext context = mock(SensorContext.class);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    UnitTestResults results = mock(UnitTestResults.class);
    when(results.tests()).thenReturn(0.0);
    when(results.skipped()).thenReturn(1.0);
    when(results.failures()).thenReturn(2.0);
    when(results.errors()).thenReturn(3.0);
    when(results.executionTime()).thenReturn(null);
    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    new UnitTestResultsImportSensor(unitTestResultsAggregator).analyze(context, results);

    verify(unitTestResultsAggregator).aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.eq(results));
    verify(context).saveMeasure(CoreMetrics.TESTS, 0.0);
    verify(context).saveMeasure(CoreMetrics.SKIPPED_TESTS, 1.0);
    verify(context).saveMeasure(CoreMetrics.TEST_FAILURES, 2.0);
    verify(context).saveMeasure(CoreMetrics.TEST_ERRORS, 3.0);
    verify(context, Mockito.never()).saveMeasure(Mockito.eq(CoreMetrics.TEST_EXECUTION_TIME), Mockito.anyDouble());
    verify(context, Mockito.never()).saveMeasure(Mockito.eq(CoreMetrics.TEST_SUCCESS_DENSITY), Mockito.anyDouble());
  }

  @Test
  public void should_analyze_on_reactor_project() {
    Project project = mock(Project.class);
    when(project.isRoot()).thenReturn(true);
    when(project.getModules()).thenReturn(ImmutableList.of(mock(Project.class)));

    SensorContext context = mock(SensorContext.class);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    UnitTestResults results = mock(UnitTestResults.class);
    when(results.tests()).thenReturn(1.0);
    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    new UnitTestResultsImportSensor(unitTestResultsAggregator).analyse(project, context);

    verify(context, Mockito.atLeastOnce()).saveMeasure(Mockito.any(Metric.class), Mockito.anyDouble());
  }

  @Test
  public void should_not_analyze_on_multi_module_modules() {
    Project project = mock(Project.class);
    when(project.isRoot()).thenReturn(false);

    SensorContext context = mock(SensorContext.class);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    UnitTestResults results = mock(UnitTestResults.class);
    when(results.tests()).thenReturn(1.0);
    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    new UnitTestResultsImportSensor(unitTestResultsAggregator).analyse(project, context);

    verify(context, Mockito.never()).saveMeasure(Mockito.any(Metric.class), Mockito.anyDouble());
  }

  @Test
  public void should_analyze_on_non_multi_module_project() {
    Project project = mock(Project.class);
    when(project.isRoot()).thenReturn(true);
    when(project.getModules()).thenReturn(Collections.emptyList());

    SensorContext context = mock(SensorContext.class);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    UnitTestResults results = mock(UnitTestResults.class);
    when(results.tests()).thenReturn(1.0);
    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    new UnitTestResultsImportSensor(unitTestResultsAggregator).analyse(project, context);

    verify(context, Mockito.atLeastOnce()).saveMeasure(Mockito.any(Metric.class), Mockito.anyDouble());
  }

}
