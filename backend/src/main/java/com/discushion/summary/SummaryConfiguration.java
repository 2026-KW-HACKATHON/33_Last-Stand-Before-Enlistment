package com.discushion.summary;
import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.share.SharedPostAccess;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
@Configuration(proxyBeanMethods=false) @Profile("!local")
class SummaryConfiguration {
    @Bean SummaryGenerator summaryGenerator(Environment env){
        String model=env.getProperty("GEMINI_MODEL","gemini-3.5-flash-lite");
        if(model.isBlank() || model.contains("<"))model="gemini-3.5-flash-lite";
        if(!"gemini-3.5-flash-lite".equals(model))throw new IllegalStateException("Unconfirmed Gemini model");
        return new GeminiSummaryGenerator(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build(),
            URI.create("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent"),
            env.getProperty("GEMINI_API_KEY"),env.getProperty("AI_SUMMARIES_ENABLED",Boolean.class,false),Duration.ofSeconds(15));
    }
    @Bean SummaryService summaryService(CurrentActorProvider actors,SharedPostAccess shares,DataSource source,PlatformTransactionManager manager,SummaryGenerator generator){
        var writes=new TransactionTemplate(manager);writes.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return new SummaryService(actors,shares,new JdbcSummaryStore(source),generator,writes);
    }
}
