/*
 * Licensed to STRATIO (C) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  The STRATIO (C) licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.puntanegra.fhir.index.lucene;

import org.apache.lucene.search.similarities.DefaultSimilarity;

/**
 * {@link DefaultSimilarity} that ignores the inverse document frequency, doing the similarity independent of the index
 * context.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
class NoIDFSimilarity extends DefaultSimilarity {

    /**
     * Returns a constant neutral score value of {@code 1.0}.
     */
    @Override
    public float idf(long docFreq, long numDocs) {
        return 1.0f;
    }
}
