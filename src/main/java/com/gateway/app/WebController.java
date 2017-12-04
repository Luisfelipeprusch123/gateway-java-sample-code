package com.gateway.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.client.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.measure.unit.Dimension;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;

@Controller
public class WebController {


    @Autowired
    private Config config;

    @GetMapping("/authorize")
    public ModelAndView showAuthorize() {
        ModelAndView mav = new ModelAndView("authorize");
        ApiRequest req = createTestRequest("AUTHORIZE");
        req.setTransactionId(randomNumber());
        req.setOrderId(randomNumber());
        mav.addObject("apiRequest", req);
        mav.addObject("merchantId", config.getMerchantId());
        mav.addObject("baseUrl", config.getApiBaseURL());
        return mav;
    }

    @GetMapping("/capture")
    public ModelAndView showCapture() {
        ModelAndView mav = new ModelAndView("capture");
        ApiRequest req = createTestRequest("CAPTURE");
        req.setTransactionId(randomNumber());
        mav.addObject("apiRequest", req);
        return mav;
    }

    @GetMapping("/confirm")
    public ModelAndView showConfirm() {
        ModelAndView mav = new ModelAndView("confirm");
        ApiRequest req = createTestRequest("CONFIRM_BROWSER_PAYMENT");
        req.setTransactionId(randomNumber());
        req.setOrderId(randomNumber());
        mav.addObject("apiRequest", req);
        return mav;
    }

    @GetMapping("/initiate")
    public ModelAndView showInitiate() {
        ModelAndView mav = new ModelAndView("initiate");
        ApiRequest req = createTestRequest("INITIATE_BROWSER_PAYMENT");
        req.setTransactionId(randomNumber());
        req.setOrderId(randomNumber());
        req.setSourceType(null);
        mav.addObject("apiRequest", req);
        return mav;
    }

    @GetMapping("/pay")
    public ModelAndView showPay() {
        ModelAndView mav = new ModelAndView("pay");
        ApiRequest req = createTestRequest("PAY");
        req.setOrderId(randomNumber());
        req.setTransactionId(randomNumber());
        mav.addObject("apiRequest", req);
        return mav;
    }

    @GetMapping("/refund")
    public ModelAndView showRefund() {
        ModelAndView mav = new ModelAndView("refund");
        ApiRequest req = createTestRequest("REFUND");
        req.setTransactionId(randomNumber());
        mav.addObject("apiRequest", req);
        return mav;
    }

    @GetMapping("/retrieve")
    public ModelAndView showRetrieve() {
        ModelAndView mav = new ModelAndView("retrieve");
        ApiRequest req = createTestRequest("RETRIEVE_TRANSACTION");
        req.setMethod("GET");
        mav.addObject("apiRequest", req);
        return mav;
    }

    @GetMapping("/update")
    public ModelAndView showUpdate() {
        ModelAndView mav = new ModelAndView("update");
        ApiRequest req = createTestRequest("UPDATE_AUTHORIZATION");
        req.setTransactionId(randomNumber());
        mav.addObject("apiRequest", req);
        return mav;
    }

    @GetMapping("/verify")
    public ModelAndView showVerify() {
        ModelAndView mav = new ModelAndView("verify");
        ApiRequest req = createTestRequest("VERIFY");
        req.setOrderId(randomNumber());
        req.setTransactionId(randomNumber());
        mav.addObject("apiRequest", req);
        return mav;
    }

    @GetMapping("/void")
    public ModelAndView showVoid() {
        ModelAndView mav = new ModelAndView("void");
        ApiRequest req = createTestRequest("VOID");
        req.setTransactionId(randomNumber());
        mav.addObject("apiRequest", req);
        return mav;
    }

    @GetMapping("/hostedCheckout")
    public ModelAndView showHostedCheckout() {
        ModelAndView mav = new ModelAndView("hostedCheckout");
        ApiRequest req = new ApiRequest();
        req.setApiOperation("CREATE_CHECKOUT_SESSION");
        req.setOrderId(randomNumber());
        req.setOrderCurrency("USD");

        String requestUrl = ClientUtil.getSessionRequestUrl(config);

        String data = ClientUtil.buildJSONPayload(req);

        try {
            ApiClient connection = new ApiClient();
            String resp = connection.postTransaction(data, requestUrl, config);
            System.out.println("CREATED SESSION: " + resp);

            JsonObject json = new Gson().fromJson(resp, JsonObject.class);
            JsonObject jsonSession = json.get("session").getAsJsonObject();

            CheckoutSession checkoutSession = new CheckoutSession();
            checkoutSession.setId(jsonSession.get("id").getAsString());
            checkoutSession.setVersion(jsonSession.get("version").getAsString());
            checkoutSession.setSuccessIndicator(json.get("successIndicator").getAsString());

            mav.addObject("orderId", req.getOrderId());
            mav.addObject("sessionId", checkoutSession.getId());
            mav.addObject("sessionVersion", checkoutSession.getVersion());
            mav.addObject("successIndicator", checkoutSession.getSuccessIndicator());
            mav.addObject("merchantId", config.getMerchantId());
            mav.addObject("apiPassword", config.getApiPassword());
        } catch (Exception e) {
            e.printStackTrace();
            mav.addObject("error", e.getMessage());
        }

        return mav;
    }

