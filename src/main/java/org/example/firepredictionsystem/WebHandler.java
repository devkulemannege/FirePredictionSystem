package org.example.firepredictionsystem;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.HashMap;

@Controller
public class WebHandler {
    public final RestClient client = RestClient.create();
    public final String api = "https://appeears.earthdatacloud.nasa.gov/api/";
    private String accessToken;

	@GetMapping("/")
	public String index() {
        /* display initial web page at root route */
		return "index";
	}

    @GetMapping("/task_form")
    public String task_form() {
        /* display point extraction form */
        return "task_form";
    }

    @PostMapping("/request_api_data")
    @ResponseBody
    public void request_api_data(@RequestBody HashMap<String, String> user) {
        /* initialize connection with AppEEARS and retrieve usable access token */
//        this.accessToken = client.post()
//                .uri(String.format("%slogin", api))
//                .headers(httpHeaders ->
//                        httpHeaders.setBasicAuth(user.get("username"), user.get("password"))) // convert to base64 and send
//                .retrieve()
//                .body(String.class);
        this.accessToken = "helo"; // TODO: debug. remove later
        System.out.println("access token: " + this.accessToken); // TODO: debug. remove later

        HashMap<String, String> reply = new HashMap<>();
        reply.put("response", "successful"); // return access token to the front end
    }

    @GetMapping("/get_token")
    @ResponseBody
    public HashMap<String, String> get_token() {
        /* send access token to the front end when required */
        HashMap<String, String> token = new HashMap<>();
        token.put("accessToken", this.accessToken);
        return token;
    }

    @PostMapping("/submit_appeears_task")
    @ResponseBody
    public void submit_appeears_task(@RequestBody HashMap<String, String> request) {
        /* format JSON request into applicable format and sent to AppEEARs for requesting for data */
        // TODO: complete method
    }
}

