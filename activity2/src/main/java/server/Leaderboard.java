package server;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Leaderboard {
    private static String filename; // The filename of the JSON file to read/write
    private static List<Entry> entries; // The list of players in the leaderboard

    // Constructor that initializes the filename and loads the player data from the JSON file
    public Leaderboard( ) {
        this.filename = "leaderboard.json";
        this.entries = new ArrayList<>();
        load(); // Call the private load method to load the player data from the JSON file
    }

    // Private method that loads the player data from the JSON file
    private void load() {
        try {
            FileReader reader = new FileReader(filename);
            JSONTokener tokener = new JSONTokener(reader);
            JSONArray array = new JSONArray(tokener);
            for (int i = 0; i < array.length(); i++) {
                JSONObject playerObj = array.getJSONObject(i);
                String name = playerObj.getString("name");
                int wins = playerObj.getInt("wins");
                int logins = playerObj.getInt("logins");
                Entry entry = new Entry(name, wins, logins);
                entries.add(entry);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Public method that saves the player data to the JSON file
    public synchronized static void save() {
        JSONArray array = new JSONArray();
        for (Entry entry : entries) {
            JSONObject playerObj = new JSONObject();
            playerObj.put("name", entry.getName());
            playerObj.put("wins", entry.getWins());
            playerObj.put("logins", entry.getLogins());
            array.put(playerObj);
        }
        try {
            FileWriter writer = new FileWriter(filename);
            array.write(writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Public method that adds a new player to the leaderboard and saves the updated player data to the JSON file
    public synchronized void addPlayer( String name, int wins, int logins ) {
        Entry entry = new Entry(name, wins, logins);
        entries.add(entry);
        save(); // Call the public save method to save the updated player data to the JSON file
    }

    // Public method that returns a Player object with a specific name
    public synchronized Entry getPlayerByName( String playerName) {
        for (Entry entry : entries) {
            if (entry.getName().equals(playerName)) {
                return entry;
            }
        }
        // If a player with the given name is not found, return null
        return null;
    }

    // Public method that returns the list of players in the leaderboard
    public List<Entry> getPlayers() {
        return entries;
    }

    // Public method that returns a string representation of the leaderboard
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry entry : entries) {
            sb.append(entry.getName()).append(": ").append(entry.getWins()).append(" wins, ").append(entry.getLogins()).append(" logins\n");
        }
        return sb.toString();
    }

    public synchronized boolean playerNameExists(String playerName) {
        for (Entry entry : entries) {
            if (entry.getName().equals(playerName)) {
                return true;
            }
        }
        return false;
    }

    public synchronized List<Entry> getPlayersList() {
        return entries;
    }

    // Inner class that represents a player in the leaderboard
    public static class Entry {
        private String name; // The player's name
        private int wins; // The number of wins the player has
        private int logins; // The number of times the player has logged in

        // Constructor that initializes the player's name, number of wins, and number of logins
        public Entry( String name, int wins, int logins ) {
            this.name = name;
            this.wins = wins;
            this.logins = logins;
        }

        // Getter methods for the player's name, number of wins, and number
        public synchronized String getName() {
            return name;
        }

        public synchronized int getWins() {
            return wins;
        }

        public synchronized int getLogins() {
            return logins;
        }

        // Setter methods for the player's name, number of wins, and number
        public void setName( String name ) {
            this.name = name;
        }

        public synchronized void addWin() {
            this.wins = wins + 1;
            save();
        }

        public synchronized void addLogin() {
            this.logins = logins + 1;
            save();
        }
    }
}