    @GetMapping("/browserPaymentReceipt")
    public ModelAndView browserPaymentReceipt(HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("browserPaymentReceipt");
        String data = "";
        try {
            StringBuilder buffer = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            data = buffer.toString();
            mav.addObject("request", data);
        } catch (IOException e) {
            e.printStackTrace();
            mav.addObject("error", e.getMessage());
        }

        return mav;
    }

    @GetMapping("/hostedCheckoutReceipt/{orderId}/{result}")
    public ModelAndView hostedCheckoutReceipt(@PathVariable(value="orderId") String orderId, @PathVariable(value="result") String result) {

        ModelAndView mav = new ModelAndView("hostedCheckoutReceipt");

        if(result.equals("success")) {
            // Retrieve order details
            ApiRequest req = new ApiRequest();
            req.setApiOperation("RETRIEVE_ORDER");
            req.setOrderId(orderId);

            String requestUrl = ClientUtil.getRequestUrl(config, req);
            System.out.println("REQUEST URL: " + requestUrl);

            try {
                ApiClient connection = new ApiClient();
                String resp = connection.getTransaction(requestUrl, config);
                System.out.println("RETRIEVE ORDER: " + resp);
                mav.addObject("orderDetails", resp);
            }
            catch(Exception e) {
                e.printStackTrace();
                mav.addObject("error", e.getMessage());
            }
        }

        mav.addObject("result", result);
        return mav;
    }

    // Endpoint for Hosted Session
    @GetMapping("/process/{operation}/{sessionId}")
    public ModelAndView process(@PathVariable(value="operation") String operation, @PathVariable(value="sessionId") String sessionId) {

        ModelAndView mav = new ModelAndView("receipt");

        try {
            // Retrieve session
            String url = ClientUtil.getSessionRequestUrl(config, sessionId);
            ApiClient sessionConnection = new ApiClient();
            String sessionResponse = sessionConnection.getTransaction(url, config);

            // Parse session response into CheckoutSession object
            CheckoutSession session = parseSessionResponse(sessionResponse);

            // Construct API request
            ApiRequest request = createTestRequest(operation);
            request.setSessionId(session.getId());
            String jsonPayload = ClientUtil.buildJSONPayload(request);
            String requestUrl = ClientUtil.getRequestUrl(config, request);

            // Perform API operation
            ApiClient apiConnection = new ApiClient();
            String apiResponse = apiConnection.sendTransaction(jsonPayload, requestUrl, config);

            // Send info on transaction to view
            ObjectMapper mapper = new ObjectMapper();
            Object prettyResp = mapper.readValue(apiResponse, Object.class);
            Object prettyPayload = mapper.readValue(jsonPayload, Object.class);
            mav.addObject("resp", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prettyResp));
            mav.addObject("operation", request.getApiOperation());
            mav.addObject("method", request.getMethod());
            mav.addObject("request", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prettyPayload));
            mav.addObject("requestUrl", requestUrl);
        }
        catch(Exception e) {
            e.printStackTrace();
            mav.addObject("error", e.getMessage());
        }
        return mav;
    }

    // Endpoint for form POST
    @PostMapping("/process")
    public ModelAndView process(ApiRequest request) {

        String requestUrl = ClientUtil.getRequestUrl(config, request);
        String jsonPayload = ClientUtil.buildJSONPayload(request);

        String resp = "";

        ModelAndView mav = new ModelAndView("receipt");

        try {
            ApiClient connection = new ApiClient();
            if (request.getMethod().equals("PUT")) {
                resp = connection.sendTransaction(jsonPayload, requestUrl, config);
            } else if (request.getMethod().equals("GET")) {
                resp = connection.getTransaction(requestUrl, config);
            }
            ObjectMapper mapper = new ObjectMapper();
            Object prettyResp = mapper.readValue(resp, Object.class);
            Object prettyPayload = mapper.readValue(jsonPayload, Object.class);
            mav.addObject("resp", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prettyResp));
            mav.addObject("operation", request.getApiOperation());
            mav.addObject("method", request.getMethod());
            mav.addObject("request", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prettyPayload));
            mav.addObject("requestUrl", requestUrl);
        } catch (Exception e) {
            e.printStackTrace();
            mav.addObject("error", e.getMessage());
        }

        return mav;
    }

    private static String randomNumber() {
        return RandomStringUtils.random(10, true, true);
    }

    private CheckoutSession parseSessionResponse(String sessionResponse) {
        JsonObject json = new Gson().fromJson(sessionResponse, JsonObject.class);
        JsonObject jsonSession = json.get("session").getAsJsonObject();

        CheckoutSession checkoutSession = new CheckoutSession();
        checkoutSession.setId(jsonSession.get("id").getAsString());
        checkoutSession.setVersion(jsonSession.get("version").getAsString());

        return checkoutSession;
    }

    public static ApiRequest createTestRequest(String apiOperation) {
        ApiRequest req = new ApiRequest();
        req.setApiOperation(apiOperation);
        if(apiOperation.equals("AUTHORIZE")) {
            req.setMethod("PUT");
        }
        req.setOrderAmount("5000");
        req.setOrderCurrency("USD");
        req.setOrderId(randomNumber());
        req.setTransactionId(randomNumber());
        //TODO: This URL should come from the client dynamically
        req.setReturnUrl("http://localhost:5000/browserPaymentReceipt");
        return req;
    }

}