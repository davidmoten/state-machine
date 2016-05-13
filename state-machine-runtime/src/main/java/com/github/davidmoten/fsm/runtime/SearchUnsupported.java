package com.github.davidmoten.fsm.runtime;

public class SearchUnsupported<Id> implements Search<Id> {

    private static final SearchUnsupported<Object> INSTANCE = new SearchUnsupported<>();

    @SuppressWarnings("unchecked")
    public static <Id> SearchUnsupported<Id> instance() {
        return (SearchUnsupported<Id>) INSTANCE;
    }

    @Override
    public <T> T search(Class<T> cls, Id id) {
        throw new UnsupportedOperationException("search is not supported");
    }

}
