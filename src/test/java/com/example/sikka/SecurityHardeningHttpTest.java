package com.example.sikka;

import com.example.sikka.Config.AuthenticationAttemptLimiter;
import com.example.sikka.Service.VerificationService;
import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.Account;
import com.example.sikka.Repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties="spring.datasource.url=jdbc:h2:mem:sikka-security13;MODE=MySQL;DB_CLOSE_DELAY=-1")
class SecurityHardeningHttpTest {
    @LocalServerPort int port;
    @Autowired AccountRepository accounts;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AuthenticationAttemptLimiter limiter;
    @Autowired VerificationService verification;
    private URI uri(String path) { return URI.create("http://localhost:"+port+path); }
    private HttpClient browser() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
    }
    private HttpResponse<String> get(HttpClient client,String path)throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    private String token(HttpClient client)throws Exception {
        HttpResponse<String> response=get(client,"/api/v1/auth/csrf");
        assertEquals(200,response.statusCode());
        String body=response.body(),prefix="\"token\":\"";
        int begin=body.indexOf(prefix);
        assertTrue(begin>=0,"Missing CSRF token response");
        return body.substring(begin+prefix.length(),body.indexOf('"',begin+prefix.length()));
    }
    private HttpResponse<String> login(HttpClient client,String email,String password,String csrf)throws Exception {
        String form="username="+URLEncoder.encode(email,StandardCharsets.UTF_8)
                +"&password="+URLEncoder.encode(password,StandardCharsets.UTF_8);
        HttpRequest.Builder builder=HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type","application/x-www-form-urlencoded");
        if(csrf!=null)builder.header("X-CSRF-TOKEN",csrf);
        return client.send(builder.POST(HttpRequest.BodyPublishers.ofString(form)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private Account account() {
        String email="security13-"+UUID.randomUUID()+"@example.test";
        Account a=new Account();
        a.setName("Security check");a.setEmail(email);a.setPasswordHash(passwordEncoder.encode("StrongPassword#13"));
        a.setRole("USER");a.setStatus("ACTIVE");a.setEmailVerified(true);
        return accounts.save(a);
    }

    @Test void sessionLoginRequiresCsrfTokenAndValidTokenStillWorks()throws Exception {
        Account a=account();HttpClient client=browser();
        assertEquals(403,login(client,a.getEmail(),"StrongPassword#13",null).statusCode());
        assertEquals(200,login(client,a.getEmail(),"StrongPassword#13",token(client)).statusCode());
        assertEquals(200,get(client,"/api/v1/account/me").statusCode());
        HttpRequest without=HttpRequest.newBuilder(uri("/api/v1/auth/logout"))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(403,client.send(without,HttpResponse.BodyHandlers.ofString()).statusCode());
        HttpRequest with=HttpRequest.newBuilder(uri("/api/v1/auth/logout"))
                .header("X-CSRF-TOKEN",token(client)).POST(HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(200,client.send(with,HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test void passwordReplacementInvalidatesOldSession()throws Exception {
        Account a=account();HttpClient client=browser();
        assertEquals(200,login(client,a.getEmail(),"StrongPassword#13",token(client)).statusCode());
        a.setPasswordHash(passwordEncoder.encode("NewStrongPassword#13"));accounts.save(a);
        assertEquals(401,get(client,"/api/v1/account/me").statusCode());
    }

    @Test void suspendingAccountInvalidatesOldSession()throws Exception {
        Account a=account();HttpClient client=browser();
        assertEquals(200,login(client,a.getEmail(),"StrongPassword#13",token(client)).statusCode());
        a.setStatus("SUSPENDED");accounts.save(a);
        assertEquals(401,get(client,"/api/v1/account/me").statusCode());
    }

    @Test void repeatedBadLoginIsTemporarilyBlocked()throws Exception {
        Account a=account();HttpClient client=browser();
        String csrf=token(client);
        for(int i=0;i<5;i++)
            assertEquals(401,login(client,a.getEmail(),"incorrect-password",csrf).statusCode());
        assertEquals(429,login(client,a.getEmail(),"StrongPassword#13",csrf).statusCode());
    }

    @Test void repeatedWrongBasicCredentialsAreTemporarilyBlocked()throws Exception {
        Account a=account();HttpClient client=browser();
        String wrong="Basic "+java.util.Base64.getEncoder().encodeToString(
                (a.getEmail()+":invalid-password").getBytes(StandardCharsets.UTF_8));
        for(int i=0;i<5;i++) {
            HttpRequest req=HttpRequest.newBuilder(uri("/api/v1/account/me"))
                    .header("Authorization",wrong).GET().build();
            assertEquals(401,client.send(req,HttpResponse.BodyHandlers.ofString()).statusCode());
        }
        String valid="Basic "+java.util.Base64.getEncoder().encodeToString(
                (a.getEmail()+":StrongPassword#13").getBytes(StandardCharsets.UTF_8));
        HttpRequest blocked=HttpRequest.newBuilder(uri("/api/v1/account/me"))
                .header("Authorization",valid).GET().build();
        assertEquals(429,client.send(blocked,HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test void repeatedInvalidEmailAndResetCodesAreLimited() {
        Account a=account();
        for(int i=0;i<5;i++)
            assertThrows(ApiException.class,()->verification.verify(a.getEmail(),"000001"));
        assertTrue(assertThrows(ApiException.class,()->verification.verify(a.getEmail(),"000001")).getMessage().contains("too many"));
        for(int i=0;i<5;i++)
            assertThrows(ApiException.class,()->verification.resetPassword(a.getEmail(),"000001","NewStrongPassword#13"));
        assertTrue(assertThrows(ApiException.class,()->verification.resetPassword(a.getEmail(),"000001","NewStrongPassword#13")).getMessage().contains("too many"));
    }

    @Test void failedAttemptsThrottleAndSuccessfulAttemptClearsState() {
        String key="security13-"+UUID.randomUUID()+"@example.test";
        for(int i=0;i<5;i++){assertTrue(limiter.allowed("email-verification",key));limiter.failed("email-verification",key);}
        assertFalse(limiter.allowed("email-verification",key));
        limiter.succeeded("email-verification",key);
        assertTrue(limiter.allowed("email-verification",key));
    }
}
