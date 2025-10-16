package com.infomoney.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infomoney.dto.ArticleDTO;
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

    public List<ArticleDTO> scrapeNews() {
        List<ArticleDTO> dtos = new ArrayList<>();
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
                    String postUrl = post.path("link").asText("");
                    if (!postUrl.contains("/mercados/")) continue;

                    ArticleDTO dto = new ArticleDTO();
                    dto.setUrl(postUrl);
                    dto.setTitle(post.path("title").path("rendered").asText("Título indisponível"));
                    dto.setSubtitle(post.path("excerpt").path("rendered").asText("").replaceAll("<[^>]*>", "").trim());

                    try {
                        String dateString = post.path("date").asText("").substring(0, 19);
                        LocalDateTime dateTime = LocalDateTime.parse(
                                dateString,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                        );
                        dto.setPublicationDate(dateTime);
                    } catch (Exception e) {
                        dto.setPublicationDate(null);
                    }

                    try {
                        JsonNode authorNode = post.path("_embedded").path("author");
                        dto.setAuthor(authorNode.isArray() && authorNode.size() > 0
                                ? authorNode.get(0).path("name").asText()
                                : "N/A");
                    } catch (Exception e) {
                        dto.setAuthor("N/A");
                    }

                    try {
                        if (!dto.getUrl().isEmpty()) {
                            Document doc = Jsoup.connect(dto.getUrl()).get();
                            Element contentElement = doc.selectFirst("div.article-content, div.single-content, article");
                            if (contentElement != null) {
                                dto.setContent(contentElement.text().replaceAll("\\s+", " ").trim());
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("⚠️ Falha ao obter conteúdo da URL: {}", dto.getUrl(), e);
                    }

                    logger.info("Título: {}", dto.getTitle());
                    logger.info("Subtítulo: {}", dto.getSubtitle());
                    logger.info("Autor: {}", dto.getAuthor());
                    logger.info("Data: {}", dto.getPublicationDate() != null ? dto.getPublicationDate().format(outputFormatter) : "N/A");
                    logger.info("URL: {}", dto.getUrl());
                    logger.info("Conteúdo: {}", dto.getContent());

                    dtos.add(dto);
                }

            } catch (Exception e) {
                logger.error("❌ Erro ao acessar {}: {}", url, e.getMessage(), e);
            }
        }

        logger.info("💾 Total de artigos encontrados: {}", dtos.size());
        return dtos;
    }

      public List<Article> saveArticles(List<ArticleDTO> dtos) {
        List<Article> articles = dtos.stream().map(dto -> {
            Article article = new Article();
            article.setTitle(dto.getTitle());
            article.setSubtitle(dto.getSubtitle());
            article.setAuthor(dto.getAuthor());
            article.setUrl(dto.getUrl());
            article.setContent(dto.getContent());
            article.setPublicationDate(dto.getPublicationDate());
            return article;
        }).toList();

        return articleRepository.saveAll(articles);
    }

    public List<Article> scrapeAndSaveNews() {
        List<ArticleDTO> dtos = scrapeNews();
        return saveArticles(dtos);
    }

    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }
}
