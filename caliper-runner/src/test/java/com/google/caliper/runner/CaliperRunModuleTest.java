/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.caliper.runner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.caliper.Benchmark;
import com.google.caliper.core.InvalidBenchmarkException;
import com.google.caliper.model.InstrumentType;
import com.google.caliper.runner.Instrument.Instrumentation;
import com.google.caliper.runner.options.CaliperOptions;
import com.google.caliper.runner.platform.Platform;
import com.google.caliper.runner.platform.SupportedPlatform;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Method;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/** Tests {@link CaliperRunModule}. */
@RunWith(MockitoJUnitRunner.class)
public class CaliperRunModuleTest {
  private Instrument instrumentA = new FakeInstrument();
  private Instrument instrumentB = new FakeInstrument();

  @Mock CaliperOptions options;

  private Method methodA;
  private Method methodB;
  private Method methodC;

  @Before
  public void setUp() throws Exception {
    methodA = TestBenchmark.class.getDeclaredMethod("a");
    methodB = TestBenchmark.class.getDeclaredMethod("b");
    methodC = TestBenchmark.class.getDeclaredMethod("c");
  }

  @Test
  public void provideInstrumentations_noNames() throws Exception {
    when(options.benchmarkMethodNames()).thenReturn(ImmutableSet.<String>of());
    assertEquals(
        new ImmutableSet.Builder<Instrumentation>()
            .add(instrumentA.createInstrumentation(methodA))
            .add(instrumentA.createInstrumentation(methodB))
            .add(instrumentA.createInstrumentation(methodC))
            .add(instrumentB.createInstrumentation(methodA))
            .add(instrumentB.createInstrumentation(methodB))
            .add(instrumentB.createInstrumentation(methodC))
            .build(),
        CaliperRunModule.provideInstrumentations(
            options,
            BenchmarkClass.forClass(TestBenchmark.class),
            ImmutableSet.of(instrumentA, instrumentB)));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void provideInstrumentations_withNames() throws Exception {
    when(options.benchmarkMethodNames())
        .thenReturn(ImmutableSet.of("b"), ImmutableSet.of("a", "c"));
    assertEquals(
        new ImmutableSet.Builder<Instrumentation>()
            .add(instrumentA.createInstrumentation(methodB))
            .add(instrumentB.createInstrumentation(methodB))
            .build(),
        CaliperRunModule.provideInstrumentations(
            options,
            BenchmarkClass.forClass(TestBenchmark.class),
            ImmutableSet.of(instrumentA, instrumentB)));
    assertEquals(
        new ImmutableSet.Builder<Instrumentation>()
            .add(instrumentA.createInstrumentation(methodA))
            .add(instrumentA.createInstrumentation(methodC))
            .add(instrumentB.createInstrumentation(methodA))
            .add(instrumentB.createInstrumentation(methodC))
            .build(),
        CaliperRunModule.provideInstrumentations(
            options,
            BenchmarkClass.forClass(TestBenchmark.class),
            ImmutableSet.of(instrumentA, instrumentB)));
  }

  @Test
  public void provideInstrumentations_withInvalidName() {
    when(options.benchmarkMethodNames()).thenReturn(ImmutableSet.of("a", "c", "bad"));
    try {
      CaliperRunModule.provideInstrumentations(
          options,
          BenchmarkClass.forClass(TestBenchmark.class),
          ImmutableSet.of(instrumentA, instrumentB));
      fail("should have thrown for invalid benchmark method name");
    } catch (Exception expected) {
      assertTrue(expected.getMessage().contains("[bad]"));
    }
  }

  static final class TestBenchmark {
    @Benchmark
    void a() {}

    @Benchmark
    void b() {}

    @Benchmark
    void c() {}
  }

  @SupportedPlatform(Platform.Type.JVM)
  static final class FakeInstrument extends Instrument {
    @Override
    public boolean isBenchmarkMethod(Method method) {
      return true;
    }

    @Override
    public Instrumentation createInstrumentation(Method benchmarkMethod) {
      return new Instrumentation(benchmarkMethod) {
        @Override
        public InstrumentType type() {
          throw new UnsupportedOperationException();
        }

        @Override
        public void dryRun(Object benchmark) throws InvalidBenchmarkException {}

        @Override
        MeasurementCollectingVisitor getMeasurementCollectingVisitor() {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public TrialSchedulingPolicy schedulingPolicy() {
      return TrialSchedulingPolicy.SERIAL;
    }
  }
}
