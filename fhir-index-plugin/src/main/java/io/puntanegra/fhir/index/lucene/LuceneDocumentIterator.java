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

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.cassandra.utils.CloseableIterator;
import org.apache.cassandra.utils.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.puntanegra.fhir.index.FhirIndexException;
import io.puntanegra.fhir.index.util.TimeCounter;

/**
 * {@link Iterator} for retrieving Lucene {@link Document}s satisfying a
 * {@link Query} from an {@link IndexSearcher}.
 *
 * @author Jorge L. Middleton {@literal <jorge.middleton@gmail.com>}
 *
 */
public class LuceneDocumentIterator implements CloseableIterator<Pair<Document, ScoreDoc>> {

	private static final Logger logger = LoggerFactory.getLogger(LuceneDocumentIterator.class);

	private final SearcherManager manager;
	private final Query query;
	private final Integer page;
	private final Set<String> fields;
	private final Deque<Pair<Document, ScoreDoc>> documents = new LinkedList<>();
	private Sort sort;
	private ScoreDoc after;
	private boolean mayHaveMore = true;

	/**
	 * Builds a new iterator over the {@link Document}s satisfying the specified
	 * {@link Query}.
	 *
	 * @param manager
	 *            the index searcher manager
	 * @param query
	 *            the query to be satisfied by the documents
	 * @param sort
	 *            the sort in which the documents are going to be retrieved
	 * @param after
	 *            a pointer to the start document (not included)
	 * @param limit
	 *            the max number of documents to be retrieved
	 * @param fields
	 *            the names of the fields to be loaded
	 */
	LuceneDocumentIterator(SearcherManager manager, Query query, Sort sort, ScoreDoc after, Integer limit,
			Set<String> fields) {
		this.manager = manager;
		this.query = query;
		this.sort = sort;
		this.after = after;
		this.page = limit < Integer.MAX_VALUE ? limit + 1 : limit;
		this.fields = fields;
	}

	private void fetch() {
		try {
			IndexSearcher searcher = manager.acquire();

			try {

				TimeCounter time = TimeCounter.create().start();

				// Search for top documents
				TopDocs topDocs = null;
				if (this.sort != null) {
					sort = sort.rewrite(searcher);
					topDocs = searcher.searchAfter(after, query, page, sort);
				} else {
					topDocs = searcher.searchAfter(after, query, page);
				}
				ScoreDoc[] scoreDocs = topDocs.scoreDocs;

				// Check inf mayHaveMore
				mayHaveMore = scoreDocs.length == page;

				// Collect the documents from query result
				for (ScoreDoc scoreDoc : scoreDocs) {
					Document document = searcher.doc(scoreDoc.doc, fields);
					documents.add(Pair.create(document, scoreDoc));
					after = scoreDoc;
				}

				logger.debug("Get page with {} documents in {}", scoreDocs.length, time.stop());

			} finally {
				manager.release(searcher);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new FhirIndexException(e, "Error searching in with %s and %s", query, sort);
		}
	}

	/**
	 * Returns {@code true} if the iteration has more {@link Document}s. (In
	 * other words, returns {@code true} if {@link #next} would return an
	 * {@link Document} rather than throwing an exception.)
	 *
	 * @return {@code true} if the iteration has more {@link Document}s
	 */
	@Override
	public boolean hasNext() {
		if (needsFetch()) {
			fetch();
		}
		return !documents.isEmpty();
	}

	/**
	 * Returns if more {@link Document}s should be fetched from the Lucene
	 * index.
	 *
	 * @return {@code true} if more documents should be fetched, {@code false}
	 *         otherwise
	 */
	public boolean needsFetch() {
		return mayHaveMore && documents.isEmpty();
	}

	/**
	 * Returns the next {@link Document} in the iteration.
	 *
	 * @return the next document
	 * @throws NoSuchElementException
	 *             if the iteration has no more {@link Document}s
	 */
	@Override
	public Pair<Document, ScoreDoc> next() {
		if (hasNext()) {
			return documents.poll();
		} else {
			throw new NoSuchElementException();
		}
	}

	/** {@inheritDoc} */
	@Override
	public void close() {
	}
}
