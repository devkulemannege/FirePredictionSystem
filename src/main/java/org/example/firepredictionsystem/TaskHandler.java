package org.example.firepredictionsystem;

import jakarta.servlet.http.HttpSession;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

public class TaskHandler {
    private final RestClient client;

    TaskHandler(RestClient client) {
        this.client = client;
    }

    public String submit_task(HttpSession session, String api) throws ParseException {
        // ---- dummy return for testing. TODO: remove for production ----
//        return """
//            {"status": "pending", "task_id": "dummy-id-123"}
//            """;
        // -------------------------------------------------------------


        /* use prepared session variables to construct json payload
         * send POST request to AppEEARS */
        String taskReply;

        // format and prepare JSON payload
        String taskPayload = String.format("""
            {"params": {"coordinates": [{"latitude": "%s",
                        "longitude": "%s"}],
                "dates": [{"endDate": "%s", "startDate": "%s"}],
                "layers": [{"layer": "LST_Day_1km", "product": "MOD11A2.061"},
            {"layer": "_1_km_16_days_NDVI", "product": "MOD13A2.061"},
            {"layer": "sur_refl_b05", "product": "MOD09A1.061"}]},
                "task_name": "%s",
                "task_type": "point"}
            """,session.getAttribute("latitude"),
                session.getAttribute("longitude"),
                session.getAttribute("endDate"),
                session.getAttribute("startDate"),
                session.getAttribute("taskName"));

        Object tokenObj = new JSONParser().parse(session.getAttribute("token").toString());
        JSONObject tokenInJson = (JSONObject) tokenObj; // convert String to json objects

        try {
            taskReply = client.post()
                    .uri(String.format("%stask", api))
                    .headers(httpHeaders -> {
                        httpHeaders.setBearerAuth(tokenInJson.get("token").toString()); // use auth-token intsead of usr & psw
                        httpHeaders.set("content-type", "application/json");
                    })
                    .body(taskPayload)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException e) {
            System.out.printf("%s | %s%n", session.getAttribute("username").toString(), e.getMessage());
            session.invalidate(); // clear session variables to allow new values to be assigned
            taskReply = "";
        }

        return taskReply;
    }
}
