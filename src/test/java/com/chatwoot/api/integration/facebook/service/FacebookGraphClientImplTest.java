package com.chatwoot.api.integration.facebook.service;

import com.chatwoot.api.integration.facebook.config.FacebookProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FacebookGraphClientImplTest {

    FacebookProperties properties;
    MockRestServiceServer server;
    FacebookGraphClientImpl client;

    @BeforeEach
    void setUp() {
        properties = new FacebookProperties("app-id", "app-secret", "verify", "v18.0");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FacebookGraphClientImpl(properties, builder);
    }

    @Test
    void exchangeLongLivedTokenPostsFormAndParsesJson() {
        server.expect(requestTo("https://graph.facebook.com/v18.0/oauth/access_token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("grant_type=fb_exchange_token")))
                .andExpect(content().string(containsString("client_id=app-id")))
                .andExpect(content().string(containsString("client_secret=app-secret")))
                .andExpect(content().string(containsString("fb_exchange_token=short-token")))
                .andRespond(withSuccess("{\"access_token\":\"long-lived\"}", MediaType.APPLICATION_JSON));

        assertThat(client.exchangeLongLivedToken("short-token")).isEqualTo("long-lived");
        server.verify();
    }

    @Test
    void exchangeLongLivedTokenParsesQueryStringBody() {
        server.expect(requestTo("https://graph.facebook.com/v18.0/oauth/access_token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("access_token=long-lived&expires=5184000", MediaType.TEXT_PLAIN));

        assertThat(client.exchangeLongLivedToken("short-token")).isEqualTo("long-lived");
        server.verify();
    }

    @Test
    void listPagesParsesJsonWhenFacebookReturnsTextJavascript() {
        MediaType facebookJson = MediaType.parseMediaType("text/javascript;charset=UTF-8");
        server.expect(requestTo("https://graph.facebook.com/v18.0/me/accounts?access_token=user-token"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"data\":[{\"id\":\"page-1\",\"name\":\"Shop\",\"access_token\":\"page-token\"}]}",
                        facebookJson));

        assertThat(client.listPages("user-token")).containsExactly(
                new FacebookGraphClient.FacebookAccountPage("page-1", "Shop", "page-token"));
        server.verify();
    }
}
