/*
 * Copyright (c) 2019 MasterCard. All rights reserved.
 */

package com.gateway;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.gateway.app.Config;
import com.gateway.client.ApiException;
import com.gateway.client.ApiResponseService;
import com.gateway.client.HostedSession;
import com.gateway.response.BrowserPaymentResponse;
import com.gateway.response.SecureIdEnrollmentResponse;
import com.gateway.response.TransactionResponse;
import com.gateway.response.WalletResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ApiResponseServiceTest {

    private Config config;

    @Before
    public void setUp() {
        config = new Config();
        config.setMerchantId("TESTMERCHANTID");
        config.setApiPassword("APIPASSWORD1234");
        config.setApiBaseURL("https://test-gateway.com");
        config.setGatewayHost("https://test-gateway.com");
        config.setCurrency("USD");
        config.setApiVersion(45);
    }

    @Test
    public void parseSessionResponse() throws Exception {
        String data = "{\"merchant\":\"TESTAB2894354\",\"result\":\"SUCCESS\",\"session\":{\"id\":\"SESSION0002799480514F69145320L2\",\"updateStatus\":\"SUCCESS\",\"version\":\"6f8b683701\"},\"successIndicator\":\"0a292205c57e4dc8\"}";
        HostedSession session = ApiResponseService.parseSessionResponse(data);

        assertEquals("SESSION0002799480514F69145320L2", session.getId());
        assertEquals("0a292205c57e4dc8", session.getSuccessIndicator());
        assertEquals("6f8b683701", session.getVersion());

        data = "{\n" +
                "    \"merchant\": \"TESTAB2894354\",\n" +
                "    \"result\": \"SUCCESS\",\n" +
                "    \"session\": {\n" +
                "        \"aes256Key\": \"DXbdcjSznnTId2lQWjs3Y4KVZuC4OPPn5bRkjCcm2sM=\",\n" +
                "        \"authenticationLimit\": 25,\n" +
                "        \"id\": \"SESSION0002411702239G42317712I7\",\n" +
                "        \"updateStatus\": \"NO_UPDATE\",\n" +
                "        \"version\": \"4e9556a901\"\n" +
                "    }\n" +
                "}";
        session = ApiResponseService.parseSessionResponse(data);

        assertEquals("SESSION0002411702239G42317712I7", session.getId());
        assertEquals("DXbdcjSznnTId2lQWjs3Y4KVZuC4OPPn5bRkjCcm2sM=", session.getAes256Key());
        assertEquals("NO_UPDATE", session.getUpdateStatus());
        assertEquals("4e9556a901", session.getVersion());

    }

    @Test
    public void parse3DSecureResponse() throws Exception {
        String data = "{\"3DSecure\":{\"authenticationRedirect\":{\"customized\":{\"acsUrl\":\"https://www.issuer.com/acsUrl\",\"paReq\":\"PAREQ_VALUE\"}},\"summaryStatus\":\"CARD_ENROLLED\"},\"3DSecureId\":\"wqUyNrvOO6\",\"merchant\":\"TESTAB2894354\",\"response\":{\"3DSecure\":{\"gatewayCode\":\"CARD_ENROLLED\"}}}";
        SecureIdEnrollmentResponse secureIdEnrollmentResponse = ApiResponseService.parse3DSecureResponse(data);

        assertEquals("https://www.issuer.com/acsUrl", secureIdEnrollmentResponse.getAcsUrl());
        assertEquals("CARD_ENROLLED", secureIdEnrollmentResponse.getStatus());
        assertEquals("PAREQ_VALUE", secureIdEnrollmentResponse.getPaReq());
    }

    @Test
    public void parse3DSecure2UpdateSessionResponse() throws Exception {
        String data = "{\n" +
                "    \"authentication\": {\n" +
                "        \"channel\": \"MERCHANT_REQUESTED\"\n" +
                "    },\n" +
                "    \"merchant\": \"TESTWTF25446060\",\n" +
                "    \"order\": {\n" +
                "        \"amount\": \"5000\",\n" +
                "        \"currency\": \"USD\",\n" +
                "        \"id\": \"order-693\"\n" +
                "    },\n" +
                "    \"session\": {\n" +
                "        \"id\": \"SESSION0002396845074E89371117N9\",\n" +
                "        \"updateStatus\": \"SUCCESS\",\n" +
                "        \"version\": \"fd4b2f7b02\"\n" +
                "    },\n" +
                "    \"version\": \"51\"\n" +
                "}";
        HostedSession hostedSession = ApiResponseService.parseSessionResponse(data);

        assertEquals("SUCCESS", hostedSession.getUpdateStatus());
        assertEquals("fd4b2f7b02", hostedSession.getVersion());
        assertEquals("SESSION0002396845074E89371117N9", hostedSession.getId());
    }

    @Test
    public void parseHostedCheckoutResponse() throws Exception {
        String data = "{\"amount\":\"100.00\",\"currency\":\"USD\",\"description\":\"Ordered goods\",\"id\":\"order-W9JzSaC1Ky\",\"merchant\":\"TESTSIMPLIFYDEV1\",\"result\":\"SUCCESS\",\"status\":\"CAPTURED\",\"transaction\":[{\"order\":{\"amount\":\"100.00\",\"currency\":\"USD\",\"description\":\"Ordered goods\",\"id\":\"order-W9JzSaC1Ky\"},\"response\":{\"acquirerCode\":\"00\",\"cardSecurityCode\":{\"acquirerCode\":\"M\",\"gatewayCode\":\"MATCH\"},\"gatewayCode\":\"APPROVED\"},\"result\":\"SUCCESS\",\"transaction\":{\"acquirer\":{\"batch\":1,\"id\":\"SYSTEST_ACQ1\",\"merchantId\":\"646515314\"},\"amount\":100,\"authorizationCode\":\"027465\",\"currency\":\"USD\",\"frequency\":\"SINGLE\",\"id\":\"1\",\"receipt\":\"180131305\",\"source\":\"INTERNET\",\"terminal\":\"9358\",\"type\":\"PAYMENT\"},\"version\":\"45\"}]}";
        TransactionResponse response = ApiResponseService.parseHostedCheckoutResponse(data);

        assertEquals("SUCCESS", response.getApiResult());
        assertEquals("APPROVED", response.getGatewayCode());
        assertEquals("100.00", response.getOrderAmount());
        assertEquals("USD", response.getOrderCurrency());
        assertEquals("order-W9JzSaC1Ky", response.getOrderId());
        assertEquals("Ordered goods", response.getOrderDescription());
    }

    @Test
    public void parseAuthorizeResponse() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("json/Response_AUTHORIZE.json")) {

            String data = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            TransactionResponse response = ApiResponseService.parseAuthorizeResponse(data);

            assertEquals("SUCCESS", response.getApiResult());
            assertEquals("APPROVED", response.getGatewayCode());
            assertEquals("100", response.getOrderAmount());
            assertEquals("AUD", response.getOrderCurrency());
            assertEquals("7655c56a-bccb-4974-aae2-721240fbaa94", response.getOrderId());
        }
    }

    /* essentials_exclude_start */
    @Test
    public void parseMasterpassResponse() throws Exception {
        String data = "{\"order\":{\"amount\":\"5000.00\",\"currency\":\"USD\",\"id\":\"order-78oSgRzCqs\",\"status\":\"CAPTURED\",\"totalAuthorizedAmount\":5000,\"totalCapturedAmount\":5000,\"totalRefundedAmount\":0,\"walletIndicator\":\"101\",\"walletProvider\":\"MASTERPASS_ONLINE\"},\"response\":{\"acquirerCode\":\"00\",\"acquirerMessage\":\"Approved\",\"gatewayCode\":\"APPROVED\"},\"result\":\"SUCCESS\"}";
        TransactionResponse response = ApiResponseService.parseMasterpassResponse(data);

        assertEquals("SUCCESS", response.getApiResult());
        assertEquals("APPROVED", response.getGatewayCode());
        assertEquals("5000.00", response.getOrderAmount());
        assertEquals("USD", response.getOrderCurrency());
        assertEquals("order-78oSgRzCqs", response.getOrderId());
        
    }
    /* essentials_exclude_end */

    @Test
    public void parseBrowserPaymentResponse() throws Exception {
        String data = "{\"browserPayment\":{\"interaction\":{\"status\":\"COMPLETED\"},\"operation\":\"PAY\"},\"order\":{\"amount\":\"50.00\",\"currency\":\"USD\",\"id\":\"order-E5AaY8Hsuo\",\"status\":\"CAPTURED\"},\"response\":{\"acquirerCode\":\"Success\",\"gatewayCode\":\"APPROVED\"},\"result\":\"SUCCESS\"}";
        BrowserPaymentResponse response = ApiResponseService.parseBrowserPaymentResponse(data);

        assertEquals("SUCCESS", response.getApiResult());
        assertEquals("APPROVED", response.getGatewayCode());
        assertEquals("COMPLETED", response.getInteractionStatus());
        assertEquals("50.00", response.getOrderAmount());
        assertEquals("USD", response.getOrderCurrency());
        assertEquals("order-E5AaY8Hsuo", response.getOrderId());
    }

    /* essentials_exclude_start */
    @Test
    public void parseWalletResponse() throws Exception {
        String data = "{\"merchant\":\"TESTCSTESTMID\",\"order\":{\"amount\":\"50.00\",\"currency\":\"USD\",\"walletProvider\":\"MASTERPASS_ONLINE\"},\"session\":{\"id\":\"SESSION0002798226376L35023121J8\",\"updateStatus\":\"SUCCESS\",\"version\":\"831cb86303\"},\"version\":\"45\",\"wallet\":{\"masterpass\":{\"allowedCardTypes\":\"visa,master\",\"merchantCheckoutId\":\"MERCHANT_CHECKOUT_ID\",\"originUrl\":\"http://localhost:5000/masterpassResponse\",\"requestToken\":\"REQUEST_TOKEN\"}}}";
        WalletResponse response = ApiResponseService.parseWalletResponse(data, "masterpass");

        assertEquals("USD", response.getOrderCurrency());
        assertEquals("50.00", response.getOrderAmount());
        assertEquals("visa,master", response.getAllowedCardTypes());
        assertEquals("MERCHANT_CHECKOUT_ID", response.getMerchantCheckoutId());
        assertEquals("http://localhost:5000/masterpassResponse", response.getOriginUrl());
        assertEquals("REQUEST_TOKEN", response.getRequestToken());
    }
    /* essentials_exclude_end */

    @Test
    public void parseNVPResponse() throws Exception {
        String data = "merchant=TESTCSTESTMID&order.amount=50.00&order.currency=USD&order.id=IXoAyo48VS&order.status=CAPTURED&response.gatewayCode=APPROVED&result=SUCCESS";
        Map<String, String> map = ApiResponseService.parseNVPResponse(data);

        assertEquals("TESTCSTESTMID", map.get("merchant"));
        assertEquals("50.00", map.get("order.amount"));
        assertEquals("USD", map.get("order.currency"));
        assertEquals("IXoAyo48VS", map.get("order.id"));
        assertEquals("CAPTURED", map.get("order.status"));
        assertEquals("APPROVED", map.get("response.gatewayCode"));
        assertEquals("SUCCESS", map.get("result"));
    }

    @Test
    public void parseNVPErrorResponseThrowsApiException() throws Exception {
        String data = "error.cause=INVALID_REQUEST&error.explanation=Value+%27PAY%27+is+invalid.+Pay+request+not+permitted+for+this+merchant.&error.field=apiOperation&error.validationType=INVALID&result=ERROR";

        try {
            Map<String, String> map = ApiResponseService.parseNVPResponse(data);
            fail("Expected exception was not thrown");
        } catch (ApiException e) {
            assertEquals("INVALID_REQUEST", e.getErrorCode());
            assertEquals("Value 'PAY' is invalid. Pay request not permitted for this merchant.", e.getExplanation());
            assertEquals("apiOperation", e.getField());
            assertEquals("INVALID", e.getValidationType());
        }
    }

    @Test
    public void parseTokenResponse() throws Exception {
        String data = "{\"token\":\"MYTOKEN\"}";

        assertEquals("MYTOKEN", ApiResponseService.parseTokenResponse(data));
    }

    /* essentials_exclude_start */
    @Test
    public void getBrowserPaymentRedirectUrl() throws Exception {
        String data = "{\"browserPayment\":{\"interaction\":{\"status\":\"INITIATED\"},\"operation\":\"PAY\",\"browserPayment\":{\"displayShippingAddress\":true,\"overrideShippingAddress\":true,\"paymentConfirmation\":\"CONFIRM_AT_PROVIDER\"},\"redirectUrl\":\"https://test-gateway.com/bpui/pp/out/BP-4652f0dd79cade57ba6726992464c994\",\"returnUrl\":\"http://localhost:5000/browserPaymentReceipt?transactionId=oZRL5sU3Fm&orderId=Qcgkl4EGnR\"},\"gatewayEntryPoint\":\"WEB_SERVICES_API\",\"merchant\":\"TESTSIMPLIFYDEV1\",\"order\":{\"amount\":50.00,\"creationTime\":\"2018-01-29T16:08:39.296Z\",\"currency\":\"USD\",\"id\":\"Qcgkl4EGnR\",\"status\":\"INITIATED\",\"totalAuthorizedAmount\":0,\"totalCapturedAmount\":0,\"totalRefundedAmount\":0},\"response\":{\"gatewayCode\":\"SUBMITTED\"},\"result\":\"SUCCESS\",\"sourceOfFunds\":{\"type\":\"UNION_PAY\"},\"timeOfRecord\":\"2018-01-29T16:08:39.296Z\",\"transaction\":{\"acquirer\":{\"date\":\"2018-01-29\",\"id\":\"UNION_PAY\",\"merchantId\":\"test.sandbox@unionpay.com\",\"time\":\"16:08:39\"},\"amount\":50.00,\"currency\":\"USD\",\"frequency\":\"SINGLE\",\"id\":\"oZRL5sU3Fm\",\"source\":\"CALL_CENTRE\",\"type\":\"PAYMENT\"},\"version\":\"45\"}";

        assertEquals("https://test-gateway.com/bpui/pp/out/BP-4652f0dd79cade57ba6726992464c994",
                ApiResponseService.getBrowserPaymentRedirectUrl(data));

    }
    /* essentials_exclude_end */
}
