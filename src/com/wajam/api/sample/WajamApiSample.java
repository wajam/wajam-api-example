package com.wajam.api.sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class WajamApiSample {

	private static final String API_HOST = "https://api.wajam.com/";
	private static final String CALLBACK_PATH = "/callback";
	private static final String API_KEY = "REPLACE_ME";
	private static final String API_SECRET = "REPLACE_ME";

	private static final Pattern VERIFIER_EXTRACTOR = Pattern.compile("oauth_verifier=(\\w*)");

	public static void main(String[] args) throws IOException {
		final OAuthService service = new ServiceBuilder()
				.signatureType(SignatureType.QueryString)
				.provider(WajamApi.Twitter.class)
				//.provider(WajamApi.Facebook.class)
				.callback("http://127.0.0.1:9999"+CALLBACK_PATH)
				.apiKey(API_KEY)
				.apiSecret(API_SECRET).build();

		final Token requestToken = service.getRequestToken();

		String authUrl = service.getAuthorizationUrl(requestToken);

		System.out.println("Go to this URL in your browser to authenticate: " + authUrl);
		

		HttpServer server = HttpServer.create(new InetSocketAddress(9999), 1);
		server.createContext(CALLBACK_PATH).setHandler(new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {

				Matcher m = VERIFIER_EXTRACTOR.matcher(exchange.getRequestURI().getQuery());

				if (m.find()) {
					Verifier verifier = new Verifier(m.group(1));
					final Token accessToken = service.getAccessToken(requestToken, verifier);

					new Thread(new Runnable() {

						@Override
						public void run() {
							BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
							while (true) {
								System.out.println("\n\nWhich API call do you want to make (e.g. /v1/search?q=wajam&type=links)");
								String apiCall;
								try {
									apiCall = br.readLine();
									String fullURL = API_HOST + apiCall;
									OAuthRequest request = new OAuthRequest(Verb.GET, fullURL);
									System.out.println("Sending request: " + fullURL);
									service.signRequest(accessToken, request);
									Response response = request.send();
									System.out.println("\nReceived response code: " + response.getCode());
									System.out.println(response.getBody());
								} catch (IOException e) {
									System.out.println("Error in the main loop: " + e);
								}
								
							}
						}
						
					}).start();
					
					byte[] message = "Go back to the WajamApiSample application prompt to explore Wajam API!".getBytes("ascii");
					exchange.sendResponseHeaders(200, message.length);
					exchange.getResponseBody().write(message);
					exchange.getResponseBody().close();
					
				} else {
					System.out.println("No verifier in request: " + exchange.getRequestURI());
					byte[] message = "No verifier in callback request, try again.".getBytes("ascii");
					exchange.sendResponseHeaders(500, message.length);
					exchange.getResponseBody().write(message);
					exchange.getResponseBody().close();
				}
			}
		});

		server.start();
	}

	private static abstract class WajamApi extends DefaultApi10a {
		
		private static String AUTHORIZE_URL_FORMAT = "/v1/oauth/authorize?network=%s&oauth_token=%s";

		@Override
		public String getAccessTokenEndpoint() {
			return API_HOST + "/v1/oauth/access_token";
		}

		@Override
		public String getRequestTokenEndpoint() {
			return API_HOST + "/v1/oauth/request_token";
		}

		public static class Twitter extends WajamApi {

			@Override
			public String getAuthorizationUrl(Token requestToken) {
				return String.format(API_HOST + AUTHORIZE_URL_FORMAT, "twitter", requestToken.getToken());
			}
		}

		public static class Facebook extends WajamApi {

			@Override
			public String getAuthorizationUrl(Token requestToken) {
				return String.format(API_HOST + AUTHORIZE_URL_FORMAT, "facebook", requestToken.getToken());
			}
		}
	}
}
