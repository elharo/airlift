/*
 * Copyright 2010 Proofpoint, Inc.
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
package io.airlift.testing;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import io.airlift.testing.EquivalenceTester.ElementCheckFailure;

import java.util.List;

public class EquivalenceAssertionError
        extends AssertionError
{
    private final List<ElementCheckFailure> failures;

    public EquivalenceAssertionError(Iterable<ElementCheckFailure> failures)
    {
        super("Equivalence failed:\n      " + Joiner.on("\n      ").join(failures));
        this.failures = ImmutableList.copyOf(failures);
    }

    public List<ElementCheckFailure> getFailures()
    {
        return failures;
    }
}
