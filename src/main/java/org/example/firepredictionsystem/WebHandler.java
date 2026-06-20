package org.example.firepredictionsystem;

import jakarta.servlet.http.HttpSession;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Map;

@Controller
public class WebHandler {
    public final RestClient client = RestClient.create();
    public final String api = "https://appeears.earthdatacloud.nasa.gov/api/";

    // retrieve env variables through application.properties
    @Value("${api.usr}") private String API_USR;
    @Value("${api.psw}") private String API_PSW;
    @Value("${api.url}") private String API_URL;

    @GetMapping("/")
	public String index(HttpSession session) {
        /* display initial web page at root route */
        session.invalidate(); // clear session variables to allow new values to be assigned
		return "index";
	}

    @GetMapping("/task_form")
    public String task_form() {
        /* display point extraction form */
        return "task_form";
    }

    @GetMapping("/success_page")
    public String success_page() {
        /* display success html page*/
        return "success";
    }

    @GetMapping("/fail_page")
    public String fail_page() {
        /* display fail html page */
        return "fail";
    }

    @PostMapping("/request_api_data")
    @ResponseBody
    public void request_api_data(@RequestBody Map<String, String> user, HttpSession session) {
        /* initialize connection with AppEEARS and retrieve usable access token.
        * save required information in session variables to handle user concurrency */

        session.setAttribute("username", user.get("username"));
        session.setAttribute("password", user.get("password"));
        session.setAttribute("email", user.get("email"));

        try{
            session.setAttribute("token", client.post()
                    .uri(String.format("%slogin", api))
                    .headers(httpHeaders ->
                            httpHeaders.setBasicAuth(
                                    session.getAttribute("username").toString(),
                                    session.getAttribute("password").toString()
                            )) // convert to base64 and send
                    .retrieve()
                    .body(String.class));
        } catch (HttpClientErrorException e) {
            session.setAttribute("token", "unauthorized");
            return;
        }
        session.removeAttribute("password"); // immediately remove password from session storage
    }

    @GetMapping("/get_token")
    @ResponseBody
    public Object get_token(HttpSession session) {
        /* send access token (JSON object) to the front end when required */
        if (session.getAttribute("token").toString().equals("unauthorized")) {
            return Map.of("token", "unauthorized");
        } else {
            return session.getAttribute("token");
        }
    }

    @PostMapping("/prepare_task")
    @ResponseBody
    public Map<String, String> prepare_task(@RequestBody Map<String, String> request, HttpSession session) throws ParseException {
        /* format JSON request into applicable format and sent to AppEEARs for requesting for data */
        // initialize variables
        String taskReply;

        session.setAttribute("taskName", request.get("taskName"));
        session.setAttribute("latitude", request.get("latitude"));
        session.setAttribute("longitude", request.get("longitude"));
        session.setAttribute("endDate",  request.get("date"));

        // subtract 3 years inclusive of endDate, from specified date for yearly data retrieval.
        // reformat the positions of values as required
        int[] splitDate = {
                Integer.parseInt(session.getAttribute("endDate").toString().split("-")[0]),
                Integer.parseInt(session.getAttribute("endDate").toString().split("-")[1]),
                Integer.parseInt(session.getAttribute("endDate").toString().split("-")[2])
        };
        LocalDate date = LocalDate.of(splitDate[2], splitDate[0], splitDate[1]);
        date = date.minusYears(3); // subtract 3 years

        session.setAttribute("startDate", String.format("%s-%s-%s",
                String.format("%02d", date.getMonthValue()), String.format("%02d", date.getDayOfMonth()), date.getYear()));

        // handle transmission of point request and send to appeears
        taskReply = new TaskHandler(client).submit_task(session, api);

        if (taskReply.isEmpty()){
            return Map.of("status","fail"); // if sending request failed, send json reply as "fail"
        }

        Object taskReplyObj = new JSONParser().parse(taskReply);
        JSONObject tokenReplyJson = (JSONObject) taskReplyObj;
        String taskStatus = tokenReplyJson.get("status").toString(); // get status of the sent point request
        String taskId = tokenReplyJson.get("task_id").toString(); // get task id of the submitted point request

        if (taskStatus.equals("pending") || taskStatus.equals("processing") || taskStatus.equals("done") || taskStatus.equals("queued")) {
            // handle transmission of data per user to python prediction server

            String requestReply = new PredictionRequestHandler(client).submit_request(
                    session,
                    session.getAttribute("token").toString(),
                    taskId,
                    session.getAttribute("email").toString(),
                    API_USR, API_PSW, API_URL
            );

            if (requestReply.isEmpty()){
                return Map.of("status","pred_fail"); // fail flag for prediction
            } else {
                Object requestReplyObj = new JSONParser().parse(requestReply);
                JSONObject requestReplyJson = (JSONObject) requestReplyObj;

                if (requestReplyJson.get("status").toString().equals("ok")) { // check if predictions erver returned ok
                    return Map.of("status","success"); // send success of the request was successful
                } else {
                    return Map.of("status","fail"); // else fail
                }
            }
        } else {
            return Map.of("status","fail"); // else fail
        }
    }
}

