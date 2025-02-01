package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import org.example.generated.tables.YoutubeSearchCache;
import org.jooq.JSONB;
import org.springframework.stereotype.Service;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class YoutubeService {

    private static final String APPLICATION_NAME = "YouTube Search";
    private static final String API_KEY = "AIzaSyDzguOhfsA9O89xp9AQE7qBevm74YWjC8Y";
    private static final long NUMBER_OF_VIDEOS_RETURNED = 10;

    @Autowired
    private DSLContext dsl;

    public List<SearchResult> search(String query) throws Exception {
        // Check if the query is in the cache
        Record record = dsl.selectFrom(YoutubeSearchCache.YOUTUBE_SEARCH_CACHE)
                .where(YoutubeSearchCache.YOUTUBE_SEARCH_CACHE.SEARCH_QUERY.eq(query))
                .fetchOne();

        if (record != null) {
            // Return cached results
            JSONB cachedResultsJson = record.getValue(YoutubeSearchCache.YOUTUBE_SEARCH_CACHE.SEARCH_RESULTS);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(cachedResultsJson.data(), List.class);
        }
        YouTube youtube = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                null)
                .setApplicationName(APPLICATION_NAME)
                .build();

        YouTube.Search.List search = youtube.search().list("snippet");
        search.setKey(API_KEY);
        search.setQ(query);
        search.setType("video");
        search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

        SearchListResponse searchResponse = search.execute();
        List<SearchResult> searchResults = searchResponse.getItems();

        // Serialize search results to JSON
        ObjectMapper mapper = new ObjectMapper();
        String resultsJson = mapper.writeValueAsString(searchResults);
        JSONB jsonbResults = JSONB.valueOf(resultsJson);

        // Store query and results in the database
        dsl.insertInto(YoutubeSearchCache.YOUTUBE_SEARCH_CACHE)
                .set(YoutubeSearchCache.YOUTUBE_SEARCH_CACHE.SEARCH_QUERY, query)
                .set(YoutubeSearchCache.YOUTUBE_SEARCH_CACHE.SEARCH_RESULTS, jsonbResults)
                .set(YoutubeSearchCache.YOUTUBE_SEARCH_CACHE.CREATED_AT, LocalDateTime.now())
                .execute();

        return searchResults;

    }
}

