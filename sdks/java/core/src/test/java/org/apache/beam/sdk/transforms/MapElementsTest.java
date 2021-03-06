/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.transforms;

import static org.apache.beam.sdk.transforms.display.DisplayDataMatchers.hasDisplayItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.util.Set;
import org.apache.beam.sdk.testing.NeedsRunner;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.ValidatesRunner;
import org.apache.beam.sdk.transforms.display.DisplayData;
import org.apache.beam.sdk.transforms.display.DisplayDataEvaluator;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link MapElements}.
 */
@RunWith(JUnit4.class)
public class MapElementsTest implements Serializable {

  @Rule
  public final transient TestPipeline pipeline = TestPipeline.create();

  @Rule
  public transient ExpectedException thrown = ExpectedException.none();

  /**
   * A {@link SimpleFunction} to test that the coder registry can propagate coders
   * that are bound to type variables.
   */
  private static class PolymorphicSimpleFunction<T> extends SimpleFunction<T, T> {
    @Override
    public T apply(T input) {
      return input;
    }
  }

  /**
   * A {@link SimpleFunction} to test that the coder registry can propagate coders
   * that are bound to type variables, when the variable appears nested in the
   * output.
   */
  private static class NestedPolymorphicSimpleFunction<T> extends SimpleFunction<T, KV<T, String>> {
    @Override
    public KV<T, String> apply(T input) {
      return KV.of(input, "hello");
    }
  }

  /**
   * Basic test of {@link MapElements} with a {@link SimpleFunction}.
   */
  @Test
  @Category(NeedsRunner.class)
  public void testMapBasic() throws Exception {
    PCollection<Integer> output = pipeline
        .apply(Create.of(1, 2, 3))
        .apply(MapElements.via(new SimpleFunction<Integer, Integer>() {
          @Override
          public Integer apply(Integer input) {
            return -input;
          }
        }));

    PAssert.that(output).containsInAnyOrder(-2, -1, -3);
    pipeline.run();
  }

  /**
   * Basic test of {@link MapElements} coder propagation with a parametric {@link SimpleFunction}.
   */
  @Test
  public void testPolymorphicSimpleFunction() throws Exception {
    pipeline.enableAbandonedNodeEnforcement(false);

    PCollection<Integer> output = pipeline
        .apply(Create.of(1, 2, 3))

        // This is the function that needs to propagate the input T to output T
        .apply("Polymorphic Identity", MapElements.via(new PolymorphicSimpleFunction<Integer>()))

        // This is a consumer to ensure that all coder inference logic is executed.
        .apply("Test Consumer", MapElements.via(new SimpleFunction<Integer, Integer>() {
          @Override
          public Integer apply(Integer input) {
            return input;
          }
        }));
  }

  /**
   * Test of {@link MapElements} coder propagation with a parametric {@link SimpleFunction}
   * where the type variable occurs nested within other concrete type constructors.
   */
  @Test
  public void testNestedPolymorphicSimpleFunction() throws Exception {
    pipeline.enableAbandonedNodeEnforcement(false);

    PCollection<Integer> output =
        pipeline
            .apply(Create.of(1, 2, 3))

            // This is the function that needs to propagate the input T to output T
            .apply(
                "Polymorphic Identity",
                MapElements.via(new NestedPolymorphicSimpleFunction<Integer>()))

            // This is a consumer to ensure that all coder inference logic is executed.
            .apply(
                "Test Consumer",
                MapElements.via(
                    new SimpleFunction<KV<Integer, String>, Integer>() {
                      @Override
                      public Integer apply(KV<Integer, String> input) {
                        return 42;
                      }
                    }));
  }

  /**
   * Basic test of {@link MapElements} with a {@link SerializableFunction}. This style is
   * generally discouraged in Java 7, in favor of {@link SimpleFunction}.
   */
  @Test
  @Category(NeedsRunner.class)
  public void testMapBasicSerializableFunction() throws Exception {
    PCollection<Integer> output = pipeline
        .apply(Create.of(1, 2, 3))
        .apply(MapElements.via(new SerializableFunction<Integer, Integer>() {
          @Override
          public Integer apply(Integer input) {
            return -input;
          }
        }).withOutputType(new TypeDescriptor<Integer>() {}));

    PAssert.that(output).containsInAnyOrder(-2, -1, -3);
    pipeline.run();
  }

