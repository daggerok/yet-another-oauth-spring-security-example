package daggerok.authserver;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
class ApplicationPasswordEncoderConfig {

  @Bean
  PasswordEncoder applicationPasswordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}

/**
 * Configure web security with authentication manager first: @Order(Ordered.HIGHEST_PRECEDENCE)
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApplicationWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // @formatter:off
    http.authorizeRequests()
          .anyRequest()
            .authenticated()
            .and()
        .csrf()
          .disable()
        .formLogin();
    // @formatter:on
  }

  @SneakyThrows
  @Bean//(name = "applicationAuthenticationManagerBean")
  AuthenticationManager applicationAuthenticationManagerBean() {
    return super.authenticationManagerBean();
  }

  @Bean
  Map<String, UserDetails> users(PasswordEncoder applicationPasswordEncoder) {
    return Stream.of("max", "josh", "rob", "joe")
                 .collect(Collectors.toMap(Function.identity(), username ->
                     // new User(username,
                     //          applicationPasswordEncoder.encode("pas"),
                     //          true, // enabled
                     //          true, // accountNonExpired
                     //          true, // credentialsNonExpired
                     //          true, // accountNonLocked
                     //          AuthorityUtils.createAuthorityList("USER"))
                     // // User.builder()
                     // //     .username(username)
                     // //     .password(applicationPasswordEncoder.encode("pas"))
                     // //     .disabled(false)
                     // //     .accountExpired(false)
                     // //     .credentialsExpired(false)
                     // //     .accountLocked(false)
                     // //     .authorities(AuthorityUtils.createAuthorityList("USER"))
                     // //     .build()
                     User.withUsername(username)
                         .password(applicationPasswordEncoder.encode("pas"))
                         .disabled(false)
                         .accountExpired(false)
                         .credentialsExpired(false)
                         .accountLocked(false)
                         .authorities(AuthorityUtils.createAuthorityList("USER"))
                         .build()
                 ));
  }
}

/**
 * Custom user details service (should be re-written for prod to use some cassandra as data-store...)
 */
@Service
@RequiredArgsConstructor
class ApplicationUserDetailsService implements UserDetailsService {

  private final Map<String, UserDetails> users;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return Optional.ofNullable(users.get(username))
                   .orElseThrow(() -> new UsernameNotFoundException(String.format("User %s was not found.", username)));
  }
}

/**
 * Configure resources security (see scope: profile)
 */
@Configuration
@EnableResourceServer
class ApplicationResourceServerConfigurerAdapter extends ResourceServerConfigurerAdapter {

  @Override
  public void configure(HttpSecurity http) throws Exception {
    // @formatter:off
    http.csrf()
          .disable()
        .antMatcher("/resources/**")
          .authorizeRequests()
        .mvcMatchers("/resources/user-info")
          .access("#oauth2.hasScope('profile')")
    ;
    // @formatter:on
  }
}

/**
 * Configure JWT auth. server. Requires custom auth. manager and generated .oauth2.keystore file (see README.md)
 */
@Configuration
@RequiredArgsConstructor
@EnableAuthorizationServer
class ApplicationAuthorizationServerConfigurerAdapter extends AuthorizationServerConfigurerAdapter {

  @Qualifier//("applicationAuthenticationManagerBean")
  private final AuthenticationManager applicationAuthenticationManagerBean;

  @Override
  public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
    // @formatter:off
    clients.inMemory()
             .withClient("application-client")
               .secret("application-client-secret")
               .authorizedGrantTypes("authorization_code")
               .scopes("profile")
               .redirectUris("http://127.0.0.1:8080/login/oauth2/code/application-client");
    // @formatter:on
  }

  @Override
  public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
    endpoints.tokenStore(tokenStore())
             .accessTokenConverter(jwtAccessTokenConverter())
             .authenticationManager(applicationAuthenticationManagerBean);
  }

  @Bean
  TokenStore tokenStore() {
    return new JwtTokenStore(jwtAccessTokenConverter());
  }

  @Bean
  JwtAccessTokenConverter jwtAccessTokenConverter() {
    var keystoreResource = new ClassPathResource(".oauth2.keystore");
    var keystorePassword = ".oauth2.keystore.password".toCharArray();
    var factory = new KeyStoreKeyFactory(keystoreResource, keystorePassword);
    var jwtAccessTokenConverter = new JwtAccessTokenConverter();
    jwtAccessTokenConverter.setKeyPair(factory.getKeyPair("oauth2-app"));
    return jwtAccessTokenConverter;
  }
}

@Controller
class ApplicationIndexPage {

  /*
   * http -a user:uuid :8000
   */
  @GetMapping
  String index() {
    return "index";
  }
}

@RestController
class ApplicationRestResource {

  /*
   * http -a user:uuid :8000
   */
  @GetMapping("/resources/user-info")
  Map<String, String> getUserInfo(Authentication auth) {
    return Collections.singletonMap("name", auth.getName());
  }
}

@SpringBootApplication
public class AuthServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuthServerApplication.class, args);
  }
}
