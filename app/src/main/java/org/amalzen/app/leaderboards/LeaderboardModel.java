package org.amalzen.app.leaderboards;

import org.amalzen.app.APIs;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LeaderboardModel {
    private static final Logger LOGGER = Logger.getLogger(LeaderboardModel.class.getName());
    private static final String API_URL = APIs.USER_URL.getValue() + "/getUsers";
    private final HttpClient client;
    private final List<LeaderboardEntry> entries;

    public LeaderboardModel() {
        this.client = HttpClient.newHttpClient();
        this.entries = new ArrayList<>();
    }

    public List<LeaderboardEntry> fetchLeaderboard() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray users = new JSONArray(response.body());

            entries.clear();
            List<LeaderboardEntry> tempEntries = new ArrayList<>();

            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                tempEntries.add(new LeaderboardEntry(
                        0, // rank will be set after sorting
                        user.getString("username"),
                        user.getInt("total_score")
                ));
            }

            // Sort by score in descending order and assign ranks
            tempEntries.sort(Comparator.comparing(LeaderboardEntry::score).reversed());
            for (int i = 0; i < tempEntries.size(); i++) {
                LeaderboardEntry entry = tempEntries.get(i);
                entries.add(new LeaderboardEntry(i + 1, entry.username(), entry.score()));
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching leaderboard data", e);
        }
        return entries;
    }

    public record LeaderboardEntry(int rank, String username, int score) {
    }
}