package test.model;

import dict.model.Database;
import dict.model.Definition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatabaseTest {

    private Database database;
    private String name;
    private String description;

    @BeforeEach
    public void initialize() {
        name = "Test";
        description = "A test to run";
        database = new Database(name, description);
    }

    @Test
    public void normalCall() {
        Database testDatabase = new Database(name, description);
        Assertions.assertTrue(testDatabase.equals(database));
    }


}
