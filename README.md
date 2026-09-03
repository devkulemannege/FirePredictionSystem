# Fire Prediction System

A Spring Boot web application that collects a NASA Earthdata login, submits a geospatial point-extraction task to NASA AppEEARS, and forwards the resulting task metadata to a remote prediction service for wildfire-risk analysis.

[Click here to view the respository for the remote prediction server.](https://github.com/devkulemannege/FirePredictionSystem-prediction-server)

This project is designed for a browser-based workflow where the user:

1. Enters NASA Earthdata credentials and an email address without storing them onto a cache or database.
2. Authenticates to the AppEEARS API using a token.
3. Chooses a task name, latitude, longitude, and date.
4. Submits a point-based remote-sensing request.
5. Receives a success or fail result and a notification email is later sent by the downstream prediction service.

---

## Overview

The application is built with:

- Java 21
- Spring Boot 4.0.6
- Thymeleaf templates for the front-end pages
- REST calls to NASA AppEEARS and a prediction backend
- Session-based state management for the authenticated token and request details

The main backend controller is `WebHandler`, supported by:

- `TaskHandler` for building and submitting the AppEEARS task payload
- `PredictionRequestHandler` for sending task metadata to the prediction service

---

## Project Structure

- `src/main/java/org/example/firepredictionsystem/`  
  Contains the Spring controller and relevant classes.
- `src/main/resources/templates/`  
  HTML pages for the browser UI.
- `src/main/resources/static/`  
  CSS and JavaScript for styling and behaviour

---

## Environment Variables

The app requires environment variables defined in `application.properties` in order to communicate with the prediction server:

```properties
api.usr=${API_USR} // username for the external prediction service
api.psw=${API_PSW} // password for the external prediction service
api.url=${API_URL} // base URL for the prediction backend
```

---

## Local Run Instructions

From the project root:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The app runs on port `5000` as configured in `application.properties`. All development and testing has been done on IntelliJ IDEA 2025.2.2

---

## Application Route Reference

Below is a complete route-by-route description of what the application does.

### GET `/`

Purpose: landing page for the initial user authentication step.

Behavior:

- Invalidates the current HTTP session.
- Returns the `index` view.
- Displays the Earthdata login form with fields for:
  - username
  - password
  - notification email

This is the starting route for the workflow.

---

### GET `/task_form`

Purpose: display the task configuration form after a successful Earthdata login.

Behavior:

- Returns the `task_form` view.
- Shows the AppEEARS access token area.
- Lets the user enter:
  - task name
  - latitude
  - longitude
  - target date

The JavaScript client then calls `/get_token` to fetch the AppEEARS token and displays it in the UI.

---

### GET `/success_page`

Purpose: show a success confirmation page after a valid prediction request is submitted.

Behavior:

- Returns the `success` template.
- Informs the user that the request was accepted and the result will be emailed.

This page is displayed when the backend returns a `success` status from `/prepare_task`.

---

### GET `/fail_page`

Purpose: show an error page when the task submission fails.

Behavior:

- Returns the `fail` template.
- Displays a general failure message explaining that the request could not be processed.

This page is used when the backend determines the AppEEARS task submission failed.

---

### POST `/request_api_data`

Purpose: authenticate the user to NASA AppEEARS and initialize the request session.

Request body example:

```json
{
  "username": "your_earthdata_username",
  "password": "your_earthdata_password",
  "email": "user@example.com"
}
```

Behavior:

- Stores `username`, `password`, and `email` in the current `HttpSession`.
- Sends a POST request to `https://appeears.earthdatacloud.nasa.gov/api/login`.
- Saves the returned token in the session.
- Removes the password from the session immediately after successful authentication.
- If NASA rejects the credentials, it sets the session token to `unauthorized`.

This endpoint is the first API handshake between the app and the AppEEARS service.

---

### GET `/get_token`

Purpose: return the AppEEARS authentication token to the front-end.

Behavior:

- Reads the token from the current session.
- Returns JSON like:

```json
{ "token": "...actual-token..." }
```

or:

```json
{ "token": "unauthorized" }
```

If the login was unsuccessful, the front-end shows an authentication failure message and sends the user back to `/`.

---

### POST `/prepare_task`

Purpose: prepare and submit the point-request task to AppEEARS and pass the task information to the prediction service.

Request body example:

```json
{
  "taskName": "point_tutorial_test",
  "latitude": 36.206228,
  "longitude": -112.127134,
  "date": "03-19-2026"
}
```

Behavior:

1. Saves the task metadata into the session:
   - task name
   - latitude
   - longitude
   - end date
2. Calculates a 30-day window ending at the selected date.
3. Builds an AppEEARS point-task JSON payload containing:
   - coordinates
   - date range
   - layers:
     - `LST_Day_1km` from `MOD11A2.061`
     - `_1_km_16_days_NDVI` from `MOD13A2.061`
     - `sur_refl_b05` from `MOD09A1.061`
4. Sends the request to `https://appeears.earthdatacloud.nasa.gov/api/task` using the bearer token.
5. Parses the response and checks the task status.
6. If the task is in a valid state (`pending`, `processing`, `done`, or `queued`), it sends a second request to the prediction backend.
7. Calls the prediction server at `${API_URL}/transfer` with:
   - token
   - taskId
   - email
8. Replies to the browser with a status payload like:

```json
{ "status": "success" }
```

Possible statuses returned by the backend:

- `success` → task and prediction submission were accepted
- `fail` → AppEEARS task submission failed or response was invalid
- `pred_fail` → AppEEARS task accepted, but prediction service was unreachable
- `limited` → the prediction service rate limit was hit
- `failed` → prediction backend returned a generic failure state

The front-end uses these statuses to redirect to `/success_page`, `/fail_page`, or display an in-page modal error message.


---

## Front-End Files

### `index.html`
The welcome page and credential collection form.

### `task_form.html`
The point-request task form with latitude, longitude, and date inputs.

### `success.html`
Shows a successful submission confirmation message.

### `fail.html`
Shows a failed submission message.

### `index.js`
Handles login validation and the POST call to `/request_api_data`.

### `task_form.js`
Fetches the token, validates the task submission, calls `/prepare_task`, and redirects based on the server response.

---

## Typical User Journey

A normal user flow is:

1. Open the app at `http://localhost:5000/`
2. Enter Earthdata username, password, and email
3. Submit credentials
4. See the task form page
5. Enter a task name, coordinates, and date
6. Submit the point request
7. Receive confirmation or failure response
8. Wait for the prediction result to be emailed

---

## Security and Data Handling

This app is intended for demonstration and project use, but it is important to note:

- Credentials are not stored in a database.
- Session data is used temporarily for request processing.
- Sensitive values should be managed through environment variables and secure deployment settings in production.

---

## Summary

This application is a data collection and task- submission layer for wildfire prediction. It performs a secure login to NASA AppEEARS, creates a remote sensing extraction task, and sends the task metadata to a prediction backend for downstream analysis and email notification.
