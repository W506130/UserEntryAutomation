import com.google.gson.Gson;
import io.cucumber.java.Scenario;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
//import utilities.api.PropUtils;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class UserCreation {
    final String COUNTRY_CLIENT_QUERY = " SELECT C.DESCRIPTION, CLI.CLIENT_MID FROM M_CLIENTS CLI INNER JOIN COUNTRIES C ON CLI.COUNTRY_OID = C.COUNTRY_OID ";
    final String ROLE_OID_QUERY = " SELECT A.DESCRIPTION, A.ACCESS_ROLE_OID FROM ACCESS_ROLES A ";
    final String CGMID_QUERY = " SELECT A.DESCRIPTION, A.CLIENT_GROUP_MID FROM ACCESS_ROLES A ";
    final String LOGIN_CLIENT_QUERY = " SELECT 'CLIENT_MID', ARC.CLIENT_MID FROM ACCESS_ROLE_CLIENTS ARC INNER JOIN USER_ROLES UR ON UR.ACCESS_ROLE_OID = ARC.ACCESS_ROLE_OID INNER JOIN USERS U ON U.USER_OID = UR.USER_OID WHERE U.LOGON_ID = '?' ";
    String CONFIG_PROPERTY_FILE_BASE_PATH = null;
    public JSONObject requestParams = new JSONObject();
    public static Response response;
    public RequestSpecification request;
    String authToken = "";
    public JsonPath jsonPathEvaluator;
    public Scenario logger;

    private Properties loadConfigProperties(String environment) {
        CONFIG_PROPERTY_FILE_BASE_PATH = System.getProperty("property.folder.path", "../");
        System.out.println(CONFIG_PROPERTY_FILE_BASE_PATH);
        String configFIlePath = CONFIG_PROPERTY_FILE_BASE_PATH + "/user-creation-" + "prop" + ".properties";
        Properties configProperties = PropUtils.getProps(new File(configFIlePath));
        return configProperties;
    }

    private Map<String, String> getCountryLevelClientMids(Properties configProperties) throws UserCreationException {
        Map<String, String> clientMidMap = new HashMap<String, String>();
        clientMidMap = exceuteQuery(configProperties, COUNTRY_CLIENT_QUERY);
        return clientMidMap;
    }

    private String getClientMidByLogonId(Properties configProperties, String logonId) throws UserCreationException {
        Map<String, String> loginUserClientMidMap = new HashMap<String, String>();
        loginUserClientMidMap = exceuteQuery(configProperties, LOGIN_CLIENT_QUERY.replace("?", logonId));
        return loginUserClientMidMap.get("client_mid");
    }

    private Map<String, String> getRoleOidByRoleDescription(Properties configProperties) throws UserCreationException {
        Map<String, String> roleMap = new HashMap<String, String>();
        roleMap = exceuteQuery(configProperties, ROLE_OID_QUERY);
        return roleMap;
    }

    private Map<String, String> getClientGroupMidsByRoleDescription(Properties configProperties) throws UserCreationException {
        Map<String, String> cgMidMap = new HashMap<String, String>();
        cgMidMap = exceuteQuery(configProperties, CGMID_QUERY);
        return cgMidMap;
    }

    private Map<String, String> exceuteQuery(Properties configProperties, String query) throws UserCreationException {
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        Map<String, String> resultMap = new HashMap<>();
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            con = DriverManager.getConnection(configProperties.getProperty("dbURL"), configProperties.getProperty("dbUserName"), configProperties.getProperty("dbPassword"));
            if (con != null) {
                st = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                rs = st.executeQuery(query);
                while (rs.next()) {
                    resultMap.put(rs.getString(1).toLowerCase(), rs.getString(2));
                }
                ;
            }
        } catch (ClassNotFoundException e) {
            throw new UserCreationException("Oracle JDBC Driver not found...", e);
        } catch (SQLException e) {
            throw new UserCreationException("Connection Failed! Check output console", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                throw new UserCreationException("Closing of Connection Failed! Check output console", e);
            }
        }
        return resultMap;
    }

    public void loginToApplication(Properties configProperties) throws UserCreationException {
        String GRANT_TYPE = "password";
        RestAssured.baseURI = configProperties.getProperty("serviceBaseURL");
        requestParams.put("grant_type", GRANT_TYPE);
        requestParams.put("username", configProperties.getProperty("userName"));
        requestParams.put("password", configProperties.getProperty("passWord"));
        String stringToEncode = configProperties.getProperty("CLIENT_ID") + ":" + configProperties.getProperty("SECERT_ID");
        // Encode into Base64 format
        String accessToken = Base64.getEncoder().encodeToString(stringToEncode.getBytes());
        // Post a request for login API call
        String currentClientMid = null;
        currentClientMid = getClientMidByLogonId(configProperties, configProperties.getProperty("userName"));
        if (currentClientMid == null) {
            throw new UserCreationException("Given Login user is not asociated with any Client");
        }
        request = RestAssured.given().config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().
                        appendDefaultContentCharsetToContentTypeIfUndefined(false))).header("Authorization", "Basic " + accessToken)
                .header("API-Version", "2.0.0").header("Current-Client-Mid", currentClientMid).contentType(ContentType.JSON).
                body(requestParams.toString()).when();

        System.out.println("Request info::" + request.log().all());

        response = request.post(RestAssured.baseURI + "/login");
        jsonPathEvaluator = response.jsonPath();
        System.out.println("INSIDE THE AUTH TOKEN " + response.jsonPath().get("token"));
        authToken = response.jsonPath().get("token");
    }

    public void headerMapping(Cell cell, Map<Integer, String> columnMap, Map<String, String> columnNameMap,
                              int columnNo) {

        if ("OLS User Role".equalsIgnoreCase(cell.getStringCellValue().trim())) {
            columnMap.put(columnNo, "OBJ-role");
        } else if ("Customer Number".equalsIgnoreCase(cell.getStringCellValue().trim())) {
            columnMap.put(columnNo, "LIST-accountProfiles");
        } else if (columnNameMap.containsKey(cell.getStringCellValue().trim())) {
            columnMap.put(columnNo, columnNameMap.get(cell.getStringCellValue().trim()));
        }
    }

    public void createUsers(Properties configProperties, Map<String, String> midMap, Map<String, String> roleMap, Map<String, String> clientMidMap) throws UserCreationException {
        RestAssured.baseURI = configProperties.getProperty("serviceBaseURL");
        System.out.println("RestAssured.baseURI  " + RestAssured.baseURI);
        File file = new File(configProperties.getProperty("inputFileName"));
        File outputfile = new File("Result_" + System.currentTimeMillis() + "_" + configProperties.getProperty("inputFileName"));
        try {
            FileInputStream inputStream = new FileInputStream(file);
            FileInputStream inputStream2 = new FileInputStream(file);
            Workbook workbook = null;
            workbook = new XSSFWorkbook(inputStream);
            Workbook resultWorkBook = new XSSFWorkbook(inputStream2);
            Sheet sheet = workbook.getSheetAt(0);
            Sheet resultWorkSheet = resultWorkBook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            int columnNo;
            boolean isHeader = true;
            Map<String, String> columnNameMap = new HashMap<>();
            columnNameMap.put("Full Name", "displayName");
            columnNameMap.put("Email", "emailAddress");
            columnNameMap.put("Username", "userId");
            columnNameMap.put("Country", "Country");

            Map<Integer, String> columnMap = new HashMap<>();
            Gson gson = new Gson();

            String clientMid = "";
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(100);
            List<Future<UserServiceResponse>> resultList = new ArrayList<>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                UserProfile userProfile = new UserProfile();
                Iterator<Cell> cellIterator = row.cellIterator();
                columnNo = 1;
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    if (isHeader) {
                        headerMapping(cell, columnMap, columnNameMap, columnNo);
                    } else {
                        if (columnMap.containsKey(columnNo)) {
                            if (columnMap.get(columnNo).contains("OBJ-role")) {
                                AccessRoles accessRoles = new AccessRoles();
                                if (StringUtils.isNotEmpty(roleMap.get(cell.getStringCellValue().trim().toLowerCase()))) {
                                    accessRoles.setAccessRoleOid(Long.valueOf(roleMap.get(cell.getStringCellValue().trim().toLowerCase())));
                                }
                                accessRoles.setDescription(cell.getStringCellValue().trim());
                                if (StringUtils.isNotEmpty(clientMidMap.get(cell.getStringCellValue().trim().toLowerCase()))) {
                                    accessRoles.setClientGroupMid(Long.valueOf(clientMidMap.get(cell.getStringCellValue().trim().toLowerCase())));
                                    userProfile.setClientGroupMid(accessRoles.getClientGroupMid());
                                }
                                userProfile.setRole(accessRoles);
                            } else if (columnMap.get(columnNo).contains("LIST-accountProfiles")) {
                                List<AccountProfile> accountProfiles = new ArrayList<>();
                                if (cell.getCellTypeEnum().equals(CellType.NUMERIC)) {
                                    AccountProfile accountProfile = new AccountProfile();
                                    accountProfile.setAccountNumber(String.valueOf(Double.valueOf(cell.getNumericCellValue()).longValue()));
                                    accountProfiles.add(accountProfile);
                                } else {
                                    if (cell.getStringCellValue().contains(",")) {
                                        List<String> accounts= Arrays.asList(cell.getStringCellValue().split(","));
                                        accounts.replaceAll(String::trim);
                                        for(String account: accounts) {
                                            AccountProfile accountProfile = new AccountProfile();
                                            accountProfile.setAccountNumber(account);
                                            accountProfiles.add(accountProfile);
                                        }
                                    } else {
                                        AccountProfile accountProfile = new AccountProfile();
                                        accountProfile.setAccountNumber(cell.getStringCellValue());
                                        accountProfiles.add(accountProfile);
                                    }
                                }

                                userProfile.setAccountProfiles(accountProfiles);
                            } else if (columnMap.get(columnNo).contains("Country")) {
                                clientMid = midMap.get(cell.getStringCellValue().trim().toLowerCase());
                            } else if (columnMap.get(columnNo).contains("displayName")) {
                                userProfile.setDisplayName(cell.getStringCellValue());
                            } else if (columnMap.get(columnNo).contains("emailAddress")) {
                                userProfile.setEmailAddress(cell.getStringCellValue());
                            } else if (columnMap.get(columnNo).contains("userId")) {
                                userProfile.setUserId(cell.getStringCellValue());
                            }

                            userProfile.setOtherPhone("");
                            userProfile.setMobilePhone("");
                            userProfile.setStatus("Active");
                        }
                    }
                    columnNo++;
                }
                if (!isHeader) {
                    if (userProfile.getAccountProfiles().isEmpty() ||
                            StringUtils.isEmpty(userProfile.getUserId()) ||
                            StringUtils.isEmpty(userProfile.getEmailAddress()) ||
                            userProfile.getRole() == null ||
                            userProfile.getRole().getClientGroupMid() == null ||
                            userProfile.getRole().getAccessRoleOid() == null ||
                            StringUtils.isEmpty(userProfile.getRole().getDescription()) ||
                            StringUtils.isEmpty(userProfile.getDisplayName()) ||
                            StringUtils.isEmpty(clientMid)) {
                        Row resultRow = resultWorkSheet.getRow(row.getRowNum());
                        Cell statusCell = resultRow.createCell(resultRow.getLastCellNum() + 1);
                        statusCell.setCellValue("Failure");
                        Cell errorMessageCell = row.createCell(row.getLastCellNum() + 1);
                        errorMessageCell.setCellValue("Mandatory values like - OLS User Role / Full Name / Email / Customer Number / Country / Username - are missing");
                        continue;
                    }
                    UserServiceTask userServiceTask = new UserServiceTask(gson.toJson(userProfile), clientMid, authToken, row.getRowNum());
                    Future<UserServiceResponse> result = executor.submit(userServiceTask);
                    resultList.add(result);
                }
                isHeader = false;
            }
            for (Future<UserServiceResponse> future : resultList) {
                try {
                    System.out.println("Tasks that are completed--->"+executor.getCompletedTaskCount());
                    System.out.println("Get Active Thread Count---->"+executor.getActiveCount());
                    System.out.println("Tasks pending for exeuction--->"+executor.getQueue().size());
                    UserServiceResponse userServiceResponse = future.get();
                    if (future.isDone() && userServiceResponse.getStatus() == HttpStatus.SC_OK) {
                        Row row = resultWorkSheet.getRow(userServiceResponse.getRowNumber());
                        Cell statusCell = row.createCell(row.getPhysicalNumberOfCells() + 1);
                        statusCell.setCellValue("Success");
                    } else {
                        Row row = resultWorkSheet.getRow(userServiceResponse.getRowNumber());
                        Cell statusCell = row.createCell(row.getPhysicalNumberOfCells() + 1);
                        statusCell.setCellValue("Failure");
                        Cell errorMessageCell = row.createCell(row.getPhysicalNumberOfCells() + 1);
                        errorMessageCell.setCellValue(userServiceResponse.getErrorMessage());
                    }
                    System.out.println("Future result is - " + " - " + userServiceResponse + "; And Task done is " + future.isDone());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    // not throwing exception in order to coninue with remaining rows processing
                }
            }
            //shut down the executor service now
            executor.shutdown();
            try (FileOutputStream outputStream = new FileOutputStream(outputfile)) {
                resultWorkBook.write(outputStream);
            } catch (Exception e) {
                throw new UserCreationException("Unable to Create the result file with the execution status details", e);
            }
        } catch (Exception e) {
            throw new UserCreationException("User Creation Process failed", e);
        }
    }

    public void sendUserRequest(String requestBodyParams, String clientMid) {
        request = RestAssured.given().config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().
                        appendDefaultContentCharsetToContentTypeIfUndefined(false))).header("Authorization", "Bearer " +
                        authToken).header("API-Version", "2.0.0")
                .header("Current-Client-Mid", clientMid)
                .contentType(ContentType.JSON).body(requestBodyParams).when();

        System.out.println("Request info::" + request.log().all());

        String userEndPointAPI = "/user";
        response = request.post(RestAssured.baseURI + userEndPointAPI);
//        response = postRequestAsBearerAuthWithBodyData(PropUtils.getPropValue(inputProp, "userEndPointAPI"), requestBodyParams,
//                PropUtils.getPropValue(inputProp, "AuthorizationToken"));
        if (response.prettyPrint().contains("error")) {
//            logger.log("--------------------------------------");
//            logger.log("response:" + response.prettyPrint());
        }
    }

    public static void main(String args[]) {
        try {
            UserCreation userCreation = new UserCreation();
            Properties configProperties = userCreation.loadConfigProperties(args.length > 0 ? args[1] : null);
            try {
                Map<String, String> clientMidMap = userCreation.getCountryLevelClientMids(configProperties);
                Map<String, String> roleMap = userCreation.getRoleOidByRoleDescription(configProperties);
                Map<String, String> cgMidMap = userCreation.getClientGroupMidsByRoleDescription(configProperties);
                userCreation.loginToApplication(configProperties);
                userCreation.createUsers(configProperties, clientMidMap, roleMap, cgMidMap);
            } catch (UserCreationException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}