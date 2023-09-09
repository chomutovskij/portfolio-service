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

package com.achomutovskij.portfolioservice.util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class OffsetDateTimeUtils {
    private OffsetDateTimeUtils() {}

    public static OffsetDateTime getValidTradingDateStartOfDayInUtc() {
        return LocalDate.now(ZoneOffset.UTC)
                .minusDays(3)
                .atStartOfDay(ZoneOffset.UTC)
                .toOffsetDateTime();
    }

    public static OffsetDateTime utcStartOfDay(OffsetDateTime dateTime) {
        return dateTime.withOffsetSameInstant(ZoneOffset.UTC)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }
}
