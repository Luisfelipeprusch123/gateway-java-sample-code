if (self === top) {
    var antiClickjack = document.getElementById("antiClickjack");
    if (antiClickjack) antiClickjack.parentNode.removeChild(antiClickjack);
} else {
    top.location = self.location;
}

PaymentSession.configure({
    fields: {
        // ATTACH HOSTED FIELDS TO YOUR PAYMENT PAGE FOR A CREDIT CARD
        card: {
            number: "#card-number",
            securityCode: "#security-code",
            expiryMonth: "#expiry-month",
            expiryYear: "#expiry-year"
        }
    },
    //SPECIFY YOUR MITIGATION OPTION HERE
    frameEmbeddingMitigation: ["javascript"],
    callbacks: {
        initialized: function (response) {
            // HANDLE INITIALIZATION RESPONSE
        },
        formSessionUpdate: function (response) {
            // HANDLE RESPONSE FOR UPDATE SESSION
            if (response.status) {
                if ("ok" == response.status) {
                    console.log("Session updated with data: " + response.session.id);

                    //check if the security code was provided by the user
                    if (response.sourceOfFunds.provided.card.securityCode) {
                        console.log("Security code was provided.");
                    }

                    //check if the user entered a MasterCard credit card
                    if (response.sourceOfFunds.provided.card.scheme == 'MASTERCARD') {
                        console.log("The user entered a MasterCard credit card.")
                    }

                    if (JavaSample.protocol === "NVP") {
                        //Make a POST call
                        var $form = $("#formPay");
                        $('#session-id').val(response.session.id);
                        $form.submit();
                    }
                    else {
                        //process operation
                        // TODO need to do post here instead
                        window.location.href = JavaSample.endpoint() + JavaSample.operation() + "/" + response.session.id + JavaSample.params();
                    }
                } else if ("fields_in_error" == response.status) {

                    console.log("Session update failed with field errors.");
                    if (response.errors.cardNumber) {
                        console.log("Card number invalid or missing.");
                    }
                    if (response.errors.expiryYear) {
                        console.log("Expiry year invalid or missing.");
                    }
                    if (response.errors.expiryMonth) {
                        console.log("Expiry month invalid or missing.");
                    }
                    if (response.errors.securityCode) {
                        console.log("Security code invalid.");
                    }
                } else if ("request_timeout" == response.status) {
                    console.log("Session update failed with request timeout: " + response.errors.message);
                } else if ("system_error" == response.status) {
                    console.log("Session update failed with system error: " + response.errors.message);
                }
            } else {
                console.log("Session update failed: " + response);
            }
        }
    }
});

function pay() {
    // UPDATE THE SESSION WITH THE INPUT FROM HOSTED FIELDS
    PaymentSession.updateSessionFromForm('card');
}