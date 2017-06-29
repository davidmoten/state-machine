package com.github.davidmoten.fsm.persistence;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

import org.junit.Test;

public class PersistenceH2Test {

    @Test
    public void test() throws IOException {
        File directory = File.createTempFile("db-", "", new File("target"));
        directory.mkdir();
        PersistenceH2 p = new PersistenceH2(directory, Executors.newScheduledThreadPool(5), null,
                null, null);
        p.create();
    }

}
