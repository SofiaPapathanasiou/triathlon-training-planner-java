package com.tribuddy.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tribuddy.model.*;
import com.tribuddy.service.FatigueReport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public class AICoach {
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public AICoach(String apiKey){
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    //Returns Ai coaching feedback on this week's plan
    //returns string, but doesn't throw to not crash the app
    public String getWeeklyFeedback(TrainingPlan plan, FatigueReport report){
        if (apiKey == null || apiKey.isBlank()){
            return "AI feedback not available: no API key is configured";
        }
        try{
            String prompt = buildFeedbackPrompt(plan, report);
            return callApi(prompt);
        }catch(Exception e){
            return "AI feedback unavailable: " + e.getMessage();
        }
    }
    // recommends next week's focus based on recent load to prevent injury
    //returns String: ENDURANCE, STRENGTH, or RECOVERY
    public String recommendNextWeekFocus(FatigueReport report, int weeksOfHighLoad){
        if (apiKey == null || apiKey.isBlank()){
            return "AI feedback not available: no API key is configured";
        }
        try{
            String prompt = buildFocusPrompt(report, weeksOfHighLoad);
            return callApi(prompt);
        }catch(Exception e){
            return "AI feedback unavailable: " + e.getMessage();
        }
    }

    //prompt builders
    String buildFeedbackPrompt(TrainingPlan plan, FatigueReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are training assistant for triathletes. ");
        sb.append("Review this athlete's weekly training plan and give 2-3 sentences of specific, practical feedback.\n\n");
        sb.append("WEEKLY SCHEDULE:\n");
        for (Map.Entry<DayOfWeek, List<Workout>> entry : plan.getSchedule().entrySet()) {
            sb.append(entry.getKey()).append(": ");
            if (entry.getValue().isEmpty()) {
                sb.append("Rest");
            } else {
                for (Workout w : entry.getValue()) {
                    sb.append(w.getType()).append(" ")
                            .append(w.getDurationMinutes()).append("min @")
                            .append(w.getZone()).append("  ");
                }
            }
            sb.append("\n");
        }
        sb.append("\nFATIGUE REPORT:\n");
        sb.append("Weekly load: ").append(report.getWeeklyLoad()).append("\n");
        sb.append("Previous week load: ").append(report.getPrevWeekLoad()).append("\n");
        sb.append(String.format("Change: %+.1f%%\n", report.getChangePercent()));
        sb.append("Status: ").append(report.getStatus()).append("\n");
        sb.append("Completed workouts: ").append(plan.getCompletedCount()).append("\n");
        sb.append("Skipped workouts: ").append(plan.getSkippedCount()).append("\n");
        sb.append("\nProvide focused coaching feedback:");
        return sb.toString();
    }
    String buildFocusPrompt(FatigueReport report, int weeksOfHighLoad) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are training assistant for triathletes. ");
        sb.append("Based on this athlete's fatigue data, recommend next week's training focus.\n\n");
        sb.append("Current fatigue status: ").append(report.getStatus()).append("\n");
        sb.append(String.format("Load change this week: %+.1f%%\n", report.getChangePercent()));
        sb.append("Consecutive weeks of high load: ").append(weeksOfHighLoad).append("\n");
        sb.append("De-load recommended: ").append(report.isDeLoad()).append("\n\n");
        sb.append("Reply with ONLY one word (either ENDURANCE, STRENGTH, or RECOVERY) based on what this athlete needs most next week.");
        return sb.toString();
    }

    private String callApi(String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", MODEL,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return "[API error: status " + response.statusCode() + "]";
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("choices").get(0).path("message").path("content").asText("[No response]");
    }


}
