# Desafio Java Backend Jr - Scraper de Notícias InfoMoney

Este projeto é uma solução para o desafio técnico proposto, que consiste em desenvolver uma aplicação Java utilizando Spring Boot para extrair notícias da seção de Mercado do site InfoMoney.

## Requisitos

Para executar este projeto, você precisará ter instalado:

*   **Java Development Kit (JDK) 11** ou superior.
*   **Apache Maven 3.6.3** ou superior.

## Estrutura do Projeto

O projeto segue a estrutura padrão de um projeto Spring Boot Maven:

```
infomoney-news-scraper
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── infomoney
│   │   │           ├── config
│   │   │           │   └── AppConfig.java
│   │   │           ├── controller
│   │   │           │   └── NewsController.java
│   │   │           ├── model
│   │   │           │   └── Article.java
│   │   │           ├── repository
│   │   │           │   └── ArticleRepository.java
│   │   │           ├── service
│   │   │           │   └── NewsScraperService.java
│   │   │           └── InfomoneyNewsScraperApplication.java
│   │   └── resources
│   │       └── application.properties
│   └── test
│       └── java
│           └── com
│               └── infomoney
└── README.md
```

## Como Executar

Siga os passos abaixo para construir e executar a aplicação:

1.  **Navegue até o diretório do projeto:**
    ```bash
    cd infomoney-news-scraper
    ```

2.  **Compile e empacote o projeto usando Maven:**
    ```bash
    mvn clean install
    ```

3.  **Execute a aplicação Spring Boot:**
    ```bash
    java -jar target/infomoney-news-scraper-0.0.1-SNAPSHOT.jar
    ```

A aplicação será iniciada na porta padrão 8081.

## Endpoints da API

A aplicação expõe os seguintes endpoints REST:

*   **`GET /api/news/scrape`**: Inicia o processo de scraping das notícias do InfoMoney, salva-as no banco de dados H2 e retorna a lista de artigos raspados.
*   **`GET /api/news`**: Retorna todos os artigos de notícias que foram previamente raspados e salvos no banco de dados.

### Scraping de Notícias

O serviço `NewsScraperService` é responsável por realizar o scraping das notícias. Ele utiliza as seguintes bibliotecas e abordagens:

*   **Jsoup**: Para parsear o HTML das páginas do InfoMoney e extrair os dados das notícias (URL, título, subtítulo, autor, data de publicação).
*   **RestTemplate**: Para fazer requisições HTTP à API de 

paginação do InfoMoney, que retorna dados em formato JSON.
*   **ObjectMapper (Jackson)**: Para desserializar as respostas JSON da API de paginação em objetos Java.

O processo de scraping funciona da seguinte forma:

1.  Primeiro, a página inicial da seção de Mercado do InfoMoney é acessada via Jsoup para extrair as notícias visíveis inicialmente.
2.  Em seguida, a API de paginação (`https://www.infomoney.com.br/wp-json/infomoney/v1/posts?editoria=mercados&page=`) é chamada para as próximas duas 

páginas (totalizando 3 páginas, conforme o desafio). Cada resposta JSON é parseada, e os snippets HTML contidos nela são processados pelo Jsoup para extrair os detalhes das notícias.
3.  Para cada notícia encontrada, é feita uma requisição individual à URL da notícia para extrair o conteúdo completo, removendo tags HTML e quebras de linha.

### Persistência de Dados

*   **Spring Data JPA**: Utilizado para abstrair a camada de persistência, facilitando a interação com o banco de dados.
*   **H2 Database**: Um banco de dados em memória leve, ideal para desenvolvimento e testes. A configuração está em `application.properties` e permite o acesso ao console H2 em `/h2-console`.

### API REST

O `NewsController` expõe dois endpoints REST:

*   `GET /api/news/scrape`: Aciona o serviço de scraping e salva as notícias no banco de dados. Retorna a lista de `Article`s raspados.
*   `GET /api/news`: Retorna todos os `Article`s atualmente armazenados no banco de dados.

## Considerações Finais

Este projeto demonstra a capacidade de integrar diferentes tecnologias (Spring Boot, Jsoup, REST APIs, JPA, H2) para resolver um problema de extração e persistência de dados web. A abordagem busca equilibrar a minimização de requisições com a necessidade de obter o conteúdo completo das notícias, conforme solicitado no desafio.

---



