package org.example.firepredictionsystem;

import jakarta.servlet.http.HttpSession;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.HashMap;

@Controller
public class WebHandler {
    public final RestClient client = RestClient.create();
    public final String api = "https://appeears.earthdatacloud.nasa.gov/api/";

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
    public void request_api_data(@RequestBody HashMap<String, String> user, HttpSession session) {
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
            HashMap<String, String> reply = new HashMap<>();
            reply.put("token","unauthorized");
            return reply;
        } else {
            return session.getAttribute("token");
        }
    }

    @PostMapping("/prepare_task")
    @ResponseBody
    public HashMap<String, String> prepare_task(@RequestBody HashMap<String, String> request, HttpSession session) throws ParseException {
        /* format JSON request into applicable format and sent to AppEEARs for requesting for data */
        // initialize variables
        HashMap<String, String> reply = new HashMap<>();
        String taskReply;

        session.setAttribute("taskName", request.get("taskName"));
        session.setAttribute("latitude", request.get("latitude"));
        session.setAttribute("longitude", request.get("longitude"));
        session.setAttribute("endDate",  request.get("date"));

        // subtract 8 days inclusive of endDate, from specified date for multi-date data retrieval.
        // reformat the positions of values as required
        int[] splitDate = {
                Integer.parseInt(session.getAttribute("endDate").toString().split("-")[0]),
                Integer.parseInt(session.getAttribute("endDate").toString().split("-")[1]),
                Integer.parseInt(session.getAttribute("endDate").toString().split("-")[2])
        };
        LocalDate date = LocalDate.of(splitDate[2], splitDate[0], splitDate[1]);

        date = date.minusDays(7); // subtract 8 days (inclusive)
        session.setAttribute("startDate", String.format("%s-%s-%s",
                String.format("%02d", date.getMonthValue()), String.format("%02d", date.getDayOfMonth()), date.getYear()));


        taskReply = new TaskHandler(client).submit_task(session, api);

        if (taskReply.isEmpty()){
            reply.put("status", "fail"); // if sending request failed, send json reply as "fail"
            return reply;
        }

        Object taskReplyObj = new JSONParser().parse(taskReply);
        JSONObject tokenReplyJson = (JSONObject) taskReplyObj;
        String taskStatus = tokenReplyJson.get("status").toString(); // get status of the sent point request

        if (taskStatus.equals("pending") || taskStatus.equals("processing") || taskStatus.equals("done") || taskStatus.equals("queued")) {
            reply.put("status", "success"); // send success of the request was successful
        } else {
            reply.put("status", "fail"); // else fail
        }
        return reply;
    }
}

