package com.github.davidmoten.fsm.persistence;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class PersistenceH2Test {

    @Test
    public void test() throws IOException {
        File directory = File.createTempFile("db-", "", new File("target"));
        directory.mkdir();
        PersistenceH2<String> p = new PersistenceH2<String>(directory);
        p.create();
    }

}
