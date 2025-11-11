# webhook-solver

Spring Boot app that posts to generateWebhook and auto-submits SQL answer.

## Setup

1. Edit `WebhookSolverApplication.java` and set:
   - `name`
   - `regNo` (very important — odd/even chooses question)
   - `email`

2. Build:
mvn clean package


3. Run:
java -jar target/webhook-solver-1.0.0.jar


The app will:
- call `https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA`
- receive `webhook` and `accessToken`
- choose SQL answer for odd or even regNo (embedded)
- post `{"finalQuery": "...SQL..."}` to the returned webhook with `Authorization: <accessToken>`

## Files included
- Source code
- This README

## Troubleshooting
- If the webhook POST returns 401, change the Authorization header to `Bearer <token>` in the source and rebuild.
- If API field names differ, inspect the JSON printed by the app and adjust parsing in the code.

