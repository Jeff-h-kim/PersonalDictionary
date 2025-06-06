package test.net;

import dict.model.Database;
import dict.model.Definition;
import dict.net.DictConnectionException;
import dict.net.DictionaryConnection;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DictionaryConnectionTest {
    @Test
    public void testBasicConnection() throws DictConnectionException {
        DictionaryConnection conn = new DictionaryConnection("dict.org");
        assertNotNull(conn);
    }

    @Test
    public void testGetDatabaseList() throws DictConnectionException {
        DictionaryConnection conn = new DictionaryConnection("dict.org");
        Map<String, Database> dbl = conn.getDatabaseList();
        assertTrue(dbl.size() > 0);
    }

    @Test
    public void testGetDefinition() throws DictConnectionException {
        DictionaryConnection conn = new DictionaryConnection("dict.org");
        Map<String, Database> dbl = conn.getDatabaseList();
        assertTrue(dbl.size() > 0);
        Database wn = dbl.get("wn");
        assertNotNull(wn);
        Collection<Definition> defs = conn.getDefinitions("parrot", wn);
        assertTrue(defs.size() > 0);
    }
}
