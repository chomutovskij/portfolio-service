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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.palantir.conjure.java.api.errors.ErrorType;
import com.palantir.conjure.java.api.testing.Assertions;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BucketManagementResourceTest {
    private BucketManagementResource bucketManagementResource;

    @BeforeEach
    public void beforeEach() {
        bucketManagementResource = new BucketManagementResource();
    }

    @Test
    public void create() {
        bucketManagementResource.createBucket("BucketA");
        assertThat(bucketManagementResource.getAllBuckets())
                .isEqualTo(ImmutableMap.of("BucketA", Collections.emptyList()));
    }

    @Test
    public void createFailed() {
        bucketManagementResource.createBucket("BucketA");
        Assertions.assertThatServiceExceptionThrownBy(() -> bucketManagementResource.createBucket("BucketA"))
                .hasType(ErrorType.create(ErrorType.Code.INVALID_ARGUMENT, "Bucket:BucketCreationFailed"));
        assertThat(bucketManagementResource.getAllBuckets())
                .isEqualTo(ImmutableMap.of("BucketA", Collections.emptyList()));
        assertThat(bucketManagementResource.getPositionsInBucket("BucketA")).isEqualTo(ImmutableSet.of());
    }

    @Test
    public void deleteBucket() {
        bucketManagementResource.createBucket("BucketA");
        bucketManagementResource.deleteBucket("BucketA");
        assertThat(bucketManagementResource.getAllBuckets()).isEmpty();
    }

    @Test
    public void deleteBucketFailed() {
        assertThat(bucketManagementResource.getAllBuckets()).isEmpty();
        Assertions.assertThatServiceExceptionThrownBy(() -> bucketManagementResource.deleteBucket("BucketA"))
                .hasType(ErrorType.create(ErrorType.Code.NOT_FOUND, "Bucket:BucketNotFound"));
        assertThat(bucketManagementResource.getAllBuckets()).isEmpty();
    }

    @Test
    public void insertSymbolIntoMultipleBuckets() {
        assertThat(bucketManagementResource.getAllBuckets()).isEmpty();
        bucketManagementResource.createBucket("BucketA");
        bucketManagementResource.createBucket("BucketZ");
        bucketManagementResource.insertSymbolIntoBuckets("NVDA", ImmutableSet.of("BucketB", "BucketA", "BucketC"));

        assertThat(bucketManagementResource.getAllBuckets())
                .isEqualTo(ImmutableMap.of(
                        "BucketA", ImmutableList.of("NVDA"),
                        "BucketB", ImmutableList.of("NVDA"),
                        "BucketC", ImmutableList.of("NVDA"),
                        "BucketZ", ImmutableList.of()));

        assertThat(bucketManagementResource.getPositionsInBucket("BucketA")).isEqualTo(ImmutableSet.of("NVDA"));
        assertThat(bucketManagementResource.getPositionsInBucket("BucketB")).isEqualTo(ImmutableSet.of("NVDA"));
        assertThat(bucketManagementResource.getPositionsInBucket("BucketC")).isEqualTo(ImmutableSet.of("NVDA"));
        assertThat(bucketManagementResource.getPositionsInBucket("BucketZ")).isEmpty();
        assertThat(bucketManagementResource.getBucketsForSymbol("NVDA"))
                .isEqualTo(ImmutableList.of("BucketA", "BucketB", "BucketC"));

        bucketManagementResource.insertSymbolIntoBuckets("AMZN", ImmutableSet.of("BucketB", "BucketC"));
        bucketManagementResource.insertSymbolIntoBuckets("TSLA", ImmutableSet.of("BucketB", "BucketC"));

        assertThat(bucketManagementResource.getAllBuckets())
                .isEqualTo(ImmutableMap.of(
                        "BucketA", ImmutableList.of("NVDA"),
                        "BucketB", ImmutableList.of("AMZN", "NVDA", "TSLA"),
                        "BucketC", ImmutableList.of("AMZN", "NVDA", "TSLA"),
                        "BucketZ", ImmutableList.of()));

        assertThat(bucketManagementResource.getPositionsInBucket("BucketA")).isEqualTo(ImmutableSet.of("NVDA"));
        assertThat(bucketManagementResource.getPositionsInBucket("BucketB"))
                .isEqualTo(ImmutableSet.of("NVDA", "AMZN", "TSLA"));
        assertThat(bucketManagementResource.getPositionsInBucket("BucketC"))
                .isEqualTo(ImmutableSet.of("NVDA", "AMZN", "TSLA"));
        assertThat(bucketManagementResource.getPositionsInBucket("BucketZ")).isEmpty();
        assertThat(bucketManagementResource.getBucketsForSymbol("AMZN"))
                .isEqualTo(ImmutableList.of("BucketB", "BucketC"));
        assertThat(bucketManagementResource.getBucketsForSymbol("NVDA"))
                .isEqualTo(ImmutableList.of("BucketA", "BucketB", "BucketC"));
        assertThat(bucketManagementResource.getBucketsForSymbol("TSLA"))
                .isEqualTo(ImmutableList.of("BucketB", "BucketC"));
    }
}
