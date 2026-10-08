package com.discushion.summary;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
class GeminiSummaryGeneratorTests {
    HttpServer server;String response;int status;final AtomicInteger calls=new AtomicInteger();String request,key;boolean slowBody;
    final JsonMapper json=JsonMapper.builder().build();
    @BeforeEach void start()throws Exception{
        status=200;calls.set(0);slowBody=false;server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/generate",exchange->{calls.incrementAndGet();key=exchange.getRequestHeaders().getFirst("x-goog-api-key");request=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);var bytes=response.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);if(slowBody){try{Thread.sleep(1500);}catch(InterruptedException e){Thread.currentThread().interrupt();}}try(var out=exchange.getResponseBody()){out.write(bytes);}});server.start();
    }
    @AfterEach void stop(){server.stop(0);}
    GeminiSummaryGenerator generator(boolean enabled){return new GeminiSummaryGenerator(HttpClient.newHttpClient(),URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/generate"),"synthetic-key",enabled,Duration.ofSeconds(2));}
    void output(boolean insufficient,List<String> sentences){
        var value=json.writeValueAsString(Map.of("insufficientSource",insufficient,"sentences",sentences));
        response=json.writeValueAsString(Map.of("candidates",List.of(Map.of("finishReason","STOP","content",Map.of("parts",List.of(Map.of("text",value)))))));
    }
    @Test void sendsOnlySourceAndJoinsExactlyThreeSentences(){
        output(false,List.of("첫 문장입니다.","두 번째 문장입니다.","마지막 문장입니다."));
        var result=generator(true).generate("제목","원문에만 있는 정보");assertThat(result.status()).isEqualTo("SUCCEEDED");assertThat(result.summary()).isEqualTo("첫 문장입니다. 두 번째 문장입니다. 마지막 문장입니다.");
        assertThat(key).isEqualTo("synthetic-key");assertThat(request).contains("원문에만 있는 정보").doesNotContain("synthetic-key");
        assertThat(json.readTree(request).has("tools")).isFalse();
    }
    @Test void insufficientSourceReturnsNoInventedSummary(){output(true,List.of());assertThat(generator(true).generate("제목","한 단어").status()).isEqualTo("SOURCE_TOO_SHORT");}
    @Test void rejectsWrongCountBulletAndMultipleSentences(){
        for(var sentences:List.of(List.of("한 문장입니다."),List.of("- 첫 문장입니다.","둘째입니다.","셋째입니다."),List.of("첫 문장입니다. 다른 문장입니다.","둘째입니다.","셋째입니다."))){
            output(false,sentences);assertThat(generator(true).generate("제목","원문").status()).isEqualTo("FAILED");
        }
    }
    @Test void providerErrorsAndMalformedResponsesFailWithoutRetry(){response="provider private details";status=429;assertThat(generator(true).generate("제목","원문").status()).isEqualTo("FAILED");assertThat(calls).hasValue(1);status=200;assertThat(generator(true).generate("제목","원문").status()).isEqualTo("FAILED");assertThat(calls).hasValue(2);}
    @Test void disabledProviderDoesNotSendSource(){response="unused";assertThat(generator(false).generate("제목","원문").status()).isEqualTo("FAILED");assertThat(calls).hasValue(0);}
    @Test void responseBodyDelayIsBoundedAndNeverRetried(){
        output(false,List.of("첫 문장입니다.","둘째입니다.","셋째입니다."));slowBody=true;
        var bounded=new GeminiSummaryGenerator(HttpClient.newHttpClient(),URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/generate"),"synthetic-key",true,Duration.ofSeconds(1));
        assertThat(bounded.generate("제목","원문").status()).isEqualTo("FAILED");
        assertThat(calls).hasValue(1);
    }
}
