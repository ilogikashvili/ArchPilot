package com.ilia.gikashvili.archmain.codegen;

import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.model.EdgeModel;
import com.ilia.gikashvili.archmain.architecture.model.NodeModel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates Spring Boot (Java 17, Gradle) starter code from an architecture diagram.
 *
 * Component mapping:
 *   service / api  -> runnable Spring Boot application (api = gateway that routes to services)
 *   web            -> static frontend placeholder that calls its backends
 *   database       -> PostgreSQL in docker-compose + JPA entity/repository in every service using it
 *   queue          -> RabbitMQ in docker-compose + publisher / listener classes on connected services
 *   cache          -> Redis in docker-compose + cache config/service on connected services
 *   external       -> typed HTTP client interface on the services that call it
 */
@Component
public class JavaSpringGenerator implements CodeGenerator {

    private static final String BOOT_VERSION = "3.3.5";

    @Override
    public String language() { return "java"; }

    private record Ctx(ArchitectureDocument doc, CodegenOptions opt, Map<String, NodeModel> byId,
                       Map<String, Integer> ports) {}

    @Override
    public Map<String, String> generate(ArchitectureDocument doc, CodegenOptions opt) {
        List<NodeModel> nodes = doc.nodes() == null ? List.of() : doc.nodes();
        Map<String, NodeModel> byId = new LinkedHashMap<>();
        nodes.forEach(n -> byId.put(n.id(), n));
        Map<String, Integer> ports = new LinkedHashMap<>();
        int port = 8081;
        for (NodeModel n : apps(nodes)) ports.put(n.id(), port++);
        Ctx ctx = new Ctx(doc, opt, byId, ports);

        Map<String, String> files = new TreeMap<>();
        String root = slug(opt.projectName()) + "/";
        List<NodeModel> apps = apps(nodes);

        if (opt.multiModule()) {
            files.put(root + "settings.gradle", "rootProject.name = '" + slug(opt.projectName()) + "'\n\ninclude 'common'\n"
                    + apps.stream().map(n -> "include '" + slug(n.label()) + "'\n").collect(Collectors.joining()));
            files.put(root + "build.gradle", rootBuild(opt));
            files.put(root + "common/build.gradle", "plugins { id 'java-library' }\n");
            String commonPkg = opt.groupId() + ".common";
            files.put(root + "common/src/main/java/" + path(commonPkg) + "/ApiResponse.java", """
                    package %s;

                    import java.time.Instant;

                    /** Shared response envelope used by every module. */
                    public record ApiResponse<T>(boolean success, T data, String error, Instant timestamp) {
                        public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, data, null, Instant.now()); }
                        public static <T> ApiResponse<T> fail(String error) { return new ApiResponse<>(false, null, error, Instant.now()); }
                    }
                    """.formatted(commonPkg));
        }

        for (NodeModel n : apps) {
            String dir = root + slug(n.label()) + "/";
            if ("web".equals(n.type())) {
                webApp(ctx, n, dir, files);
            } else {
                springApp(ctx, n, dir, files);
            }
        }

        files.put(root + "docker-compose.yml", compose(ctx, apps));
        files.put(root + "README.md", readme(ctx, apps));
        files.put(root + ".gitignore", "build/\n.gradle/\n*.iml\n.idea/\nnode_modules/\n");
        return files;
    }

    // ---------------------------------------------------------------- helpers

    private static List<NodeModel> apps(List<NodeModel> nodes) {
        return nodes.stream().filter(n -> Set.of("service", "api", "web").contains(n.type())).toList();
    }

    static String slug(String s) {
        String out = (s == null ? "" : s).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return out.isEmpty() ? "component" : out;
    }

