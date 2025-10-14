package com.infomoney.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infomoney.model.Article;
import com.infomoney.repository.ArticleRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class NewsScraperService {

    private static final String API_URL =
            "https://www.infomoney.com.br/wp-json/wp/v2/posts?categories=24&page=";
    private static final int MAX_PAGES = 3;

    @Autowired
    private ArticleRepository articleRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Article> scrapeNews() {
        List<Article> articles = new ArrayList<>();

        for (int i = 1; i <= MAX_PAGES; i++) {
            String url = API_URL + i;
            System.out.println("🔎 Buscando notícias da página: " + url);

            try {
                String jsonResponse = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(jsonResponse);

                for (JsonNode post : root) {
                    Article article = new Article();

                    // Dados básicos
                    article.setTitle(post.get("title").get("rendered").asText());
                    article.setSubtitle(post.get("excerpt").get("rendered").asText()
                            .replaceAll("<[^>]*>", "").trim());
                    article.setUrl(post.get("link").asText());

                    // Data
                    String dateString = post.get("date").asText(); // Ex: 2025-10-14T13:45:00
                    LocalDateTime dateTime = LocalDateTime.parse(
                            dateString.substring(0, 19),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    );
                    article.setPublicationDate(dateTime);

                    // Autor (opcional)
                    JsonNode authorNode = post.get("_embedded") != null ? post.get("_embedded").get("author") : null;
                    if (authorNode != null && authorNode.isArray() && authorNode.size() > 0) {
                        article.setAuthor(authorNode.get(0).get("name").asText());
                    } else {
                        article.setAuthor("N/A");
                    }

                    // Conteúdo completo
                    try {
                        Document articleDoc = Jsoup.connect(article.getUrl()).get();
                        Element contentElement = articleDoc.selectFirst("div.article-content, div.single-content, article");
                        if (contentElement != null) {
                            String cleanText = contentElement.text().replaceAll("\\s+", " ").trim();
                            article.setContent(cleanText);
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Falha ao obter conteúdo da URL " + article.getUrl());
                    }

                    articles.add(article);
                    System.out.println("✅ Encontrado: " + article.getTitle());
                }

            } catch (Exception e) {
                System.err.println("❌ Erro ao acessar " + url + ": " + e.getMessage());
            }
        }

        System.out.println("💾 Total de artigos encontrados: " + articles.size());
        return articleRepository.saveAll(articles);
    }

    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }
}
