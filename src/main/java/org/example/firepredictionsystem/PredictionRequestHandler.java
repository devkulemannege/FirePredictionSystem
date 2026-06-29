package org.example.firepredictionsystem;

import jakarta.servlet.http.HttpSession;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class PredictionRequestHandler {
    private final RestClient client;

    public PredictionRequestHandler(RestClient client) {
        this.client = client;
    }

    public String submit_request(
            HttpSession session, String token, String taskId, String email,
            String API_USR, String API_PSW, String API_URL
    ) throws ParseException {
        /* handle transmission of data per user to python prediction server */
        String request_reply;

        // preparation of json payload
        Object tokenObj = new JSONParser().parse(token);
        JSONObject tokenInJson = (JSONObject) tokenObj; // convert String to json objects

        Map<String, Object> payload = Map.of(
                "token", tokenInJson.get("token").toString(),
                "taskId", taskId,
                "email", email
        );

        try {
            request_reply = client.post()
                    .uri(String.format("%s/transfer", API_URL))
                    .headers(httpHeaders -> {
                        httpHeaders.setBasicAuth(API_USR, API_PSW);
                        httpHeaders.set("content-type", "application/json");
                    })
                    .body(payload)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.TooManyRequests e){
            // if a 429 error is returned from server, return json with appropriate status
            request_reply = """
                {
                  "status": "too_many_requests",
                }
                """;
        } catch (Exception e) {
            System.out.printf("%s | %s%n", session.getAttribute("username").toString(), e.getMessage());
            //session.invalidate(); // clear session variables to allow new values to be assigned TODO: uncomment for production
            request_reply = "";
        }

        return request_reply;
    }
}
