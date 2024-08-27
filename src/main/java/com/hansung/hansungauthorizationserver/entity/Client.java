package com.hansung.hansungauthorizationserver.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_generated_id")
    private Long id;
    private String clientId;
    private String secret;
    private String redirectUri; // 수정 필요, 다대다?
    private String scope; // 수정 필요, 다대다?
    private String authMethod; // 수정 필요, 다대다?

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "grant_type_client",
            joinColumns = {@JoinColumn(name = "client_generated_id")},
            inverseJoinColumns = {@JoinColumn(name = "grant_type")}
    )
    private Set<GrantType> grantTypes;

    @Builder
    public Client(String clientId, String secret, String redirectUri, String scope, String authMethod) {
        this.clientId = clientId;
        this.secret = secret;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.authMethod = authMethod;
    }

    public static Client from(RegisteredClient registeredClient) {
        Client client = new Client();

        Set<GrantType> grantTypeSet = new HashSet<>();

        registeredClient.getAuthorizationGrantTypes().forEach(authorizationGrantType -> {
            String value = authorizationGrantType.getValue();
            GrantType grantType = new GrantType();
            grantType.setGrantType(value);

            grantTypeSet.add(grantType);
        });

        client.setClientId(registeredClient.getClientId());
        client.setSecret(registeredClient.getClientSecret());
        client.setRedirectUri( // Not Clean Code
                registeredClient.getRedirectUris().stream().findAny().get()
        );
        client.setScope( // Not Clean Code
                registeredClient.getScopes().stream().findAny().get()
        );
        client.setAuthMethod( // Not Clean Code
                registeredClient.getClientAuthenticationMethods().stream().findAny().get().getValue()
        );
        client.setGrantTypes(grantTypeSet);

        return client;
    }

    public static RegisteredClient from(Client client) {
        Consumer<Set<AuthorizationGrantType>> grantTypes = authorizationGrantTypes -> {
            client.getGrantTypes().forEach(grantType ->
                    authorizationGrantTypes.add(new AuthorizationGrantType(grantType.getGrantType()))
            );
        };

        return RegisteredClient.withId(String.valueOf(client.getId()))
                .clientId(client.getClientId())
                .clientSecret(client.getSecret())
                .scope(client.getScope())
                .redirectUri(client.getRedirectUri())
                .clientAuthenticationMethod(new ClientAuthenticationMethod(client.getAuthMethod()))
                .authorizationGrantTypes(grantTypes)
                .tokenSettings(TokenSettings
                        .builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .refreshTokenTimeToLive(Duration.ofHours(1))
                        .build())
                .build();
    }

}
