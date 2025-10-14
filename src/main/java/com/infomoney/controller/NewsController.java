package com.infomoney.controller;

import com.infomoney.model.Article;
import com.infomoney.service.NewsScraperService;
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

    // Endpoint para executar a raspagem e salvar no banco
    @GetMapping("/scrape")
    public ResponseEntity<?> scrapeAndSaveNews() {
        System.out.println("Iniciando raspagem de notícias...");
        List<Article> articles = newsScraperService.scrapeNews();
        System.out.println("Raspagem concluída: " + articles.size() + " notícias salvas.");
        return ResponseEntity.ok(articles);
    }

    // Endpoint para listar todas as notícias armazenadas
    @GetMapping
    public ResponseEntity<List<Article>> getAllArticles() {
        List<Article> articles = newsScraperService.getAllArticles();
        return ResponseEntity.ok(articles);
    }
}
