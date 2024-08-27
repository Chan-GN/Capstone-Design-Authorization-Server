package com.hansung.hansungauthorizationserver.config;

import com.hansung.hansungauthorizationserver.service.OidcUserInfoService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.authentication.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CORSCustomizer corsCustomizer;

    @Bean
    @Order(1) // 설정 1순위
    public SecurityFilterChain asSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http); // Authorization Server 기본 설정 적용

        http
                .getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .authorizationEndpoint(a -> // localhost 환경에서 동작 허용
                        a.authenticationProviders(configureAuthenticationValidator())
                )
                .oidc(Customizer.withDefaults()); // OpenID Connect

        http
                .oauth2ResourceServer((resourceServer) -> resourceServer.jwt(Customizer.withDefaults())) // userinfo 엔드포인트를 위해
                .exceptionHandling((exceptions) -> exceptions // 에러 발생 시, Login 페이지로 이동
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        corsCustomizer.corsCustomizer(http); // CORS Configuration 적용

        return http.build();
    }

    @Bean
    @Order(2) // 설정 2순위
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
        corsCustomizer.corsCustomizer(http);

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/profile_image/**").permitAll() // 프로필 이미지 접근 허용
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                );


        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                // .issuer("https://example.com") // issuer Customize possible
                .build();
    }

    // key pair 여러개 정의해서 Rotation 하는 것이 좋음
    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        KeyPair keyPair = generateRsaKey();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);

        return new ImmutableJWKSet<>(jwkSet);
    }

    private KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> oAuth2TokenCustomizer(OidcUserInfoService userInfoService) {
        return context -> {
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) { // ID Token Customize
                OidcUserInfo userInfo = userInfoService.loadUser(
                        context.getPrincipal().getName());
                context.getClaims().claims(claims ->
                        claims.putAll(userInfo.getClaims()));
            }

            Collection<? extends GrantedAuthority> authorities = context.getPrincipal().getAuthorities();

            context.getClaims().claim("authorities",
                    authorities.stream().map(GrantedAuthority::getAuthority).toList());
        };
    }

    private Consumer<List<AuthenticationProvider>> configureAuthenticationValidator() {
        return (authenticationProviders) ->
                authenticationProviders.forEach((authenticationProvider) -> {
                    if (authenticationProvider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider) {
                        Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> authenticationValidator =
                                // Override default redirect_uri validator
                                new CustomRedirectUriValidator()
                                        // Reuse default scope validator
                                        .andThen(OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR);

                        ((OAuth2AuthorizationCodeRequestAuthenticationProvider) authenticationProvider)
                                .setAuthenticationValidator(authenticationValidator);
                    }
                });
    }

    static class CustomRedirectUriValidator implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

        @Override
        public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext authenticationContext) {
            OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication =
                    authenticationContext.getAuthentication();
            RegisteredClient registeredClient = authenticationContext.getRegisteredClient();
            String requestedRedirectUri = authorizationCodeRequestAuthentication.getRedirectUri();

            // Use exact string matching when comparing client redirect URIs against pre-registered URIs
            if (!registeredClient.getRedirectUris().contains(requestedRedirectUri)) {
                OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);
                throw new OAuth2AuthorizationCodeRequestAuthenticationException(error, null);
            }
        }

    }

}
