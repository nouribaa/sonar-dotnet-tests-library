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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;

public class CoverageReportImportSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(CoverageReportImportSensor.class);

  private static final Map<Metric, Metric> INTEGRATION_TEST_METRICS_MAP = ImmutableMap.<Metric, Metric>builder()
    .put(CoreMetrics.LINES_TO_COVER, CoreMetrics.IT_LINES_TO_COVER)
    .put(CoreMetrics.UNCOVERED_LINES, CoreMetrics.IT_UNCOVERED_LINES)
    .put(CoreMetrics.COVERAGE_LINE_HITS_DATA, CoreMetrics.IT_COVERAGE_LINE_HITS_DATA)
    .put(CoreMetrics.CONDITIONS_TO_COVER, CoreMetrics.IT_CONDITIONS_TO_COVER)
    .put(CoreMetrics.UNCOVERED_CONDITIONS, CoreMetrics.IT_UNCOVERED_CONDITIONS)
    .put(CoreMetrics.COVERED_CONDITIONS_BY_LINE, CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE)
    .put(CoreMetrics.CONDITIONS_BY_LINE, CoreMetrics.IT_CONDITIONS_BY_LINE)
    .build();

  private final WildcardPatternFileProvider wildcardPatternFileProvider = new WildcardPatternFileProvider(new File("."), File.separator);
  private final CoverageConfiguration coverageConf;
  private final CoverageAggregator coverageAggregator;
  private final FileSystem fs;
  private final boolean isIntegrationTest;

  public CoverageReportImportSensor(CoverageConfiguration coverageConf, CoverageAggregator coverageAggregator, FileSystem fs, boolean isIntegrationTest) {
    this.coverageConf = coverageConf;
    this.coverageAggregator = coverageAggregator;
    this.fs = fs;
    this.isIntegrationTest = isIntegrationTest;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return coverageAggregator.hasCoverageProperty();
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    analyze(context, new Coverage());
  }

  @VisibleForTesting
  void analyze(SensorContext context, Coverage coverage) {
    coverageAggregator.aggregate(wildcardPatternFileProvider, coverage);
    CoverageMeasuresBuilder coverageMeasureBuilder = CoverageMeasuresBuilder.create();

    for (String filePath : coverage.files()) {
      InputFile inputFile = fs.inputFile(fs.predicates().and(fs.predicates().hasType(Type.MAIN), fs.predicates().hasAbsolutePath(filePath)));

      if (inputFile == null) {
        LOG.debug("Code coverage will not be imported for the following file outside of SonarQube: " + filePath);
        continue;
      }

      if (!coverageConf.languageKey().equals(inputFile.language())) {
        continue;
      }

      coverageMeasureBuilder.reset();
      for (Map.Entry<Integer, Integer> entry : coverage.hits(filePath).entrySet()) {
        coverageMeasureBuilder.setHits(entry.getKey(), entry.getValue());
      }

      for (Measure measure : coverageMeasureBuilder.createMeasures()) {
        if (isIntegrationTest) {
          convertToIntegrationTestMeasure(measure);
        }
        context.saveMeasure(inputFile, measure);
      }
    }
  }

  private static void convertToIntegrationTestMeasure(Measure measure) {
    Metric metric = measure.getMetric();
    Metric itMetric = INTEGRATION_TEST_METRICS_MAP.get(metric);
    Preconditions.checkNotNull(itMetric, "Could not map metric \"" + itMetric.getKey() + "\" to an integration test one.");
    measure.setMetric(itMetric);
  }

}
