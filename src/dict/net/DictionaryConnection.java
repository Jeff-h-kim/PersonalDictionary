package dict.net;

import dict.model.Database;
import dict.model.Definition;
import dict.model.MatchingStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Handles connections to DICT protocol servers for dictionary operations.
 * This class provides methods to connect to dictionary servers and perform
 * various operations like getting definitions, matches, and database information.
 */
public class DictionaryConnection implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(DictionaryConnection.class.getName());
    private static final int DEFAULT_PORT = 2628;
    private static final String TERMINATOR = ".";

    // Connection components
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private boolean isConnected;

    /**
     * Creates a new dictionary connection to the specified host and port.
     *
     * @param host the dictionary server hostname
     * @param port the port number to connect to
     * @throws DictConnectionException if connection fails
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {
        try {
            this.socket = new Socket(host, port);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.isConnected = true;

            LOGGER.info("Successfully connected to " + host + ":" + port);

            // Verify connection status
            Status status = Status.readStatus(in);
            if (status.getStatusCode() != 220) {
                throw new DictConnectionException("Server rejected connection. Status: " + status.getStatusCode());
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to establish connection to " + host + ":" + port, e);
            throw new DictConnectionException("Unable to connect to dictionary server", e);
        }
    }

    /**
     * Creates a new dictionary connection using the default port.
     *
     * @param host the dictionary server hostname
     * @throws DictConnectionException if connection fails
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /**
     * Closes the connection to the dictionary server.
     */
    @Override
    public synchronized void close() {
        if (!isConnected) {
            return;
        }

        try {
            sendCommand("QUIT");
            socket.close();
            isConnected = false;
            LOGGER.info("Connection closed successfully");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing connection", e);
        }
    }

    /**
     * Retrieves definitions for a word from the specified database.
     *
     * @param word the word to define
     * @param database the database to search in
     * @return collection of definitions found
     * @throws DictConnectionException if operation fails
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database)
            throws DictConnectionException {

        validateConnection();
        Collection<Definition> definitions = new ArrayList<>();

        String command = String.format("DEFINE %s %s", database.getName(), word);
        sendCommand(command);

        Status status = Status.readStatus(in);

        switch (status.getStatusCode()) {
            case 550:
                LOGGER.warning("Invalid database: " + database.getName());
                return definitions;

            case 552:
                LOGGER.info("No definitions found for word: " + word);
                return definitions;

            case 150:
                definitions = parseDefinitions(word, status.getDetails());
                validateEndStatus(250);
                LOGGER.info("Successfully retrieved definitions for: " + word);
                break;

            default:
                throw new DictConnectionException("Unexpected status code: " + status.getStatusCode());
        }

        return definitions;
    }

    /**
     * Gets a list of words matching the specified criteria.
     *
     * @param word the word pattern to match
     * @param strategy the matching strategy to use
     * @param database the database to search in
     * @return set of matching words
     * @throws DictConnectionException if operation fails
     */
    public synchronized Set<String> getMatchList(String word, MatchingStrategy strategy, Database database)
            throws DictConnectionException {

        validateConnection();
        Set<String> matches = new LinkedHashSet<>();

        String command = String.format("MATCH %s %s %s", database.getName(), strategy.getName(), word);
        sendCommand(command);

        Status status = Status.readStatus(in);

        switch (status.getStatusCode()) {
            case 550:
                LOGGER.warning("Invalid database: " + database.getName());
                return matches;

            case 551:
                LOGGER.warning("Invalid strategy: " + strategy.getName());
                return matches;

            case 552:
                LOGGER.info("No matches found for: " + word);
                return matches;

            case 152:
                matches = parseMatches();
                validateEndStatus(250);
                LOGGER.info("Successfully retrieved matches for: " + word);
                break;

            default:
                throw new DictConnectionException("Unexpected status code: " + status.getStatusCode());
        }

        return matches;
    }

    /**
     * Retrieves the list of available databases.
     *
     * @return map of database names to Database objects
     * @throws DictConnectionException if operation fails
     */
    public synchronized Map<String, Database> getDatabaseList() throws DictConnectionException {
        validateConnection();
        Map<String, Database> databases = new LinkedHashMap<>();

        sendCommand("SHOW DB");
        Status status = Status.readStatus(in);

        switch (status.getStatusCode()) {
            case 554:
                LOGGER.info("No databases available");
                return databases;

            case 110:
                databases = parseDatabases();
                validateEndStatus(250);
                LOGGER.info("Successfully retrieved database list");
                break;

            default:
                throw new DictConnectionException("Unexpected status code: " + status.getStatusCode());
        }

        return databases;
    }

    /**
     * Retrieves the list of available matching strategies.
     *
     * @return set of available matching strategies
     * @throws DictConnectionException if operation fails
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        validateConnection();
        Set<MatchingStrategy> strategies = new LinkedHashSet<>();

        sendCommand("SHOW STRAT");
        Status status = Status.readStatus(in);

        switch (status.getStatusCode()) {
            case 555:
                LOGGER.info("No strategies available");
                return strategies;

            case 111:
                strategies = parseStrategies();
                validateEndStatus(250);
                LOGGER.info("Successfully retrieved strategy list");
                break;

            default:
                throw new DictConnectionException("Unexpected status code: " + status.getStatusCode());
        }

        return strategies;
    }

    /**
     * Gets information about a specific database.
     *
     * @param database the database to get information about
     * @return database information as a string
     * @throws DictConnectionException if operation fails
     */
    public synchronized String getDatabaseInfo(Database database) throws DictConnectionException {
        validateConnection();

        // Handle special database names
        String dbName = database.getName();
        if ("*".equals(dbName)) {
            return "Searching in all databases";
        } else if ("!".equals(dbName)) {
            return "Searching in first database with match";
        }

        String command = "SHOW INFO " + dbName;
        sendCommand(command);

        Status status = Status.readStatus(in);

        switch (status.getStatusCode()) {
            case 550:
                LOGGER.warning("Invalid database: " + dbName);
                throw new DictConnectionException("Invalid database: " + dbName);

            case 112:
                String info = parseDatabaseInfo();
                validateEndStatus(250);
                LOGGER.info("Successfully retrieved info for database: " + dbName);
                return info;

            default:
                throw new DictConnectionException("Unexpected status code: " + status.getStatusCode());
        }
    }

    // Private helper methods

    private void validateConnection() throws DictConnectionException {
        if (!isConnected || socket.isClosed()) {
            throw new DictConnectionException("Connection is not active");
        }
    }

    private void sendCommand(String command) {
        out.println(command);
        LOGGER.fine("Sent command: " + command);
    }

    private void validateEndStatus(int expectedCode) throws DictConnectionException {
        Status endStatus = Status.readStatus(in);
        if (endStatus.getStatusCode() != expectedCode) {
            throw new DictConnectionException("Expected status " + expectedCode +
                    " but got " + endStatus.getStatusCode());
        }
    }

    private Collection<Definition> parseDefinitions(String word, String details) throws DictConnectionException {
        Collection<Definition> definitions = new ArrayList<>();
        String[] detailsArray = details.split(" ");
        int numberOfDefinitions = Integer.parseInt(detailsArray[0]);

        for (int i = 0; i < numberOfDefinitions; i++) {
            Definition definition = parseDefinition(word);
            definitions.add(definition);
        }

        return definitions;
    }

    private Definition parseDefinition(String word) throws DictConnectionException {
        try {
            Status defStatus = Status.readStatus(in);
            String[] statusArray = defStatus.getDetails().split(" ", 3);
            String databaseName = statusArray[1];

            Definition definition = new Definition(word, databaseName);
            StringBuilder definitionText = new StringBuilder();

            String line = in.readLine();
            while (!TERMINATOR.equals(line)) {
                if (line.isEmpty()) {
                    definitionText.append("\n");
                } else if (!line.trim().isEmpty()) {
                    definitionText.append(line).append("\n");
                }
                line = in.readLine();
            }

            definition.setDefinition(definitionText.toString().trim());
            return definition;

        } catch (IOException e) {
            throw new DictConnectionException("Error reading definition", e);
        }
    }

    private Set<String> parseMatches() throws DictConnectionException {
        Set<String> matches = new LinkedHashSet<>();

        try {
            String line = in.readLine();
            while (!TERMINATOR.equals(line)) {
                String cleanLine = line.trim().replaceAll("^\"|\"$", "");
                String[] parts = cleanLine.split(" ", 2);
                if (parts.length > 1) {
                    matches.add(parts[1].replace("\"", "").trim());
                }
                line = in.readLine();
            }
        } catch (IOException e) {
            throw new DictConnectionException("Error reading matches", e);
        }

        return matches;
    }

    private Map<String, Database> parseDatabases() throws DictConnectionException {
        Map<String, Database> databases = new LinkedHashMap<>();

        try {
            String line = in.readLine().trim();
            while (!TERMINATOR.equals(line)) {
                String[] parts = line.split("\"", 2);
                if (parts.length >= 2) {
                    String dbName = parts[0].trim();
                    String dbDescription = parts[1].trim();
                    if (dbDescription.endsWith("\"")) {
                        dbDescription = dbDescription.substring(0, dbDescription.length() - 1);
                    }
                    databases.put(dbName, new Database(dbName, dbDescription));
                }
                line = in.readLine().trim();
            }
        } catch (IOException e) {
            throw new DictConnectionException("Error reading database list", e);
        }

        return databases;
    }

    private Set<MatchingStrategy> parseStrategies() throws DictConnectionException {
        Set<MatchingStrategy> strategies = new LinkedHashSet<>();

        try {
            String line = in.readLine();
            while (!TERMINATOR.equals(line)) {
                String[] parts = line.split(" ", 2);
                if (parts.length >= 2) {
                    String strategyName = parts[0].replaceAll("^\"|\"$", "");
                    String strategyDescription = parts[1].replaceAll("^\"|\"$", "");
                    strategies.add(new MatchingStrategy(strategyName, strategyDescription));
                }
                line = in.readLine();
            }
        } catch (IOException e) {
            throw new DictConnectionException("Error reading strategy list", e);
        }

        return strategies;
    }

    private String parseDatabaseInfo() throws DictConnectionException {
        StringBuilder info = new StringBuilder();

        try {
            // Skip the first line (header)
            in.readLine();

            String line = in.readLine();
            while (!TERMINATOR.equals(line)) {
                info.append(line).append("\n");
                line = in.readLine();
            }
        } catch (IOException e) {
            throw new DictConnectionException("Error reading database info", e);
        }

        return info.toString().trim();
    }
}