package daggerok.oauth2client;

import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
class PrincipalDetails {
  private String name;
}

@Log4j2
@Configuration
class OAuth2RestTemplateConfig {

  @Bean
  ClientHttpRequestInterceptor clientHttpRequestInterceptor(OAuth2AuthorizedClientService auth2ClientService) {
    return (request, body, execution) -> {
      var token = Optional.ofNullable(SecurityContextHolder.getContext())
                          .map(SecurityContext::getAuthentication)
                          .map(OAuth2AuthenticationToken.class::cast  )
                          .orElseThrow(() -> new RuntimeException("Auth fail!"));
      var client = auth2ClientService.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(),
                                                           token.getName());
      log.info("client token: {}", client.getAccessToken());
      request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + client.getAccessToken().getTokenValue());
      return execution.execute(request, body);
    };
  }

  @Bean
  RestTemplate restTemplate(RestTemplateBuilder builder,
                            ClientHttpRequestInterceptor clientHttpRequestInterceptor) {

    return builder.interceptors(clientHttpRequestInterceptor)
                  .build();
  }
}

@RestController
@RequiredArgsConstructor
class ClientResource {

  private final RestTemplate restTemplate;
  private final OAuth2AuthorizedClientService oauth2clientService;

  @GetMapping("/")
  PrincipalDetails getPrincipalDetails(OAuth2AuthenticationToken token) {
    var client = oauth2clientService.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getName());
    var uri = client.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUri();
    return restTemplate.exchange(uri, HttpMethod.GET, null, PrincipalDetails.class)
                       .getBody();
  }
}

@SpringBootApplication
public class Oauth2ClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(Oauth2ClientApplication.class, args);
  }
}
