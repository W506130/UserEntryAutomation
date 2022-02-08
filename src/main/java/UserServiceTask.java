import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.concurrent.Callable;

public class UserServiceTask implements Callable<UserServiceResponse> {
    private String requestBodyParams;
    private String clientMid;
    private String authToken;
    private int rowNumber;

    public UserServiceTask(String requestBodyParams, String clientMid, String authToken, int rowNumber) {
        this.requestBodyParams = requestBodyParams;
        this.clientMid = clientMid;
        this.authToken = authToken;
        this.rowNumber = rowNumber;
    }

    @Override
    public UserServiceResponse call() throws Exception {
        RequestSpecification request = RestAssured.given().config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().
                        appendDefaultContentCharsetToContentTypeIfUndefined(false))).header("Authorization", "Bearer " +
                        authToken).header("API-Version", "2.0.0")
                .header("Current-Client-Mid",clientMid )
                .contentType(ContentType.JSON).body(requestBodyParams).when();

        System.out.println("Request info::" + request.log().all());

        String userEndPointAPI="/user";
        Response response = request.post(RestAssured.baseURI + userEndPointAPI);
        UserServiceResponse userServiceResponse = new UserServiceResponse();
        userServiceResponse.setRowNumber(this.rowNumber);
        userServiceResponse.setStatus(response.getStatusCode());
        userServiceResponse.setServiceResponse(response.getBody().print());
        if(response.prettyPrint().contains("error")) {
            userServiceResponse.setErrorMessage(response.getBody().print());
        }
        return userServiceResponse;
    }
}
