/*
 * Copyright (C) 2013 Brett Wooldridge
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

package com.zaxxer.hikari.metrics.prometheus;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import java.util.HashMap;
import java.util.Map;

class PrometheusMetricsTracker implements IMetricsTracker {

   private static final Map<CollectorRegistry, PrometheusMetricCollectors> COLLECTORS_MAP = new HashMap<>();

   private final Counter.Child connectionTimeoutCounterChild;
   private final Summary.Child elapsedAcquiredSummaryChild;
   private final Summary.Child elapsedBorrowedSummaryChild;
   private final Summary.Child elapsedCreationSummaryChild;

   private final Collector collector;
   private final CollectorRegistry registry;

   PrometheusMetricsTracker(String poolName, Collector collector, CollectorRegistry registry) {
      this.collector = collector;
      this.registry = registry;
      PrometheusMetricCollectors metricCollectors = getPrometheusMetricCollectors(registry);

      this.connectionTimeoutCounterChild = metricCollectors.getConnectionTimeoutCounter()
         .labels(poolName);
      this.elapsedAcquiredSummaryChild = metricCollectors.getElapsedAcquiredSummary()
         .labels(poolName);
      this.elapsedBorrowedSummaryChild = metricCollectors.getElapsedBorrowedSummary()
         .labels(poolName);
      this.elapsedCreationSummaryChild = metricCollectors.getElapsedCreationSummary()
         .labels(poolName);
   }

   private PrometheusMetricCollectors getPrometheusMetricCollectors(CollectorRegistry registry) {
      return COLLECTORS_MAP
         .computeIfAbsent(registry, PrometheusMetricCollectors::new);
   }

   public void close() {
      registry.unregister(collector);
   }

   @Override
   public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos) {
      elapsedAcquiredSummaryChild.observe(elapsedAcquiredNanos);
   }

   @Override
   public void recordConnectionUsageMillis(long elapsedBorrowedMillis) {
      elapsedBorrowedSummaryChild.observe(elapsedBorrowedMillis);
   }

   @Override
   public void recordConnectionCreatedMillis(long connectionCreatedMillis) {
      elapsedCreationSummaryChild.observe(connectionCreatedMillis);
   }

   @Override
   public void recordConnectionTimeout() {
      connectionTimeoutCounterChild.inc();
   }

   private final static class PrometheusMetricCollectors {

      private final Counter connectionTimeoutCounter;
      private final Summary elapsedAcquiredSummary;
      private final Summary elapsedBorrowedSummary;
      private final Summary elapsedCreationSummary;

      PrometheusMetricCollectors(CollectorRegistry collectorRegistry) {
         connectionTimeoutCounter = Counter.build()
            .name("hikaricp_connection_timeout_count")
            .labelNames("pool")
            .help("Connection timeout count")
            .register(collectorRegistry);
         elapsedAcquiredSummary = Summary.build()
            .name("hikaricp_connection_acquired_nanos")
            .labelNames("pool")
            .help("Connection acquired time (ns)")
            .register(collectorRegistry);
         elapsedBorrowedSummary = Summary.build()
            .name("hikaricp_connection_usage_millis")
            .labelNames("pool")
            .help("Connection usage (ms)")
            .register(collectorRegistry);

         elapsedCreationSummary = Summary.build()
            .name("hikaricp_connection_creation_millis")
            .labelNames("pool")
            .help("Connection creation (ms)")
            .register(collectorRegistry);
      }

      Counter getConnectionTimeoutCounter() {
         return connectionTimeoutCounter;
      }

      Summary getElapsedAcquiredSummary() {
         return elapsedAcquiredSummary;
      }

      Summary getElapsedBorrowedSummary() {
         return elapsedBorrowedSummary;
      }

      Summary getElapsedCreationSummary() {
         return elapsedCreationSummary;
      }
   }
}
