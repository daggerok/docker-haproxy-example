package com.github.daggerok;

import io.vavr.Lazy;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.r2dbc.function.DatabaseClient;
import org.springframework.fu.jafu.ApplicationDsl;
import org.springframework.fu.jafu.ConfigurationDsl;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.springframework.fu.jafu.Jafu.webApplication;
import static org.springframework.fu.jafu.r2dbc.H2R2dbcDsl.r2dbcH2;
import static org.springframework.fu.jafu.web.WebFluxServerDsl.server;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.web.reactive.function.server.ServerResponse.accepted;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
class User {
  String login;
  String firstName;
  String lastName;
}

@RequiredArgsConstructor
class UserRepository {

  final DatabaseClient client;

  Flux<User> findAll() {
    return client.select()
                 .from("users")
                 .as(User.class)
                 .fetch()
                 .all();
  }

  Mono<Void> deleteAll() {
    return client.execute()
                 .sql("DELETE FROM users")
                 .fetch()
                 .one()
                 .then();
  }

  Mono<User> save(User user) {
    return client.insert()
                 .into(User.class)
                 .table("users")
                 .using(user)
                 .map((r, m) -> User.of(r.get("login", String.class),
                                        r.get("first_name", String.class),
                                        r.get("last_name", String.class)))
                 .one();
  }

  void init() {
    client.execute()
          .sql("      CREATE TABLE IF NOT EXISTS users (  " +
                   "    login VARCHAR PRIMARY KEY,        " +
                   "    first_name VARCHAR,               " +
                   "    last_name VARCHAR                 " +
                   "  );                                  "
          )
          .then()
          .then(deleteAll())
          .then(save(User.of("smaldini", "Stéphane", "Maldini")))
          .then(save(User.of("sdeleuze", "Sébastien", "Deleuze")))
          .then(save(User.of("jlong", "Joshua", "Long")))
          .then(save(User.of("bclozel", "Brian", "Clozel")))
          .block();
  }
}

@Log4j2
@RequiredArgsConstructor
class UserHandler {

  static final Lazy<String> defaultIdentifier = Lazy.of(() -> UUID.randomUUID().toString());

  static final String hostname = Try.of(() -> InetAddress.getLocalHost()
                                                         .getHostName())
                                    .getOrElseGet(e -> defaultIdentifier.get());

  static final Consumer<String> logIt = method ->
      log.info(() -> format("calling %s on host: %s", method, hostname));

  final UserRepository repository;

  Mono<ServerResponse> host(ServerRequest request) {
    logIt.accept("host");
    return ok().contentType(APPLICATION_JSON_UTF8)
               .body(Mono.just(singletonMap("hostname", hostname)), Map.class);
  }

  Mono<ServerResponse> add(ServerRequest request) {
    logIt.accept("add");
    return accepted().contentType(APPLICATION_JSON_UTF8)
                     .body(request.bodyToMono(User.class)
                                  .flatMap(repository::save), User.class);
  }

  Mono<ServerResponse> get(ServerRequest request) {
    logIt.accept("get");
    return ok().contentType(APPLICATION_JSON_UTF8)
               .body(repository.findAll(), User.class);
  }
}

public class FuJaFuApp {

  public static void main(String[] args) {

    Consumer<ConfigurationDsl> r2dbcConfig = cfg -> cfg
        .beans(beans -> beans.bean(UserRepository.class))
        .enable(r2dbcH2());

    Consumer<ConfigurationDsl> webFluxConfig = cfg -> cfg
        .beans(beans -> beans.bean(UserHandler.class))
        .enable(server(server -> server
            .router(router -> {
              UserHandler userHandler = cfg.ref(UserHandler.class);
              router.GET("/**host**", userHandler::host);
            })
            .router(router -> {
              UserHandler userHandler = cfg.ref(UserHandler.class);
              router.POST("/**", userHandler::add);
            })
            .router(router -> {
              UserHandler userHandler = cfg.ref(UserHandler.class);
              router.GET("/**", userHandler::get);
            })
            .codecs(codecs -> codecs.string()
                                    .jackson())));

    Consumer<ApplicationDsl> app = dsl -> dsl
        .enable(r2dbcConfig)
        .enable(webFluxConfig)
        .listener(ApplicationReadyEvent.class, event -> dsl.ref(UserRepository.class)
                                                           .init());
    webApplication(app).run(args);
  }
}
