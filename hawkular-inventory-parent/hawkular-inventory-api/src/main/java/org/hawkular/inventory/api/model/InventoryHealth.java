/*
 * Copyright 2014-2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.inventory.api.model;

import static com.fasterxml.jackson.annotation.JsonInclude.*;

import java.lang.management.MemoryUsage;

import org.hawkular.commons.doc.DocModel;
import org.hawkular.commons.doc.DocModelProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@DocModel(description = "Representation of metrics related to the inventory usage. + \n" +
        "It collects data from memory, disk, cpu, and inventory usage.")
public class InventoryHealth {

    public static class Builder {
        private long timestamp;
        private MemoryStats heapMemoryStatsUsage;
        private MemoryStats nonHeapMemoryStatsUsage;
        private double systemLoadAverage;
        private long gcCollectionCount;
        private long gcCollectionTime;
        private long numberOfResources;
        private long numberOfResourcesInMemory;
        private long averageReadTimeForResources;
        private long averageWriteTimeForResources;
        private long numberOfResourceTypes;
        private long numberOfResourceTypesInMemory;
        private long averageReadTimeForResourceTypes;
        private long averageWriteTimeForResourceTypes;
        private long inventoryTotalSpace;
        private long inventoryFreeSpace;

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder heapMemoryUsage(MemoryUsage heap) {
            this.heapMemoryStatsUsage = new MemoryStats(heap.getInit(), heap.getUsed(), heap.getCommitted(), heap.getMax());
            return this;
        }

        public Builder nonHeapMemoryUsage(MemoryUsage nonHeap) {
            this.nonHeapMemoryStatsUsage = new MemoryStats(nonHeap.getInit(), nonHeap.getUsed(), nonHeap.getCommitted(), nonHeap.getMax());
            return this;
        }

        public Builder systemLoadAverage(double systemLoadAverage) {
            this.systemLoadAverage = systemLoadAverage;
            return this;
        }

        public Builder gcCollectionCount(long gcCollectionCount) {
            this.gcCollectionCount = gcCollectionCount;
            return this;
        }

        public Builder gcCollectionTime(long gcCollectionTime) {
            this.gcCollectionTime = gcCollectionTime;
            return this;
        }

        public Builder numberOfResources(long numberOfResources) {
            this.numberOfResources = numberOfResources;
            return this;
        }

        public Builder numberOfResourcesInMemory(long numberOfResourcesInMemory) {
            this.numberOfResourcesInMemory = numberOfResourcesInMemory;
            return this;
        }

        public Builder averageReadTimeForResources(long averageReadTimeForResources) {
            this.averageReadTimeForResources = averageReadTimeForResources;
            return this;
        }

        public Builder averageWriteTimeForResources(long averageWriteTimeForResources) {
            this.averageWriteTimeForResources = averageWriteTimeForResources;
            return this;
        }

        public Builder numberOfResourceTypes(long numberOfResourceTypes) {
            this.numberOfResourceTypes = numberOfResourceTypes;
            return this;
        }

        public Builder numberOfResourceTypesInMemory(long numberOfResourceTypesInMemory) {
            this.numberOfResourceTypesInMemory = numberOfResourceTypesInMemory;
            return this;
        }

        public Builder averageReadTimeForResourceTypes(long averageReadTimeForResourceTypes) {
            this.averageReadTimeForResourceTypes = averageReadTimeForResourceTypes;
            return this;
        }

        public Builder averageWriteTimeForResourceTypes(long averageWriteTimeForResourceTypes) {
            this.averageWriteTimeForResourceTypes = averageWriteTimeForResourceTypes;
            return this;
        }


        public Builder inventoryTotalSpace(long inventoryTotalSpace) {
            this.inventoryTotalSpace = inventoryTotalSpace;
            return this;
        }

        public Builder inventoryFreeSpace(long inventoryFreeSpace) {
            this.inventoryFreeSpace = inventoryFreeSpace;
            return this;
        }

        public InventoryHealth build() {
            return new InventoryHealth(timestamp,
                    heapMemoryStatsUsage,
                    nonHeapMemoryStatsUsage,
                    new CpuStats(systemLoadAverage, gcCollectionCount, gcCollectionTime),
                    new DiskStats(inventoryTotalSpace, inventoryFreeSpace),
                    new InventoryStats(numberOfResources,
                            numberOfResourcesInMemory,
                            averageReadTimeForResources,
                            averageWriteTimeForResources,
                            numberOfResourceTypes,
                            numberOfResourceTypesInMemory,
                            averageReadTimeForResourceTypes,
                            averageWriteTimeForResourceTypes));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @DocModelProperty(description = "Timestamp for this sample (milliseconds since epoch).")
    @JsonInclude(Include.NON_NULL)
    private final long timestamp;

    @DocModelProperty(description = "Java heap memory statistics.")
    @JsonInclude(Include.NON_NULL)
    private final MemoryStats heapMemoryStatsUsage;

    @DocModelProperty(description = "Java non heap memory statistics.")
    @JsonInclude(Include.NON_NULL)
    private final MemoryStats nonHeapMemoryStatsUsage;

    @DocModelProperty(description = "CPU statistics.")
    @JsonInclude(Include.NON_NULL)
    private final CpuStats cpuStats;

    @DocModelProperty(description = "Disk statistics.")
    @JsonInclude(Include.NON_NULL)
    private final DiskStats diskStats;

    @DocModelProperty(description = "Inventory statistics.")
    @JsonInclude(Include.NON_NULL)
    private final InventoryStats inventoryStats;

    public InventoryHealth(@JsonProperty("timestamp") long timestamp,
                           @JsonProperty("heapMemoryStatsUsage") MemoryStats heapMemoryStatsUsage,
                           @JsonProperty("nonHeapMemoryStatsUsage") MemoryStats nonHeapMemoryStatsUsage,
                           @JsonProperty("cpuStats") CpuStats cpuStats,
                           @JsonProperty("diskStats") DiskStats diskStats,
                           @JsonProperty("inventoryStats") InventoryStats inventoryStats) {
        this.timestamp = timestamp;
        this.heapMemoryStatsUsage = heapMemoryStatsUsage;
        this.nonHeapMemoryStatsUsage = nonHeapMemoryStatsUsage;
        this.cpuStats = cpuStats;
        this.diskStats = diskStats;
        this.inventoryStats = inventoryStats;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public MemoryStats getHeapMemoryStatsUsage() {
        return heapMemoryStatsUsage;
    }

    public MemoryStats getNonHeapMemoryStatsUsage() {
        return nonHeapMemoryStatsUsage;
    }

    public CpuStats getCpuStats() {
        return cpuStats;
    }

    public DiskStats getDiskStats() {
        return diskStats;
    }

    public InventoryStats getInventoryStats() {
        return inventoryStats;
    }

    @DocModel(description = "Representation of a snapshot of memory usage.")
    public static class MemoryStats {

        @DocModelProperty(description = "Initial memory (in bytes).")
        @JsonInclude(Include.NON_NULL)
        private final long init;

        @DocModelProperty(description = "Memory currently used (in bytes).")
        @JsonInclude(Include.NON_NULL)
        private final long used;

        @DocModelProperty(description = "Memory guaranteed to be available (in bytes).")
        @JsonInclude(Include.NON_NULL)
        private final long committed;

        @DocModelProperty(description = "Max amount of memory (in bytes).")
        @JsonInclude(Include.NON_NULL)
        private final long max;

        public MemoryStats(@JsonProperty("init") long init,
                           @JsonProperty("used") long used,
                           @JsonProperty("committed") long committed,
                           @JsonProperty("max") long max) {
            this.init = init;
            this.used = used;
            this.committed = committed;
            this.max = max;
        }

        public long getInit() {
            return init;
        }

        public long getUsed() {
            return used;
        }

        public long getCommitted() {
            return committed;
        }

        public long getMax() {
            return max;
        }
    }

    @DocModel(description = "Representation of a snapshot of cpu usage.")
    public static class CpuStats {

        @DocModelProperty(description = "System load average for the last minute.")
        @JsonInclude(Include.NON_NULL)
        private final double systemLoadAverage;

        @DocModelProperty(description = "Total number of GC collections that have occurred.")
        @JsonInclude(Include.NON_NULL)
        private final long gcCollectionCount;

        @DocModelProperty(description = "Approximate accumulated GC collection elapsed time.")
        @JsonInclude(Include.NON_NULL)
        private final long gcCollectionTime;

        public CpuStats(@JsonProperty("systemLoadAverage") double systemLoadAverage,
                        @JsonProperty("gcCollectionCount") long gcCollectionCount,
                        @JsonProperty("gcCollectionTime") long gcCollectionTime) {
            this.systemLoadAverage = systemLoadAverage;
            this.gcCollectionCount = gcCollectionCount;
            this.gcCollectionTime = gcCollectionTime;
        }

        public double getSystemLoadAverage() {
            return systemLoadAverage;
        }

        public long getGcCollectionCount() {
            return gcCollectionCount;
        }

        public long getGcCollectionTime() {
            return gcCollectionTime;
        }
    }

    @DocModel(description = "Representation of a snapshot of disk usage.")
    public static class DiskStats {

        @DocModelProperty(description = "Total size of inventory data files (in bytes).")
        @JsonInclude(Include.NON_NULL)
        private long inventoryTotalSpace;

        @DocModelProperty(description = "Free space of filesystem where inventory data files are stored (in bytes).")
        @JsonInclude(Include.NON_NULL)
        private long inventoryFreeSpace;

        public DiskStats(@JsonProperty("inventoryTotalSpace") long inventoryTotalSpace,
                         @JsonProperty("inventoryFreeSpace") long inventoryFreeSpace) {
            this.inventoryTotalSpace = inventoryTotalSpace;
            this.inventoryFreeSpace = inventoryFreeSpace;
        }

        public long getInventoryTotalSpace() {
            return inventoryTotalSpace;
        }

        public long getInventoryFreeSpace() {
            return inventoryFreeSpace;
        }
    }

    @DocModel(description = "Representation of a snapshot of inventory usage.")
    public static class InventoryStats {

        @DocModelProperty(description = "Number of resources stored in the inventory.")
        @JsonInclude(Include.NON_NULL)
        private final long numberOfResources;

        @DocModelProperty(description = "Number of resources loaded in memory.")
        @JsonInclude(Include.NON_NULL)
        private final long numberOfResourcesInMemory;

        @DocModelProperty(description = "Average number of nanoseconds for a resource read from inventory.")
        @JsonInclude(Include.NON_NULL)
        private final long averageReadTimeForResources;

        @DocModelProperty(description = "Average number of nanoseconds for a resource write from inventory.")
        @JsonInclude(Include.NON_NULL)
        private final long averageWriteTimeForResources;

        @DocModelProperty(description = "Number of resource types stored in the inventory.")
        @JsonInclude(Include.NON_NULL)
        private final long numberOfResourceTypes;

        @DocModelProperty(description = "Number of resource types loaded in memory.")
        @JsonInclude(Include.NON_NULL)
        private final long numberOfResourceTypeInMemory;

        @DocModelProperty(description = "Average number of nanoseconds for a resource type read from inventory.")
        @JsonInclude(Include.NON_NULL)
        private final long averageReadTimeForResourceTypes;

        @DocModelProperty(description = "Average number of nanoseconds for a resource write from inventory.")
        @JsonInclude(Include.NON_NULL)
        private final long averageWriteTimeForResourceTypes;

        public InventoryStats(@JsonProperty("numberOfResources") long numberOfResources,
                              @JsonProperty("numberOfResourcesInMemory") long numberOfResourcesInMemory,
                              @JsonProperty("averageReadTimeForResources") long averageReadTimeForResources,
                              @JsonProperty("averageWriteTimeForResources") long averageWriteTimeForResources,
                              @JsonProperty("numberOfResourceTypes") long numberOfResourceTypes,
                              @JsonProperty("numberOfResourceTypeInMemory") long numberOfResourceTypeInMemory,
                              @JsonProperty("averageReadTimeForResourceTypes") long averageReadTimeForResourceTypes,
                              @JsonProperty("averageWriteTimeForResourceTypes") long averageWriteTimeForResourceTypes) {
            this.numberOfResources = numberOfResources;
            this.numberOfResourcesInMemory = numberOfResourcesInMemory;
            this.averageReadTimeForResources = averageReadTimeForResources;
            this.averageWriteTimeForResources = averageWriteTimeForResources;
            this.numberOfResourceTypes = numberOfResourceTypes;
            this.numberOfResourceTypeInMemory = numberOfResourceTypeInMemory;
            this.averageReadTimeForResourceTypes = averageReadTimeForResourceTypes;
            this.averageWriteTimeForResourceTypes = averageWriteTimeForResourceTypes;
        }

        public long getNumberOfResources() {
            return numberOfResources;
        }

        public long getNumberOfResourcesInMemory() {
            return numberOfResourcesInMemory;
        }

        public long getAverageReadTimeForResources() {
            return averageReadTimeForResources;
        }

        public long getAverageWriteTimeForResources() {
            return averageWriteTimeForResources;
        }

        public long getNumberOfResourceTypes() {
            return numberOfResourceTypes;
        }

        public long getNumberOfResourceTypeInMemory() {
            return numberOfResourceTypeInMemory;
        }

        public long getAverageReadTimeForResourceTypes() {
            return averageReadTimeForResourceTypes;
        }

        public long getAverageWriteTimeForResourceTypes() {
            return averageWriteTimeForResourceTypes;
        }
    }
}
