package com.infomoney.controller;

import com.infomoney.dto.ArticleDTO;
import com.infomoney.model.Article;
import com.infomoney.service.NewsScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private NewsScraperService newsScraperService;

    private static final Logger logger = LoggerFactory.getLogger(NewsScraperService.class);

    @GetMapping("/scrape")
    public ResponseEntity<?> scrapeAndSaveNews() {
        logger.info("Iniciando raspagem de notícias...");
        List<ArticleDTO> articles = newsScraperService.scrapeNews();
        logger.info("Raspagem concluída: " + articles.size() + " notícias salvas.");
        return ResponseEntity.ok(articles);
    }

    @GetMapping
    public ResponseEntity<List<Article>> getAllArticles() {
        List<Article> articles = newsScraperService.getAllArticles();
        return ResponseEntity.ok(articles);
    }
}
