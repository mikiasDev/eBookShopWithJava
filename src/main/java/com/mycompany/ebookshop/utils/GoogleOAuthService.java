package com.mycompany.ebookshop.utils;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;


import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings({"resource", "try"})
public class GoogleOAuthService {
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String CLIENT_ID_PROPERTY = "google.oauth.clientId";
    private static final String CLIENT_SECRET_PROPERTY = "google.oauth.clientSecret";
    private static final String CLIENT_ID_ENV = "GOOGLE_OAUTH_CLIENT_ID";
    private static final String CLIENT_SECRET_ENV = "GOOGLE_OAUTH_CLIENT_SECRET";
    

    public GoogleUserProfile authenticate() throws Exception {
        String clientId = readConfig(CLIENT_ID_PROPERTY, CLIENT_ID_ENV, null);
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("Google sign-up is not configured. Set GOOGLE_OAUTH_CLIENT_ID before using this option.");
        }

        String clientSecret = readConfig(CLIENT_SECRET_PROPERTY, CLIENT_SECRET_ENV, null);
        String state = generateRandomToken(24);
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = createCodeChallenge(codeVerifier);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CompletableFuture<OAuthCallback> callbackFuture = new CompletableFuture<>();
        server.createContext("/google-oauth2-callback", new OAuthCallbackHandler(state, callbackFuture));
        server.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "google-oauth-callback");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();

        int port = server.getAddress().getPort();
        String redirectUri = "http://127.0.0.1:" + port + "/google-oauth2-callback";
        String authUrl = buildAuthUrl(clientId, redirectUri, state, codeChallenge);

        try {
            
            try {
                openEmbeddedBrowser(authUrl, callbackFuture);
            } catch (Throwable t) {
                
                openBrowser(authUrl);
            }

            OAuthCallback callback = callbackFuture.get(2, TimeUnit.MINUTES);
            if (callback.errorMessage != null) {
                throw new IllegalStateException(callback.errorMessage);
            }

            String accessToken = exchangeCodeForToken(clientId, clientSecret, redirectUri, codeVerifier, callback.code);
            return fetchUserProfile(accessToken);
        } finally {
            server.stop(0);
        }
    }

    private void openEmbeddedBrowser(String authUrl, CompletableFuture<OAuthCallback> callbackFuture) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Stage> stageRef = new AtomicReference<>();

        
        Platform.runLater(() -> {
            try {
                
                try {
                    Class<?> webViewClass = Class.forName("javafx.scene.web.WebView");
                    Object webView = webViewClass.getConstructor().newInstance();
                    Class<?> webEngineClass = Class.forName("javafx.scene.web.WebEngine");

                    Stage stage = new Stage();
                    stage.initModality(Modality.APPLICATION_MODAL);
                    stage.setTitle("Sign in with Google");
                    stage.setScene(new Scene((Parent) webView, 960, 700));
                    stageRef.set(stage);

                    
                    callbackFuture.whenComplete((cb, ex) -> Platform.runLater(() -> {
                        Stage s = stageRef.get();
                        if (s != null) s.close();
                    }));

                    stage.setOnHidden(e -> latch.countDown());

                    java.lang.reflect.Method getEngine = webViewClass.getMethod("getEngine");
                    Object engine = getEngine.invoke(webView);
                    java.lang.reflect.Method loadMethod = webEngineClass.getMethod("load", String.class);
                    loadMethod.invoke(engine, authUrl);
                    stage.show();
                } catch (ClassNotFoundException cnf) {
                    
                    latch.countDown();
                }
            } catch (Throwable e) {
                
                latch.countDown();
            }
        });

        
        
        latch.await(2, TimeUnit.MINUTES);
    }

    private String buildAuthUrl(String clientId, String redirectUri, String state, String codeChallenge) {
        return AUTH_ENDPOINT
            + "?client_id=" + urlEncode(clientId)
            + "&redirect_uri=" + urlEncode(redirectUri)
            + "&response_type=code"
            + "&scope=" + urlEncode("openid email profile")
            + "&state=" + urlEncode(state)
            + "&code_challenge=" + urlEncode(codeChallenge)
            + "&code_challenge_method=S256"
            + "&access_type=offline"
            + "&prompt=select_account";
    }

    private String exchangeCodeForToken(String clientId, String clientSecret, String redirectUri, String codeVerifier, String code) throws IOException, InterruptedException {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.put("client_secret", clientSecret);
        }
        form.put("code", code);
        form.put("code_verifier", codeVerifier);
        form.put("grant_type", "authorization_code");
        form.put("redirect_uri", redirectUri);

        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(buildFormBody(form)))
            .build();

        HttpResponse<String> response = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build()
            .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Google token exchange failed: " + response.body());
        }

        String accessToken = extractJsonString(response.body(), "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Google token response did not include an access token.");
        }
        return accessToken;
    }

    private GoogleUserProfile fetchUserProfile(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(USERINFO_ENDPOINT))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + accessToken)
            .build();

        HttpResponse<String> response = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build()
            .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Google profile lookup failed: " + response.body());
        }

        String name = extractJsonString(response.body(), "name");
        String email = extractJsonString(response.body(), "email");
        boolean emailVerified = extractJsonBoolean(response.body(), "email_verified");

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Google account did not return an email address.");
        }

        String subject = extractJsonString(response.body(), "sub");
        return new GoogleUserProfile(name, email, subject, emailVerified);
    }

    private void openBrowser(String authUrl) throws IOException {
        URI uri = URI.create(authUrl);
        if (!Desktop.isDesktopSupported()) {
            throw new IllegalStateException("This computer does not support opening a browser for Google sign-up.");
        }
        Desktop.getDesktop().browse(uri);
    }

    private String buildFormBody(Map<String, String> form) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
        }
        return builder.toString();
    }

    private String readConfig(String propertyName, String envName, String defaultValue) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(envName);
        }

        
        
        
        
        if (value == null || value.isBlank()) {
            java.util.List<java.nio.file.Path> candidates = new java.util.ArrayList<>();
            candidates.add(java.nio.file.Paths.get(System.getProperty("user.home"), ".ebookshop", "oauth.properties"));
            candidates.add(java.nio.file.Paths.get(System.getProperty("user.dir"), ".ebookshop", "oauth.properties"));
            candidates.add(java.nio.file.Paths.get(System.getProperty("user.dir"), "oauth.properties"));

            for (java.nio.file.Path cfg : candidates) {
                try {
                    if (java.nio.file.Files.exists(cfg)) {
                        try (java.io.InputStream in = java.nio.file.Files.newInputStream(cfg)) {
                            java.util.Properties props = new java.util.Properties();
                            props.load(in);
                            
                            String fromFile = props.getProperty(envName);
                            if (fromFile == null) {
                                
                                fromFile = props.getProperty(envName.toLowerCase());
                            }
                            if (fromFile != null && !fromFile.isBlank()) {
                                value = fromFile.trim();
                                break;
                            }
                        }
                    }
                } catch (java.io.IOException e) {
                    
                }
            }
        }

        if (value == null || value.isBlank()) {
            value = defaultValue;
        }

        if ((value == null || value.isBlank()) && defaultValue != null && !defaultValue.isBlank()) {
            value = defaultValue;
        }
        return value;
    }

    private String generateRandomToken(int byteCount) {
        byte[] bytes = new byte[byteCount];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeVerifier() {
        return generateRandomToken(32);
    }

    private String createCodeChallenge(String verifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
    }

    private String extractJsonString(String json, String fieldName) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\\"" + java.util.regex.Pattern.quote(fieldName) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJson(matcher.group(1));
    }

    private boolean extractJsonBoolean(String json, String fieldName) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\\"" + java.util.regex.Pattern.quote(fieldName) + "\\\"\\s*:\\s*(true|false)").matcher(json);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private String unescapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\\' && index + 1 < value.length()) {
                char next = value.charAt(++index);
                switch (next) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> {
                        if (index + 4 < value.length()) {
                            String hex = value.substring(index + 1, index + 5);
                            builder.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                    }
                    default -> builder.append(next);
                }
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static final class OAuthCallback {
        private final String code;
        private final String errorMessage;

        private OAuthCallback(String code, String errorMessage) {
            this.code = code;
            this.errorMessage = errorMessage;
        }
    }

    private static final class OAuthCallbackHandler implements HttpHandler {
        private final String expectedState;
        private final CompletableFuture<OAuthCallback> callbackFuture;

        private OAuthCallbackHandler(String expectedState, CompletableFuture<OAuthCallback> callbackFuture) {
            this.expectedState = expectedState;
            this.callbackFuture = callbackFuture;
        }

        @Override
        @SuppressWarnings("TryWithResources")
        public void handle(HttpExchange exchange) throws IOException {
            String response = "<html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1'></head>"
                + "<body style='font-family:sans-serif;padding:24px;'>"
                + "Google sign-in complete. Returning to eBookShop..."
                + "<script>"
                + "function closeNow(){"
                + "  try { window.open('', '_self'); } catch (e) {}"
                + "  try { window.close(); } catch (e) {}"
                + "  try { location.replace('about:blank'); } catch (e) {}"
                + "}"
                + "setTimeout(closeNow, 150);"
                + "setTimeout(closeNow, 700);"
                + "</script>"
                + "</body></html>";
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            String error = params.get("error");
            String state = params.get("state");
            String code = params.get("code");

            if (error != null && !error.isBlank()) {
                callbackFuture.complete(new OAuthCallback(null, "Google sign-in was canceled or denied."));
            } else if (code == null || code.isBlank()) {
                callbackFuture.complete(new OAuthCallback(null, "Google did not return an authorization code."));
            } else if (state == null || !state.equals(expectedState)) {
                callbackFuture.complete(new OAuthCallback(null, "Google sign-in state validation failed."));
            } else {
                callbackFuture.complete(new OAuthCallback(code, null));
            }

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> params = new LinkedHashMap<>();
            if (query == null || query.isBlank()) {
                return params;
            }

            for (String part : query.split("&")) {
                int separatorIndex = part.indexOf('=');
                if (separatorIndex > 0) {
                    String key = java.net.URLDecoder.decode(part.substring(0, separatorIndex), StandardCharsets.UTF_8);
                    String value = java.net.URLDecoder.decode(part.substring(separatorIndex + 1), StandardCharsets.UTF_8);
                    params.put(key, value);
                }
            }
            return params;
        }
    }
}