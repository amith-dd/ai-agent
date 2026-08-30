package com.example.demo.chat;

import dev.langchain4j.agent.tool.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class WebSearchTool {

    @Tool("Search the internet for a given query to gather up-to-date information")
    public String searchInternet(String query) {
        try {
            String url = "https://html.duckduckgo.com/html/?q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8.toString());

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .get();

            Elements snippets = doc.select(".result__snippet");
            StringBuilder results = new StringBuilder();
            int count = 0;
            for (Element snippet : snippets) {
                if (count >= 3) break;
                results.append("- ").append(snippet.text()).append("\n");
                count++;
            }

            if (results.isEmpty()) {
                return "No information found on the internet for: " + query;
            }
            return results.toString();
        } catch (Exception e) {
            return "Failed to search the internet: " + e.getMessage();
        }
    }
}
