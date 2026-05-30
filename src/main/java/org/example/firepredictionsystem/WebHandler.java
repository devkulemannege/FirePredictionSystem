package org.example.firepredictionsystem;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Enumeration; // debug package
import java.util.HashMap;

@Controller
public class WebHandler {
    public final RestClient client = RestClient.create();
    public final String api = "https://appeears.earthdatacloud.nasa.gov/api/";

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
    public void request_api_data(@RequestBody HashMap<String, String> user, HttpSession session) {
        /* initialize connection with AppEEARS and retrieve usable access token.
        * save required information in session variables to handle user concurrency */

        session.setAttribute("username", user.get("username"));
        session.setAttribute("password", user.get("password"));

//        try{
//            session.setAttribute("token", client.post()
//                    .uri(String.format("%slogin", api))
//                    .headers(httpHeaders ->
//                            httpHeaders.setBasicAuth(
//                                    session.getAttribute("username").toString(),
//                                    session.getAttribute("password").toString()
//                            )) // convert to base64 and send
//                    .retrieve()
//                    .body(String.class));
//        } catch (HttpClientErrorException e) {
//            System.out.printf("%s | %s%n", session.getAttribute("username").toString(), e.getMessage());
//            session.invalidate(); // clear session variables to allow new values to be assigned
//            return;
//        }
//        session.removeAttribute("password"); // immediately remove password from session storage

        // TODO: debug. remove later
        HashMap<String,Object> map = new HashMap<>();
        map.put("token","abc");
        session.setAttribute("token", map);

        HashMap<String, String> reply = new HashMap<>();
        reply.put("response", "successful"); // return access token to the front end
    }

    @GetMapping("/get_token")
    @ResponseBody
    public Object get_token(HttpSession session) {
        /* send access token (JSON object) to the front end when required */
        return session.getAttribute("token");
    }

    @PostMapping("/submit_task")
    @ResponseBody
    public void submit_task(@RequestBody HashMap<String, String> request, HttpSession session) {
        /* format JSON request into applicable format and sent to AppEEARs for requesting for data */
        session.setAttribute("taskName", request.get("taskName"));
        session.setAttribute("latitude", request.get("latitude"));
        session.setAttribute("longitude", request.get("longitude"));
        session.setAttribute("endDate",  request.get("date"));

        // subtract 8 days inclusive of endDate, from specified date for multi-date data retrieval.
        // reformat the positions of values as required
        LocalDate date = LocalDate.of(
                Integer.parseInt(session.getAttribute("endDate").toString().split("-")[2]),
                Integer.parseInt(session.getAttribute("endDate").toString().split("-")[0]),
                Integer.parseInt(session.getAttribute("endDate").toString().split("-")[1])
        );
        date = date.minusDays(7); // subtract 8 days (inclusive)
        session.setAttribute("startDate", String.format("%s-%s-%s",
                String.format("%02d", date.getMonthValue()), String.format("%02d", date.getDayOfMonth()), date.getYear()));

//        // TODO: debug. remove later
//        Enumeration<String> attributeNames = session.getAttributeNames();
//        while (attributeNames.hasMoreElements()) {
//            String name = attributeNames.nextElement();
//            Object value = session.getAttribute(name);
//
//            System.out.println(name + " = " + value);
//        }

    }
}

