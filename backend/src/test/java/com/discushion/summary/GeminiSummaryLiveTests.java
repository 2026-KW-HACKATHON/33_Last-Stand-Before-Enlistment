package com.discushion.summary;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;
/** Explicit user-authorized real provider call; only disposable synthetic public-agenda source. */
@EnabledIfEnvironmentVariable(named="DISCUSHION_VERIFY_GEMINI",matches="true")
class GeminiSummaryLiveTests {
    GeminiSummaryGenerator generator(){
        var key=System.getenv("GEMINI_API_KEY");assertThat(key!=null && !key.isBlank() && !key.contains("<")).isTrue();
        return new GeminiSummaryGenerator(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build(),
            URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"),key,true,Duration.ofSeconds(15));
    }
    @Test void actualModelSummarizesSyntheticSourceAsOneThreeSentenceParagraph()throws Exception{
        var result=generator().generate("합성 시연 안건: 보행 안전 개선",
            "이 글은 실제 주민이나 사업을 나타내지 않는 폐기용 검증 원문입니다. 시연 구역의 횡단보도 앞에는 보행 공간이 부족합니다. 주민들은 통행 구간의 안내 표지를 개선하자는 의견을 제시했습니다. 작성자는 이 의견을 모아 담당 기관에 전달하는 방안을 제안합니다.");
        assertThat(result.status()).isEqualTo("SUCCEEDED");assertThat(result.summary()).doesNotContain("\n","\r","•");
        var sentences=java.text.BreakIterator.getSentenceInstance(java.util.Locale.KOREAN);sentences.setText(result.summary());int count=0;
        for(int start=sentences.first(),end=sentences.next();end!=java.text.BreakIterator.DONE;start=end,end=sentences.next())if(!result.summary().substring(start,end).isBlank())count++;
        assertThat(count).isEqualTo(3);
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("../.local-db"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("../.local-db/gemini-synthetic-summary.txt"),result.summary(),java.nio.charset.StandardCharsets.UTF_8);
    }
    @Test void actualModelDoesNotInventThreeSentencesForInsufficientSource(){
        assertThat(generator().generate("합성 안건","안녕").status()).isEqualTo("SOURCE_TOO_SHORT");
    }
}