    static String pascal(String s) {
        StringBuilder b = new StringBuilder();
        for (String part : (s == null ? "" : s).split("[^A-Za-z0-9]+")) {
            if (part.isEmpty()) continue;
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        String out = b.toString();
        if (out.isEmpty()) return "Component";
        return Character.isDigit(out.charAt(0)) ? "C" + out : out;
    }

    static String pkgPart(String s) {
        String p = slug(s).replace("-", "");
        return Character.isDigit(p.charAt(0)) ? "c" + p : p;
    }

    private static String path(String pkg) { return pkg.replace('.', '/'); }

    private List<EdgeModel> outgoing(Ctx c, NodeModel n) {
        return c.doc().edges() == null ? List.of() : c.doc().edges().stream().filter(e -> n.id().equals(e.sourceId()) && c.byId().containsKey(e.targetId())).toList();
    }

    private List<EdgeModel> incoming(Ctx c, NodeModel n) {
        return c.doc().edges() == null ? List.of() : c.doc().edges().stream().filter(e -> n.id().equals(e.targetId()) && c.byId().containsKey(e.sourceId())).toList();
    }

    private List<NodeModel> targetsOfType(Ctx c, NodeModel n, String type) {
        return outgoing(c, n).stream().map(e -> c.byId().get(e.targetId())).filter(t -> type.equals(t.type())).distinct().toList();
    }

    private String rootBuild(CodegenOptions o) {
        return """
                plugins {
                    id 'org.springframework.boot' version '%s' apply false
                    id 'io.spring.dependency-management' version '1.1.6' apply false
                }

                subprojects {
                    apply plugin: 'java'
                    group = '%s'
                    version = '0.0.1-SNAPSHOT'
                    java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
                    repositories { mavenCentral() }
                    tasks.withType(Test).configureEach { useJUnitPlatform() }
                }
                """.formatted(BOOT_VERSION, o.groupId());
    }

    private String serviceBuild(Ctx c, NodeModel n) {
        Set<String> deps = new LinkedHashSet<>(List.of(
                "org.springframework.boot:spring-boot-starter-web",
                "org.springframework.boot:spring-boot-starter-actuator",
                "org.springframework.boot:spring-boot-starter-validation"));
        if (!targetsOfType(c, n, "database").isEmpty()) {
            deps.add("org.springframework.boot:spring-boot-starter-data-jpa");
        }
        boolean usesQueue = !targetsOfType(c, n, "queue").isEmpty()
                || incoming(c, n).stream().anyMatch(e -> "queue".equals(c.byId().get(e.sourceId()).type()));
        if (usesQueue) deps.add("org.springframework.boot:spring-boot-starter-amqp");
        if (!targetsOfType(c, n, "cache").isEmpty()) deps.add("org.springframework.boot:spring-boot-starter-data-redis");

        StringBuilder b = new StringBuilder();
        if (c.opt().multiModule()) {
            b.append("plugins {\n    id 'org.springframework.boot'\n    id 'io.spring.dependency-management'\n}\n\n");
        } else {
            b.append("plugins {\n    id 'java'\n    id 'org.springframework.boot' version '").append(BOOT_VERSION)
                    .append("'\n    id 'io.spring.dependency-management' version '1.1.6'\n}\n\n")
                    .append("group = '").append(c.opt().groupId()).append("'\nversion = '0.0.1-SNAPSHOT'\n\n")
                    .append("java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }\n\nrepositories { mavenCentral() }\n\n")
                    .append("tasks.named('test') { useJUnitPlatform() }\n\n");
        }
        b.append("dependencies {\n");
        if (c.opt().multiModule()) b.append("    implementation project(':common')\n");
        deps.forEach(d -> b.append("    implementation '").append(d).append("'\n"));
        if (deps.contains("org.springframework.boot:spring-boot-starter-data-jpa")) b.append("    runtimeOnly 'org.postgresql:postgresql'\n");
        b.append("    testImplementation 'org.springframework.boot:spring-boot-starter-test'\n}\n");
        return b.toString();
    }

    private void springApp(Ctx c, NodeModel n, String dir, Map<String, String> files) {
        String name = pascal(n.label()).replaceAll("Service$", "");
        if (name.isEmpty()) name = "Main";
        String pkg = c.opt().groupId() + "." + pkgPart(n.label());
        String src = dir + "src/main/java/" + path(pkg) + "/";
        boolean gateway = "api".equals(n.type());

        files.put(dir + "build.gradle", serviceBuild(c, n));
        if (!c.opt().multiModule()) {
            files.put(dir + "settings.gradle", "rootProject.name = '" + slug(n.label()) + "'\n");
        }
        files.put(dir + "Dockerfile", """
                FROM eclipse-temurin:17-jre
                WORKDIR /app
                COPY build/libs/*-SNAPSHOT.jar app.jar
                EXPOSE %d
                ENTRYPOINT ["java", "-jar", "app.jar"]
                """.formatted(c.ports().get(n.id())));

        files.put(src + name + "Application.java", """
                package %s;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                /**
                 * %s
                 * %s
                 */
                @SpringBootApplication
                public class %sApplication {
                    public static void main(String[] args) {
                        SpringApplication.run(%sApplication.class, args);
                    }
                }
                """.formatted(pkg, n.label(), blank(n.description()) ? "Generated by ArchMain." : n.description(), name, name));

        // ---- application.yml
        StringBuilder yml = new StringBuilder();
        yml.append("server:\n  port: ").append(c.ports().get(n.id())).append("\n\nspring:\n  application:\n    name: ").append(slug(n.label())).append("\n");
        List<NodeModel> dbs = targetsOfType(c, n, "database");
        if (!dbs.isEmpty()) {
            String db = slug(dbs.get(0).label());
            yml.append("  datasource:\n    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/").append(db.replace("-", "_"))
                    .append("}\n    username: ${DATABASE_USER:postgres}\n    password: ${DATABASE_PASSWORD:postgres}\n")
                    .append("  jpa:\n    hibernate:\n      ddl-auto: update\n");
        }
        if (!targetsOfType(c, n, "cache").isEmpty()) yml.append("  data:\n    redis:\n      host: ${REDIS_HOST:localhost}\n      port: 6379\n");
        boolean amqp = serviceBuild(c, n).contains("amqp");
        if (amqp) yml.append("  rabbitmq:\n    host: ${RABBITMQ_HOST:localhost}\n    port: 5672\n");
        List<EdgeModel> httpOut = outgoing(c, n).stream().filter(e -> Set.of("service", "api", "external").contains(c.byId().get(e.targetId()).type())).toList();
        if (!httpOut.isEmpty()) {
            yml.append("\nclients:\n");
            for (EdgeModel e : httpOut) {
                NodeModel t = c.byId().get(e.targetId());
                String url = "external".equals(t.type()) ? "https://api.example.com" : "http://localhost:" + c.ports().get(t.id());
                yml.append("  ").append(slug(t.label())).append(": ${").append(slug(t.label()).toUpperCase(Locale.ROOT).replace("-", "_")).append("_URL:").append(url).append("}\n");
            }
        }
        yml.append("\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: health,info\n");
        files.put(dir + "src/main/resources/application.yml", yml.toString());

        // ---- controller: one endpoint per incoming synchronous caller
        StringBuilder endpoints = new StringBuilder();
        Set<String> seen = new HashSet<>();
        for (EdgeModel e : incoming(c, n)) {
            NodeModel s = c.byId().get(e.sourceId());
            if ("queue".equals(s.type())) continue;
            String method = "handleFrom" + pascal(s.label());
            if (!seen.add(method)) continue;
            endpoints.append("""

                        /** Called by %s%s. */
                        @PostMapping("/from-%s")
                        public ResponseEntity<Map<String, Object>> %s(@RequestBody Map<String, Object> request) {
                            return ResponseEntity.ok(service.process("%s", request));
                        }
                    """.formatted(s.label(), blank(e.label()) ? "" : " via " + e.label(), slug(s.label()), method, slug(s.label())));
        }
        String base = gateway ? "/api" : "/api/" + slug(n.label());
        files.put(src + "controller/" + name + "Controller.java", """
                package %s.controller;

                import %s.service.%sService;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.*;

                import java.util.Map;

                @RestController
                @RequestMapping("%s")
                public class %sController {

                    private final %sService service;

                    public %sController(%sService service) {
                        this.service = service;
                    }

                    @GetMapping("/status")
                    public Map<String, Object> status() {
                        return Map.of("component", "%s", "status", "UP");
                    }

                    @PostMapping
                    public ResponseEntity<Map<String, Object>> handle(@RequestBody Map<String, Object> request) {
                        return ResponseEntity.ok(service.process("client", request));
                    }
                %s}
                """.formatted(pkg, pkg, name, base, name, name, name, name, n.label(), endpoints));

        // ---- dependencies the service class will need
        List<String> fields = new ArrayList<>();
        List<String> imports = new ArrayList<>();
        List<String> calls = new ArrayList<>();

        for (EdgeModel e : httpOut) {
            NodeModel t = c.byId().get(e.targetId());
            String cn = pascal(t.label()) + "Client";
            if (fields.stream().anyMatch(f -> f.startsWith(cn + " "))) continue;
            imports.add(pkg + ".client." + cn);
            fields.add(cn + " " + lower(cn));
            calls.add("        result.put(\"" + slug(t.label()) + "\", " + lower(cn) + ".send(request));");
            boolean ext = "external".equals(t.type());
            String targetPath = ext ? "/" : ("api".equals(t.type()) ? "/api/from-" : "/api/" + slug(t.label()) + "/from-") + (ext ? "" : slug(n.label()));
            files.put(src + "client/" + cn + ".java", """
                    package %s.client;

                    import org.springframework.beans.factory.annotation.Value;
                    import org.springframework.core.ParameterizedTypeReference;
                    import org.springframework.stereotype.Component;
                    import org.springframework.web.client.RestClient;

                    import java.util.Map;

                    /**
                     * HTTP client for %s (%s%s).
                     */
                    @Component
                    public class %s {

                        private final RestClient http;

                        public %s(@Value("${clients.%s}") String baseUrl) {
                            this.http = RestClient.builder().baseUrl(baseUrl).build();
                        }

                        public Map<String, Object> send(Map<String, Object> payload) {
                            return http.post()
                                    .uri("%s")
                                    .body(payload)
                                    .retrieve()
                                    .body(new ParameterizedTypeReference<>() {});
                        }
                    }
                    """.formatted(pkg, t.label(), ext ? "external system" : t.type(), blank(e.label()) ? "" : ", " + e.label(),
                    cn, cn, slug(t.label()), targetPath));
        }

        for (NodeModel db : dbs) {
            String entity = pascal(n.label()).replaceAll("(Service|Api|Gateway)$", "") + "Record";
            if (entity.equals("Record")) entity = "Item";
            String repo = entity + "Repository";
            if (fields.stream().anyMatch(f -> f.startsWith(repo + " "))) continue;
            imports.add(pkg + ".repository." + repo);
            imports.add(pkg + ".entity." + entity);
            fields.add(repo + " " + lower(repo));
            calls.add("        " + entity + " saved = " + lower(repo) + ".save(new " + entity + "(String.valueOf(request)));\n"
                    + "        result.put(\"storedId\", saved.getId());");
            files.put(src + "entity/" + entity + ".java", """
                    package %s.entity;

                    import jakarta.persistence.*;
                    import java.time.Instant;

                    /** Stored in %s. */
                    @Entity
                    @Table(name = "%s")
                    public class %s {

                        @Id
                        @GeneratedValue(strategy = GenerationType.IDENTITY)
                        private Long id;

                        @Column(columnDefinition = "TEXT")
                        private String payload;

                        private Instant createdAt = Instant.now();

                        protected %s() {}

                        public %s(String payload) {
                            this.payload = payload;
                        }

                        public Long getId() { return id; }
                        public String getPayload() { return payload; }
                        public Instant getCreatedAt() { return createdAt; }
                    }
                    """.formatted(pkg, db.label(), slug(entity).replace("-", "_") + "s", entity, entity, entity));
            files.put(src + "repository/" + repo + ".java", """
                    package %s.repository;

                    import %s.entity.%s;
                    import org.springframework.data.jpa.repository.JpaRepository;

                    public interface %s extends JpaRepository<%s, Long> {
                    }
                    """.formatted(pkg, pkg, entity, repo, entity));
        }

        for (NodeModel cache : targetsOfType(c, n, "cache")) {
            imports.add("org.springframework.data.redis.core.StringRedisTemplate");
            fields.add("StringRedisTemplate redis");
            calls.add(0, "        String cached = redis.opsForValue().get(\"" + slug(n.label()) + ":\" + request.hashCode());\n"
                    + "        if (cached != null) result.put(\"cache\", \"HIT\");\n"
                    + "        redis.opsForValue().set(\"" + slug(n.label()) + ":\" + request.hashCode(), String.valueOf(request), java.time.Duration.ofMinutes(5));");
            files.put(src + "config/CacheConfig.java", """
                    package %s.config;

                    import org.springframework.cache.annotation.EnableCaching;
                    import org.springframework.context.annotation.Configuration;

                    /** Uses %s (Redis). */
                    @Configuration
                    @EnableCaching
                    public class CacheConfig {
                    }
                    """.formatted(pkg, cache.label()));
            break;
        }

        List<NodeModel> queuesOut = targetsOfType(c, n, "queue");
        for (NodeModel q : queuesOut) {
            String pub = pascal(q.label()) + "Publisher";
            imports.add(pkg + ".messaging." + pub);
            fields.add(pub + " " + lower(pub));
            calls.add("        " + lower(pub) + ".publish(request);\n        result.put(\"published\", \"" + slug(q.label()) + "\");");
            files.put(src + "messaging/" + pub + ".java", """
                    package %s.messaging;

                    import org.springframework.amqp.core.Queue;
                    import org.springframework.amqp.rabbit.core.RabbitTemplate;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.stereotype.Component;

                    import java.util.Map;

                    /** Publishes messages to %s. */
                    @Component
                    public class %s {

                        public static final String QUEUE = "%s";

                        private final RabbitTemplate rabbit;

                        public %s(RabbitTemplate rabbit) {
                            this.rabbit = rabbit;
                        }

                        public void publish(Map<String, Object> message) {
                            rabbit.convertAndSend(QUEUE, message);
                        }

                        @Configuration
                        static class QueueConfig {
                            @Bean
                            Queue %sQueue() {
                                return new Queue(QUEUE, true);
                            }
                        }
                    }
                    """.formatted(pkg, q.label(), pub, slug(q.label()), pub, lower(pascal(q.label()))));
        }
        for (EdgeModel e : incoming(c, n)) {
            NodeModel q = c.byId().get(e.sourceId());
            if (!"queue".equals(q.type())) continue;
            String lis = pascal(q.label()) + "Listener";
            files.put(src + "messaging/" + lis + ".java", """
                    package %s.messaging;

                    import %s.service.%sService;
                    import org.springframework.amqp.rabbit.annotation.RabbitListener;
                    import org.springframework.stereotype.Component;

                    import java.util.Map;

                    /** Consumes messages from %s. */
                    @Component
                    public class %s {

                        private final %sService service;

                        public %s(%sService service) {
                            this.service = service;
                        }

                        @RabbitListener(queuesToDeclare = @org.springframework.amqp.rabbit.annotation.Queue("%s"))
                        public void onMessage(Map<String, Object> message) {
                            service.process("%s", message);
                        }
                    }
                    """.formatted(pkg, pkg, name, q.label(), lis, name, lis, name, slug(q.label()), slug(q.label())));
        }

        // ---- service class
        String ctorParams = fields.stream().collect(Collectors.joining(", "));
        String fieldDecls = fields.stream().map(f -> "    private final " + f + ";\n").collect(Collectors.joining());
        String assigns = fields.stream().map(f -> f.substring(f.indexOf(' ') + 1)).map(f -> "        this." + f + " = " + f + ";\n").collect(Collectors.joining());
        String responsibilities = "";
        Object resp = n.properties() == null ? null : n.properties().get("responsibilities");
        if (resp instanceof List<?> l && !l.isEmpty()) {
            responsibilities = l.stream().map(x -> " *   - " + x + "\n").collect(Collectors.joining(" ", " *\n * Responsibilities:\n", ""));
        }
        files.put(src + "service/" + name + "Service.java", """
                package %s.service;

                %s
                import org.springframework.stereotype.Service;

                import java.util.LinkedHashMap;
                import java.util.Map;

                /**
                 * Business logic for %s.
                %s */
                @Service
                public class %sService {

                %s
                    public %sService(%s) {
                %s    }

                    public Map<String, Object> process(String source, Map<String, Object> request) {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("handledBy", "%s");
                        result.put("source", source);
                        // TODO: implement %s logic
                %s
                        return result;
                    }
                }
                """.formatted(pkg,
                imports.stream().distinct().map(i -> "import " + i + ";").collect(Collectors.joining("\n")),
                n.label(), responsibilities, name, fieldDecls, name, ctorParams, assigns, slug(n.label()), n.label(),
                String.join("\n", calls)));

        files.put(dir + "src/test/java/" + path(pkg) + "/" + name + "ApplicationTests.java", """
                package %s;

                import org.junit.jupiter.api.Test;

                class %sApplicationTests {
                    @Test
                    void placeholder() {
                        // TODO: add tests for %s
                    }
                }
                """.formatted(pkg, name, n.label()));
    }

    private void webApp(Ctx c, NodeModel n, String dir, Map<String, String> files) {
        String calls = outgoing(c, n).stream().map(e -> c.byId().get(e.targetId()))
                .filter(t -> c.ports().containsKey(t.id()))
                .map(t -> "      { name: '" + t.label() + "', url: 'http://localhost:" + c.ports().get(t.id()) + ("api".equals(t.type()) ? "/api" : "/api/" + slug(t.label())) + "' },")
                .collect(Collectors.joining("\n"));
        files.put(dir + "index.html", """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8" />
                  <title>%s</title>
                </head>
                <body>
                  <h1>%s</h1>
                  <p>%s</p>
                  <button id="send">Send test request</button>
                  <pre id="out"></pre>
                  <script>
                    const backends = [
                %s
                    ];
                    document.getElementById('send').onclick = async () => {
                      const out = document.getElementById('out');
                      for (const b of backends) {
                        const res = await fetch(b.url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ hello: 'world' }) });
                        out.textContent += b.name + ': ' + (await res.text()) + '\\n';
                      }
                    };
                  </script>
                </body>
                </html>
                """.formatted(n.label(), n.label(), blank(n.description()) ? "Frontend placeholder generated by ArchMain." : n.description(), calls));
        files.put(dir + "README.md", "# " + n.label() + "\n\nStatic placeholder for the web frontend. Replace with your framework of choice.\n");
    }

    private String compose(Ctx c, List<NodeModel> apps) {
        StringBuilder b = new StringBuilder("services:\n");
        List<NodeModel> nodes = c.doc().nodes() == null ? List.of() : c.doc().nodes();
        for (NodeModel n : nodes) {
            String s = slug(n.label());
            switch (n.type()) {
                case "database" -> b.append("  ").append(s).append(":\n    image: postgres:16\n    environment:\n      POSTGRES_DB: ")
                        .append(s.replace("-", "_")).append("\n      POSTGRES_USER: postgres\n      POSTGRES_PASSWORD: postgres\n    ports:\n      - \"5432\"\n");
                case "queue" -> b.append("  ").append(s).append(":\n    image: rabbitmq:3-management\n    ports:\n      - \"5672\"\n      - \"15672\"\n");
                case "cache" -> b.append("  ").append(s).append(":\n    image: redis:7\n    ports:\n      - \"6379\"\n");
                default -> {}
            }
        }
        for (NodeModel n : apps) {
            if ("web".equals(n.type())) continue;
            String s = slug(n.label());
            b.append("  ").append(s).append(":\n    build: ./").append(s).append("\n    ports:\n      - \"")
                    .append(c.ports().get(n.id())).append(":").append(c.ports().get(n.id())).append("\"\n");
            List<String> env = new ArrayList<>();
            List<String> depends = new ArrayList<>();
            for (NodeModel t : outgoing(c, n).stream().map(e -> c.byId().get(e.targetId())).distinct().toList()) {
                String ts = slug(t.label());
                switch (t.type()) {
                    case "database" -> { env.add("DATABASE_URL: jdbc:postgresql://" + ts + ":5432/" + ts.replace("-", "_")); depends.add(ts); }
                    case "cache" -> { env.add("REDIS_HOST: " + ts); depends.add(ts); }
                    case "queue" -> { env.add("RABBITMQ_HOST: " + ts); depends.add(ts); }
                    case "service", "api" -> env.add(ts.toUpperCase(Locale.ROOT).replace("-", "_") + "_URL: http://" + ts + ":" + c.ports().get(t.id()));
                    default -> {}
                }
            }
            for (EdgeModel e : incoming(c, n)) {
                NodeModel q = c.byId().get(e.sourceId());
                if ("queue".equals(q.type())) { env.add("RABBITMQ_HOST: " + slug(q.label())); depends.add(slug(q.label())); }
            }
            if (!env.isEmpty()) {
                b.append("    environment:\n");
                env.stream().distinct().forEach(x -> b.append("      ").append(x).append("\n"));
            }
            if (!depends.isEmpty()) {
                b.append("    depends_on:\n");
                depends.stream().distinct().forEach(x -> b.append("      - ").append(x).append("\n"));
            }
        }
        return b.toString();
    }

    private String readme(Ctx c, List<NodeModel> apps) {
        StringBuilder b = new StringBuilder("# ").append(c.opt().projectName()).append("\n\nGenerated by ArchMain from the architecture diagram.\n\n");
        b.append("Layout: **").append(c.opt().multiModule() ? "multi-module Gradle project" : "one independent project per service").append("**\n\n");
        b.append("| Component | Type | Port |\n|---|---|---|\n");
        for (NodeModel n : c.doc().nodes() == null ? List.<NodeModel>of() : c.doc().nodes()) {
            b.append("| ").append(n.label()).append(" | ").append(n.type()).append(" | ")
                    .append(c.ports().containsKey(n.id()) ? c.ports().get(n.id()).toString() : "-").append(" |\n");
        }
        b.append("\n## Connections\n\n");
        for (EdgeModel e : c.doc().edges() == null ? List.<EdgeModel>of() : c.doc().edges()) {
            NodeModel s = c.byId().get(e.sourceId()), t = c.byId().get(e.targetId());
            if (s == null || t == null) continue;
            b.append("- ").append(s.label()).append(" → ").append(t.label()).append(" (").append(e.type())
                    .append(blank(e.label()) ? "" : ", " + e.label()).append(")\n");
        }
        b.append("\n## Run\n\n```bash\n");
        if (c.opt().multiModule()) {
            b.append("gradle build            # or add a Gradle wrapper with `gradle wrapper`\n");
        } else {
            apps.stream().filter(n -> !"web".equals(n.type())).forEach(n -> b.append("(cd ").append(slug(n.label())).append(" && gradle build)\n"));
        }
        b.append("docker compose up --build\n```\n");
        return b.toString();
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private static String lower(String s) { return Character.toLowerCase(s.charAt(0)) + s.substring(1); }
}