  /**
   * Tests that when built with a concrete subclass of {@link SimpleFunction}, the type descriptor
   * of the output reflects its static type.
   */
  @Test
  @Category(NeedsRunner.class)
  public void testSimpleFunctionOutputTypeDescriptor() throws Exception {
    PCollection<String> output = pipeline
        .apply(Create.of("hello"))
        .apply(MapElements.via(new SimpleFunction<String, String>() {
          @Override
          public String apply(String input) {
            return input;
          }
        }));
    assertThat(output.getTypeDescriptor(),
        equalTo((TypeDescriptor<String>) new TypeDescriptor<String>() {}));
    assertThat(pipeline.getCoderRegistry().getDefaultCoder(output.getTypeDescriptor()),
        equalTo(pipeline.getCoderRegistry().getDefaultCoder(new TypeDescriptor<String>() {})));

    // Make sure the pipeline runs too
    pipeline.run();
  }

  @Test
  @Category(NeedsRunner.class)
  public void testVoidValues() throws Exception {
    pipeline
        .apply(Create.of("hello"))
        .apply(WithKeys.<String, String>of("k"))
        .apply(new VoidValues<String, String>() {});
    // Make sure the pipeline runs
    pipeline.run();
  }

  @Test
  public void testSerializableFunctionDisplayData() {
    SerializableFunction<Integer, Integer> serializableFn =
        new SerializableFunction<Integer, Integer>() {
          @Override
          public Integer apply(Integer input) {
            return input;
          }
        };

    MapElements<?, ?> serializableMap = MapElements.via(serializableFn)
        .withOutputType(TypeDescriptor.of(Integer.class));
    assertThat(DisplayData.from(serializableMap),
        hasDisplayItem("mapFn", serializableFn.getClass()));
  }

  @Test
  public void testSimpleFunctionClassDisplayData() {
    SimpleFunction<?, ?> simpleFn = new SimpleFunction<Integer, Integer>() {
      @Override
      public Integer apply(Integer input) {
        return input;
      }
    };

    MapElements<?, ?> simpleMap = MapElements.via(simpleFn);
    assertThat(DisplayData.from(simpleMap), hasDisplayItem("mapFn", simpleFn.getClass()));
  }
  @Test
  public void testSimpleFunctionDisplayData() {
    SimpleFunction<Integer, ?> simpleFn = new SimpleFunction<Integer, Integer>() {
      @Override
      public Integer apply(Integer input) {
        return input;
      }

      @Override
      public void populateDisplayData(DisplayData.Builder builder) {
        builder.add(DisplayData.item("foo", "baz"));
      }
    };



    MapElements<?, ?> simpleMap = MapElements.via(simpleFn);
    assertThat(DisplayData.from(simpleMap), hasDisplayItem("mapFn", simpleFn.getClass()));
    assertThat(DisplayData.from(simpleMap), hasDisplayItem("foo", "baz"));
  }

  @Test
  @Category(ValidatesRunner.class)
  public void testPrimitiveDisplayData() {
    SimpleFunction<Integer, ?> mapFn = new SimpleFunction<Integer, Integer>() {
      @Override
      public Integer apply(Integer input) {
        return input;
      }
    };

    MapElements<Integer, ?> map = MapElements.via(mapFn);
    DisplayDataEvaluator evaluator = DisplayDataEvaluator.create();

    Set<DisplayData> displayData = evaluator.<Integer>displayDataForPrimitiveTransforms(map);
    assertThat("MapElements should include the mapFn in its primitive display data",
        displayData, hasItem(hasDisplayItem("mapFn", mapFn.getClass())));
  }

  static class VoidValues<K, V>
      extends PTransform<PCollection<KV<K, V>>, PCollection<KV<K, Void>>> {

    @Override
    public PCollection<KV<K, Void>> expand(PCollection<KV<K, V>> input) {
      return input.apply(MapElements.via(
          new SimpleFunction<KV<K, V>, KV<K, Void>>() {
            @Override
            public KV<K, Void> apply(KV<K, V> input) {
              return KV.of(input.getKey(), null);
            }
          }));
    }
  }
}
