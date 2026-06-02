package com.naijavehicle.api.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Bean
    public MongoClient mongoClient() {
        String uri = System.getenv("MONGO_URL");
        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("MONGO_URL environment variable is not set");
        }
        return MongoClients.create(uri);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        String dbName = System.getenv("DB_NAME");
        if (dbName == null || dbName.isBlank()) {
            throw new IllegalStateException("DB_NAME environment variable is not set");
        }
        return new SimpleMongoClientDatabaseFactory(mongoClient, dbName);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
        return new MongoTemplate(factory);
    }
}
