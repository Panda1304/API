package com.example.webhooksolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class WebhookSolverApplication implements CommandLineRunner {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String name = "Subham Panda";
    private final String regNo = "U25UV22T006051"; 
    private final String email = "subham.panda@campusuvce.in";

    // SQL answer for ODD (question 1) 
    private static final String ODD_SQL = """
        SELECT 
            p.AMOUNT AS SALARY,
            CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME,
            FLOOR(DATEDIFF(CURDATE(), e.DOB) / 365) AS AGE,
            d.DEPARTMENT_NAME
        FROM PAYMENTS p
        JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID
        JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID
        WHERE DAY(p.PAYMENT_TIME) != 1
          AND p.AMOUNT = (
              SELECT MAX(AMOUNT)
              FROM PAYMENTS
              WHERE DAY(PAYMENT_TIME) != 1
          );
        """;

    // SQL answer for EVEN (question 2)
    private static final String EVEN_SQL = """
        SELECT 
            e1.EMP_ID,
            e1.FIRST_NAME,
            e1.LAST_NAME,
            d.DEPARTMENT_NAME,
            COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT
        FROM EMPLOYEE e1
        JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID
        LEFT JOIN EMPLOYEE e2 
            ON e1.DEPARTMENT = e2.DEPARTMENT
            AND e2.DOB > e1.DOB
        GROUP BY 
            e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME
        ORDER BY 
            e1.EMP_ID DESC;
        """;

    public static void main(String[] args) {
        SpringApplication.run(WebhookSolverApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Webhook solver starting...");

        String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("regNo", regNo);
        body.put("email", email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String,String>> request = new HttpEntity<>(body, headers);

        System.out.println("Calling generateWebhook endpoint...");
        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(generateUrl, request, String.class);
        } catch (Exception ex) {
            System.err.println("generateWebhook call failed: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        System.out.println("Status: " + response.getStatusCodeValue());
        System.out.println("Response body: " + response.getBody());

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            System.err.println("Failed to get valid response from generateWebhook.");
            return;
        }

        JsonNode respJson = mapper.readTree(response.getBody());
        String webhookUrl = null;
        String accessToken = null;

        if (respJson.has("webhook")) webhookUrl = respJson.get("webhook").asText();
        if (respJson.has("accessToken")) accessToken = respJson.get("accessToken").asText();
        if (accessToken == null && respJson.has("token")) accessToken = respJson.get("token").asText();
        if (accessToken == null && respJson.has("access_token")) accessToken = respJson.get("access_token").asText();

        if (webhookUrl == null || accessToken == null) {
            System.err.println("Missing webhook or accessToken in response. Full response JSON: " + respJson.toPrettyString());
            return;
        }

        int lastTwo = extractLastTwoDigits(regNo);
        boolean isOdd = (lastTwo % 2 != 0);
        System.out.println("Last two digits: " + lastTwo + " -> " + (isOdd ? "ODD (Question 1)" : "EVEN (Question 2)"));

        String finalSql = isOdd ? ODD_SQL : EVEN_SQL;

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.set("Authorization", accessToken);

        Map<String, String> submitBody = new HashMap<>();
        submitBody.put("finalQuery", finalSql);

        HttpEntity<Map<String, String>> submitRequest = new HttpEntity<>(submitBody, authHeaders);

        System.out.println("Submitting finalQuery to webhook: " + webhookUrl);
        try {
            ResponseEntity<String> submitResp = restTemplate.postForEntity(webhookUrl, submitRequest, String.class);
            System.out.println("Submit status: " + submitResp.getStatusCodeValue());
            System.out.println("Submit response body: " + submitResp.getBody());
        } catch (Exception ex) {
            System.err.println("Submit call failed: " + ex.getMessage());
            ex.printStackTrace();
            System.out.println("If 401 Unauthorized, try switching Authorization header to 'Bearer <token>' in the code.");
        }

        System.out.println("Flow finished. Exiting.");
        System.exit(0);
    }

    private int extractLastTwoDigits(String regNo) {
        String digits = regNo.replaceAll("\\D+", "");
        if (digits.length() >= 2) {
            return Integer.parseInt(digits.substring(digits.length()-2));
        } else if (digits.length() == 1) {
            return Integer.parseInt(digits);
        } else {
            return 0;
        }
    }
}

