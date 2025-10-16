package com.infomoney.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infomoney.model.Article;
import com.infomoney.repository.ArticleRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            "https://www.infomoney.com.br/wp-json/wp/v2/posts?per_page=10&_embed&page=";
    private static final int MAX_PAGES = 3;

    private static final Logger logger = LoggerFactory.getLogger(NewsScraperService.class);

    @Autowired
    private ArticleRepository articleRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Article> scrapeNews() {
        List<Article> articles = new ArrayList<>();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (int i = 1; i <= MAX_PAGES; i++) {
            String url = API_URL + i;
            logger.info("🔎 Buscando notícias da página: {}", url);

            try {
                String jsonResponse = restTemplate.getForObject(url, String.class);
                if (jsonResponse == null || jsonResponse.isEmpty()) {
                    logger.warn("⚠️ Nenhuma resposta da API nesta página.");
                    continue;
                }

                JsonNode root = objectMapper.readTree(jsonResponse);
                if (!root.isArray() || root.size() == 0) {
                    logger.warn("⚠️ Nenhum artigo encontrado nesta página.");
                    continue;
                }

                for (JsonNode post : root) {
                    Article article = new Article();

                    JsonNode linkNode = post.get("link");
                    String postUrl = linkNode != null ? linkNode.asText() : "";

                    if (!postUrl.contains("/mercados/")) {
                        continue;
                    }
                    article.setUrl(postUrl);

                    JsonNode titleNode = post.get("title");
                    article.setTitle(titleNode != null && titleNode.get("rendered") != null
                            ? titleNode.get("rendered").asText()
                            : "Título indisponível");

                    JsonNode excerptNode = post.get("excerpt");
                    article.setSubtitle(excerptNode != null && excerptNode.get("rendered") != null
                            ? excerptNode.get("rendered").asText().replaceAll("<[^>]*>", "").trim()
                            : "");

                    try {
                        JsonNode dateNode = post.get("date");
                        if (dateNode != null) {
                            String dateString = dateNode.asText().substring(0, 19);
                            LocalDateTime dateTime = LocalDateTime.parse(
                                    dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                            );
                            article.setPublicationDate(dateTime);
                        }
                    } catch (Exception e) {
                        article.setPublicationDate(null);
                    }

                    try {
                        JsonNode authorNode = post.get("_embedded") != null
                                ? post.get("_embedded").get("author")
                                : null;
                        if (authorNode != null && authorNode.isArray() && authorNode.size() > 0) {
                            article.setAuthor(authorNode.get(0).get("name").asText());
                        } else {
                            article.setAuthor("N/A");
                        }
                    } catch (Exception e) {
                        article.setAuthor("N/A");
                    }

                    try {
                        if (!article.getUrl().isEmpty()) {
                            Document doc = Jsoup.connect(article.getUrl()).get();
                            Element contentElement = doc.selectFirst("div.article-content, div.single-content, article");
                            if (contentElement != null) {
                                article.setContent(contentElement.text().replaceAll("\\s+", " ").trim());
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("⚠️ Falha ao obter conteúdo da URL: {}", article.getUrl(), e);
                    }

                    logger.info("--------------------------------------------------");
                    logger.info("Título: {}", article.getTitle());
                    logger.info("Subtítulo: {}", article.getSubtitle());
                    logger.info("Autor: {}", article.getAuthor());
                    logger.info("Data: {}", article.getPublicationDate() != null
                            ? article.getPublicationDate().format(outputFormatter)
                            : "N/A");
                    logger.info("URL: {}", article.getUrl());
                    logger.info("Conteúdo: {}", article.getContent());

                    articles.add(article);
                }

            } catch (Exception e) {
                logger.error("❌ Erro ao acessar {}: {}", url, e.getMessage(), e);
            }
        }

        logger.info("💾 Total de artigos encontrados: {}", articles.size());
        return articleRepository.saveAll(articles);
    }

    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }
}
