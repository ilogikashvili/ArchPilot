package com.ilia.gikashvili.archmain.ai;
import org.springframework.stereotype.Component; import org.springframework.core.env.Environment; import com.fasterxml.jackson.databind.*;
import java.net.*; import java.net.http.*; import java.time.Duration; import java.util.*;
@Component public class AiProviderClient {
 public static final String OLLAMA_DEFAULT="http://localhost:11434";
 private final Environment env; private final ObjectMapper json; private final AiKeyCipher cipher;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 public AiProviderClient(Environment e,ObjectMapper j,AiKeyCipher c){env=e;json=j;cipher=c;}
 private String base(AiProviderKey k){
  if(k.baseUrl!=null&&!k.baseUrl.isBlank())return k.baseUrl.trim().replaceAll("/+$","");
  return env.getProperty("app.ai.providers."+k.provider.name().toLowerCase(Locale.ROOT)+".base-url",switch(k.provider){case OPENAI->"https://api.openai.com/v1";case ANTHROPIC->"https://api.anthropic.com/v1";case GEMINI->"https://generativelanguage.googleapis.com/v1beta";case OPENROUTER->"https://openrouter.ai/api/v1";case OLLAMA->OLLAMA_DEFAULT;}).replaceAll("/+$","");
 }
 public String call(AiProviderKey k,String system,String prompt,boolean tiny) throws Exception {
  String model=k.model; String base=base(k); String secret=cipher.decrypt(k.encryptedKey); Map<String,Object> body=new LinkedHashMap<>(); String path;
  var b=HttpRequest.newBuilder().timeout(Duration.ofSeconds(k.provider==AiProviderKey.Provider.OLLAMA?600:90)).header("Content-Type","application/json");
  switch(k.provider){
   case ANTHROPIC -> {path="/messages";b.header("x-api-key",secret).header("anthropic-version","2023-06-01");body.put("model",model);body.put("system",system);body.put("max_tokens",tiny?16:4096);body.put("messages",List.of(Map.of("role","user","content",prompt)));}
   case GEMINI -> {path="/models/"+URLEncoder.encode(model,java.nio.charset.StandardCharsets.UTF_8)+":generateContent";b.header("x-goog-api-key",secret);body.put("system_instruction",Map.of("parts",List.of(Map.of("text",system))));body.put("contents",List.of(Map.of("parts",List.of(Map.of("text",prompt)))));body.put("generationConfig",Map.of("maxOutputTokens",tiny?16:4096));}
   case OLLAMA -> {path="/api/chat";if(secret!=null&&!secret.isBlank()&&!secret.equals("ollama-local"))b.header("Authorization","Bearer "+secret);body.put("model",model);body.put("stream",false);body.put("think",false);body.put("options",Map.of("num_predict",tiny?32:4096));body.put("messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",prompt)));}
   default -> {path="/chat/completions";b.header("Authorization","Bearer "+secret);body.put("model",model);body.put("max_tokens",tiny?16:4096);body.put("messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",prompt)));}
  }
  HttpResponse<String> response;
  try{response=http.send(b.uri(URI.create(base+path)).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
  catch(java.net.ConnectException e){throw new IllegalStateException(k.provider==AiProviderKey.Provider.OLLAMA?"Cannot reach Ollama at "+base+" (is `ollama serve` running on the same machine as the backend?)":"Cannot reach provider at "+base);}
  if(response.statusCode()<200||response.statusCode()>=300){
   if(k.provider==AiProviderKey.Provider.OLLAMA){String err="";try{err=json.readTree(response.body()).path("error").asText();}catch(Exception ignored){}throw new IllegalStateException("Ollama HTTP "+response.statusCode()+(err.isBlank()?"":": "+err)+(response.statusCode()==404?" (run `ollama pull "+model+"`)":""));}
   throw new IllegalStateException("Provider HTTP "+response.statusCode()+" (check key, model, quota and provider availability)");}
  JsonNode r=json.readTree(response.body());String text=switch(k.provider){case ANTHROPIC->r.path("content").path(0).path("text").asText();case GEMINI->r.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();case OLLAMA->r.path("message").path("content").asText();default->r.path("choices").path(0).path("message").path("content").asText();};
  text=stripThinking(text);
  if(text.isBlank()&&!tiny)throw new IllegalStateException("Provider returned no text");return text;
 }
 /** Reasoning models (qwen3, deepseek-r1) may wrap chain-of-thought in think tags; only the answer is kept. */
 public static String stripThinking(String t){if(t==null)return "";return t.replaceAll("(?s)<think>.*?</think>","").replaceAll("(?s)^.*</think>","").trim();}
 /** Lists models installed in an Ollama server (GET /api/tags). */
 public List<Map<String,Object>> ollamaModels(String baseUrl) throws Exception {
  String base=(baseUrl==null||baseUrl.isBlank()?OLLAMA_DEFAULT:baseUrl.trim()).replaceAll("/+$","");
  URI uri=URI.create(base+"/api/tags"); if(!Set.of("http","https").contains(String.valueOf(uri.getScheme())))throw new IllegalStateException("Base URL must be http(s)");
  HttpResponse<String> r;
  try{r=http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofString());}catch(java.net.ConnectException e){throw new IllegalStateException("Cannot reach Ollama at "+base);}
  if(r.statusCode()!=200)throw new IllegalStateException("Ollama HTTP "+r.statusCode());
  List<Map<String,Object>> out=new ArrayList<>(); for(JsonNode m:json.readTree(r.body()).path("models")){Map<String,Object> x=new LinkedHashMap<>();x.put("name",m.path("name").asText());x.put("size",m.path("size").asLong());x.put("modifiedAt",m.path("modified_at").asText());out.add(x);} return out;
 }
}
