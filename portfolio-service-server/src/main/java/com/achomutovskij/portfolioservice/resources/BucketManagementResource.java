/*
 * (c) Copyright 2023 Andrej Chomutovskij. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.achomutovskij.portfolioservice.resources;

import com.achomutovskij.portfolioservice.api.BucketErrors;
import com.achomutovskij.portfolioservice.api.UndertowBucketManagementService;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class BucketManagementResource implements UndertowBucketManagementService {

    private static final String BUCKET_ALREADY_EXISTS = "Bucket with the given name already exists";

    private final Map<String, Set<String>> bucketNameToSymbols;
    private final Map<String, Set<String>> symbolToBuckets;

    public BucketManagementResource() {
        this.bucketNameToSymbols = new ConcurrentHashMap<>();
        this.symbolToBuckets = new ConcurrentHashMap<>();
    }

    @Override
    public void createBucket(String bucketName) {
        if (bucketNameToSymbols.containsKey(bucketName)) {
            throw BucketErrors.bucketCreationFailed(bucketName, BUCKET_ALREADY_EXISTS);
        }
        bucketNameToSymbols.put(bucketName, new HashSet<>());
    }

    @Override
    public void deleteBucket(String bucketName) {
        if (!bucketNameToSymbols.containsKey(bucketName)) {
            throw BucketErrors.bucketNotFound(bucketName);
        }
        bucketNameToSymbols.remove(bucketName);
        symbolToBuckets.forEach((_symbol, buckets) -> buckets.remove(bucketName));
    }

    @Override
    public Map<String, List<String>> getAllBuckets() {
        return bucketNameToSymbols.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().sorted().collect(Collectors.toList()),
                        (first, _second) -> first,
                        TreeMap::new));
    }

    public void insertSymbolIntoBuckets(String symbol, Set<String> buckets) {
        buckets.forEach(bucket -> {
            bucketNameToSymbols.computeIfAbsent(bucket, _key -> new HashSet<>()).add(symbol);
            symbolToBuckets.computeIfAbsent(symbol, _key -> new HashSet<>()).add(bucket);
        });
    }

    public void removeSymbolFromAllBuckets(String symbol) {
        // create a copy to avoid a concurrent modification exception in the for each loop below
        Set<String> buckets = ImmutableSet.copyOf(symbolToBuckets.getOrDefault(symbol, Collections.emptySet()));

        buckets.forEach(bucket -> removeSymbolFromBucket(bucket, symbol));
        symbolToBuckets.remove(symbol);
    }

    public void removeSymbolFromBucket(String bucketName, String symbol) {
        if (bucketNameToSymbols.containsKey(bucketName)) {
            bucketNameToSymbols.get(bucketName).remove(symbol);
        }

        if (symbolToBuckets.containsKey(symbol)) {
            symbolToBuckets.get(symbol).remove(bucketName);
        }
    }

    public Set<String> getPositionsInBucket(String bucket) {
        if (!bucketNameToSymbols.containsKey(bucket)) {
            throw BucketErrors.bucketNotFound(bucket);
        }
        return bucketNameToSymbols.get(bucket);
    }

    public List<String> getBucketsForSymbol(String symbol) {
        return symbolToBuckets.getOrDefault(symbol, Collections.emptySet()).stream()
                .sorted()
                .collect(Collectors.toList());
    }
}
