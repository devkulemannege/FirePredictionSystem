package org.example.firepredictionsystem;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Controller
public class WebHandler {
    public final String url = "https://appeears.earthdatacloud.nasa.gov/api/";
    private final RestClient client = RestClient.create(); // initialize RestClient
    private String access_token;

    @GetMapping("/")
    public String index() {
        return "index"; // load html page
    }

    @PostMapping("/request_api_data")
    @ResponseBody
    public Map<String, String> request_api_data(@RequestBody Map<String, String> user) { // get variables to Map from provided json reply

        // access AppEEARS and receive access token for tasks requests
        this.access_token = client.post()
                .uri(String.format("%slogin", url))
                .headers(httpHeaders -> {
                    httpHeaders.setBasicAuth(user.get("username"), user.get("password"));
                })
                .retrieve() // retrieve response from API
                .body(String.class); // receive in String

        System.out.println(this.access_token);
        return Map.of("reply","done."); // auto conversion to json through jackson package
    }
}
