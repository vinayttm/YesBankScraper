package com.example.testappjava.Services;

import static com.example.testappjava.Utils.AccessibilityUtil.*;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Rect;

import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.example.testappjava.MainActivity;
import com.example.testappjava.Repository.QueryUPIStatus;
import com.example.testappjava.Repository.SaveBankTransaction;
import com.example.testappjava.Repository.UpdateDateForScrapper;
import com.example.testappjava.Utils.AES;
import com.example.testappjava.Utils.CaptureTicker;
import com.example.testappjava.Utils.Config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class YesRecorderService extends AccessibilityService {
    int appNotOpenCounter = 0;
    boolean isTransaction = false;
    boolean scrollOnce = false;
    final CaptureTicker ticker = new CaptureTicker(this::processTickerEvent);
    boolean shouldLogout = false;

    private final Runnable logoutRunnable = () -> {
        Log.d("Logout Handler", "Finished");
        shouldLogout = true;

    };


    @Override
    protected void onServiceConnected() {
        ticker.startChecking();
        super.onServiceConnected();

    }

    private void processTickerEvent() {
        Log.d("Ticker", "Processing Event");
        Log.d("Flags", printAllFlags());
        ticker.setNotIdle();

        if (!MainActivity.isAccessibilityServiceEnabled(this, this.getClass())) {
            return;
        }
        AccessibilityNodeInfo rootNode = getTopMostParentNode(getRootInActiveWindow());
        if (rootNode != null) {
            if (findNodeByPackageName(rootNode, Config.packageName) == null) {
                if (appNotOpenCounter > 4) {
                    Log.d("App Status", "Not Found");
                    relaunchApp();
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    appNotOpenCounter = 0;
                    return;
                }
                appNotOpenCounter++;
            } else {
                rootNode.refresh();
                Log.d("App Status", "Found");
                checkForSessionExpiry();
                listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow()));
                enterPin();
                scrollOnce();
                myAccount();
                statement();
                readTransaction();
                backingProcess();
                rootNode.refresh();
            }
            rootNode.recycle();
        }
    }

    Handler logoutHandler = new Handler();

    private void logout() {
        ticker.setNotIdle();
        Log.d("Yes Bank", "Logout Stage = " + shouldLogout);
        if (shouldLogout) {
            if (!listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("SELECTED ACCOUNT")) {
                AccessibilityNodeInfo nodeInfo = findNodeByResourceId(getTopMostParentNode(getRootInActiveWindow()), "testID_HeaderContainer_RightIcon_TouchableIcon");
                if (nodeInfo != null) {
                    Rect digitBounds = new Rect();
                    nodeInfo.getBoundsInScreen(digitBounds);
                    performTap(digitBounds.centerX(), digitBounds.centerY());
                    nodeInfo.recycle();
                }
            }
        } else {
            System.out.println("shouldLogout " + shouldLogout);
        }
    }


    private void relaunchApp() {
        // Might fail not tested
        if (MainActivity.isAccessibilityServiceEnabled(this, this.getClass())) {
            new QueryUPIStatus(() -> {
                Intent intent = getPackageManager().getLaunchIntentForPackage(Config.packageName);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }, () -> {
                Toast.makeText(this, "Scrapper inactive", Toast.LENGTH_SHORT).show();
            }).evaluate();
        }
    }

    private boolean appReopened = false;

    private void closeAndOpenApp() {
        // Close the current app
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        Intent intent = getPackageManager().getLaunchIntentForPackage(Config.packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Log.e("AccessibilityService", "App not found: " + Config.packageName);
        }


    }


    boolean isLogin = false;
    boolean clickOneTime = false;

    public void enterPin() {
        ticker.setNotIdle();
        isLogin = true;
        SharedPreferences sharedPreferences = getSharedPreferences(Config.packageName, MODE_PRIVATE);
        Config.loginPin = sharedPreferences.getString("loginPin", "");
        String pinText = Config.loginPin.trim();
        System.out.println("PinText = " + Config.loginPin);
        if (!pinText.isEmpty()) {
//            logoutHandler.removeCallbacks(logoutRunnable);
//            logoutHandler.postDelayed(logoutRunnable, 1000 * 60 * 2);
            ticker.setNotIdle();
            AccessibilityNodeInfo enterPinString = findNodeByText(getRootInActiveWindow(), "Enter your MPIN to Login", false, false);
            if (enterPinString != null) {
                performTap(95, 613);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ticker.setNotIdle();
                AccessibilityNodeInfo one = findNodeByText(getRootInActiveWindow(), "1", true, false);
                AccessibilityNodeInfo two = findNodeByText(getRootInActiveWindow(), "2", true, false);
                AccessibilityNodeInfo three = findNodeByText(getRootInActiveWindow(), "3", true, false);
                AccessibilityNodeInfo four = findNodeByText(getRootInActiveWindow(), "4", true, false);
                AccessibilityNodeInfo five = findNodeByText(getRootInActiveWindow(), "5", true, false);
                AccessibilityNodeInfo six = findNodeByText(getRootInActiveWindow(), "6", true, false);
                AccessibilityNodeInfo seven = findNodeByText(getRootInActiveWindow(), "7", true, false);
                AccessibilityNodeInfo eight = findNodeByText(getRootInActiveWindow(), "8", true, false);
                AccessibilityNodeInfo nine = findNodeByText(getRootInActiveWindow(), "9", true, false);
                AccessibilityNodeInfo zero = findNodeByText(getRootInActiveWindow(), "0", true, false);
                if (one != null && two != null && three != null && four != null && five != null && six != null && seven != null && eight != null && nine != null && zero != null) {
                    for (int i = 0; i < pinText.length(); i++) {
                        char currentChar = pinText.charAt(i);
                        AccessibilityNodeInfo getNumbers = findNodeByText(getRootInActiveWindow(), String.valueOf(currentChar), true, false);
                        if (getNumbers != null) {
                            if (getNumbers.getText().toString().equals(String.valueOf(currentChar))) {
                                System.out.println("Numbers " + getNumbers.getText());
                                Rect digitBounds = new Rect();
                                getNumbers.getBoundsInScreen(digitBounds);
                                performTap(digitBounds.centerX(), digitBounds.centerY());
                                ticker.setNotIdle();
                            }
                        } else {
                            System.out.println("All Clicked not found !");
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    enterPinString.recycle();
                    isLogin = true;
                }
            }
        }
    }


    private void scrollOnce() {
        ticker.setNotIdle();
        if (listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("ACCOUNTS")) {
            //findNodeByText(getRootInActiveWindow(), String.valueOf(currentChar), true, false);
            AccessibilityNodeInfo scrollNode = findNodeByContentDescription(getTopMostParentNode(getRootInActiveWindow()), "testID_DashboardContainer_ScrollView");
            if (scrollNode != null) {
                if (scrollOnce) return;
                Rect scrollBounds = new Rect();
                scrollNode.getBoundsInScreen(scrollBounds);
                Log.d("ScrollBounds", "Top: " + scrollBounds.top + ", Bottom: " + scrollBounds.bottom);
                int startX = scrollBounds.centerX();
                int startY = scrollBounds.centerY();
                int endX = startX;
                int scrollDistance = 150;
                int endY = startY - scrollDistance;

                Log.d("SwipeGesture", "StartX: " + startX + ", StartY: " + startY + ", EndX: " + endX + ", EndY: " + endY);

                Path path = new Path();
                path.moveTo(startX, startY);
                path.lineTo(endX, endY);

                GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
                gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));

                dispatchGesture(gestureBuilder.build(), null, null);

                scrollNode.recycle();
                scrollOnce = true;
            }
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public void myAccount() {
        checkForSessionExpiry();
        ticker.setNotIdle();
        if (scrollOnce) {
            if (listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("Account\nServices") && listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("My\nAccount")) {
                performTap(122, 811);
            }
        }
    }

    private void backingProcess() {
        if (isTransaction) {
            AccessibilityNodeInfo nodeInfo = findNodeByContentDescription(getTopMostParentNode(getRootInActiveWindow()), "testID_HeaderContainer_PressLeftButton");
            if (nodeInfo != null) {
                Rect digitBounds = new Rect();
                nodeInfo.getBoundsInScreen(digitBounds);
                performTap(digitBounds.centerX(), digitBounds.centerY());
                nodeInfo.recycle();
                isTransaction = false;
                scrollStatement = false;
                ticker.setNotIdle();
            }
        }
    }

    boolean scrollStatement = false;

    public void statement() {
        if (scrollStatement) return;
        if (listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("SELECTED ACCOUNT")) {
            AccessibilityNodeInfo scrollNode = findNodeByContentDescription(getTopMostParentNode(getRootInActiveWindow()), "testID_AccountOverviewDetailScreen_AccountOverviewDetailContent");
            if (scrollNode != null) {
                Rect scrollBounds = new Rect();
                scrollNode.getBoundsInScreen(scrollBounds);
                Log.d("ScrollBounds", "Top: " + scrollBounds.top + ", Bottom: " + scrollBounds.bottom);
                int startX = scrollBounds.centerX();
                int startY = scrollBounds.centerY();
                int endX = startX;
                int scrollDistance = 250;
                int endY = startY - scrollDistance;
                Log.d("SwipeGesture", "StartX: " + startX + ", StartY: " + startY + ", EndX: " + endX + ", EndY: " + endY);
                Path path = new Path();
                path.moveTo(startX, startY);
                path.lineTo(endX, endY);
                GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
                gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
                dispatchGesture(gestureBuilder.build(), null, null);
                scrollNode.recycle();
                scrollStatement = true;
            }
        }
    }

    int transaction = -1;
    int totalBalanceIndex = -1;

    public void readTransaction() {
        JSONArray output = new JSONArray();

        if (listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("SELECTED ACCOUNT")) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String totalBalance = "";
            for (int i = 0; i < listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).size(); i++) {
                if (listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).get(i).contains("Available balance ") || listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).get(i).contains("Available balance")) {
                    totalBalanceIndex = i + 1;
                    totalBalance = listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).get(totalBalanceIndex);
                    totalBalance = totalBalance.replace("₹", "").trim();
                    break;
                }
            }
            System.out.println("Current Balance " + totalBalance);
            if (totalBalance.isEmpty()) {
                return;
            }

            for (int i = 0; i < listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).size(); i++) {
                if (listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).get(i).equals("RECENT TRANSACTIONS")) {
                    transaction = i;
                    System.out.println("Found Closing Balance at index: " + transaction);
                    break;
                }
            }
            if (listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("RECENT TRANSACTIONS")) {
                List<String> extractedData = listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).subList(transaction, listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).size());
                extractedData.removeIf(String::isEmpty);
                for (int i = 0; i < extractedData.size(); i++) {
                    extractedData.set(i, extractedData.get(i).trim());
                }
                extractedData.removeIf(e -> e.contains("RECENT TRANSACTIONS"));
                extractedData.removeIf(e -> e.contains("testID_AccountOverviewDetailScreen_ViewDetailedStatement_Button"));
                extractedData.removeIf(e -> e.contains("View Detailed Statement"));
                Log.d("extractedData", extractedData.toString());
                for (int i = 0; i < extractedData.size(); i += 3) {
                    JSONObject jsonObject = new JSONObject();
                    String date = extractedData.get(i);
                    if (isDate(date)) {
                        String createdDate = extractedData.get(i);
                        String amount = "";
                        String description = extractedData.get(i + 2);
                        if (getUPIId(description).equals("")) {
                            amount = extractedData.get(i + 1);
                            amount = "-" + amount.replace("₹", "");
                        } else {
                            amount = extractedData.get(i + 1);
                            amount = amount.replace("₹", "");
                        }
                        try {
                            jsonObject.put("Description", extractUTRFromDesc(description));
                            jsonObject.put("UPIId", getUPIId(description));
                            jsonObject.put("CreatedDate", createdDate);
                            jsonObject.put("Amount", amount.trim());
                            jsonObject.put("RefNumber", extractUTRFromDesc(description));
                            jsonObject.put("AccountBalance", totalBalance);
                            jsonObject.put("BankName", Config.bankName + Config.bankLoginId);
                            jsonObject.put("BankLoginId", Config.bankLoginId);
                            output.put(jsonObject);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                Log.d("OutputData", output.toString());
                Log.d("Final Json Output", output.toString());
                Log.d("API BODY", output.toString());
                Log.d("API BODY Length", String.valueOf(output.length()));
                if (output.length() > 0) {
                    JSONObject result = new JSONObject();
                    try {
                        result.put("Result", AES.encrypt(output.toString()));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    new QueryUPIStatus(() -> {
                        new SaveBankTransaction(() -> {
                        }, () -> {
                        }).evaluate(result.toString());
                        new UpdateDateForScrapper().evaluate();

                    }, () -> {
                    }).evaluate();
                    isTransaction = true;
                }
            }
        }
    }

    public static boolean isDate(String input) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false); // This will not allow invalid dates like February 30th

        try {
            dateFormat.parse(input);
            return true; // Parsing succeeded, so it's a valid date
        } catch (ParseException e) {
            return false; // Parsing failed, so it's not a valid date
        }
    }


    private String printAllFlags() {
        StringBuilder result = new StringBuilder();
        // Get the fields of the class
        Field[] fields = getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            try {
                Object value = field.get(this);
                result.append(fieldName).append(": ").append(value).append("\n");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return result.toString();
    }

    public boolean performTap(int x, int y) {
        Log.d("Accessibility", "Tapping " + x + " and " + y);
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(p, 0, 950));

        GestureDescription gestureDescription = gestureBuilder.build();

        boolean dispatchResult = false;
        dispatchResult = dispatchGesture(gestureDescription, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
            }
        }, null);
        Log.d("Dispatch Result", String.valueOf(dispatchResult));
        return dispatchResult;
    }


    private String getUPIId(String description) {
        try {
            if (!description.contains("@")) return "";
            String[] split = description.split("/");
            String value = null;
            value = Arrays.stream(split).filter(x -> x.contains("@")).findFirst().orElse(null);
            value = value != null ? value.replace("From:", "") : "";
            return value;
        } catch (Exception ex) {
            Log.d("Exception", ex.getMessage());
            return "";
        }
    }

    private String extractUTRFromDesc(String description) {
        try {
            String[] split = description.split("/");
            String value = null;
            value = Arrays.stream(split).filter(x -> x.length() == 12).findFirst().orElse(null);
            if (value != null) {
                return value + " " + description;
            }
            return description;
        } catch (Exception e) {
            return description;
        }
    }

    // Unused AccessibilityService Callbacks
    @Override
    public void onInterrupt() {

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }


    public void checkForSessionExpiry() {
        ticker.setNotIdle();
        AccessibilityNodeInfo targetNode1 = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "We are unable to perform the required action and request you to try again. Inconvenience regretted", true, false);
        AccessibilityNodeInfo targetNode2 = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Please login again to continue", true, false);
        AccessibilityNodeInfo targetNode3 = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Are you sure you want to logout?", true, false);
        //Are you sure you want to exit?
        AccessibilityNodeInfo targetNode4 = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Are you sure you want to exit?", true, false);
        if (targetNode1 != null) {

            AccessibilityNodeInfo yesButton = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Retry", true, false);
            if (yesButton != null) {
                Rect outBounds = new Rect();
                yesButton.getBoundsInScreen(outBounds);
                performTap(outBounds.centerX(), outBounds.centerY());
                yesButton.recycle();
                ticker.setNotIdle();
            }
        }
        if (targetNode2 != null) {
            AccessibilityNodeInfo yesButton = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Login Again", true, false);
            if (yesButton != null) {
                Rect outBounds = new Rect();
                yesButton.getBoundsInScreen(outBounds);
                performTap(outBounds.centerX(), outBounds.centerY());
                isTransaction = false;
                scrollOnce = false;
                isLogin = false;
                transaction = -1;
                totalBalanceIndex = -1;
                yesButton.recycle();
                appReopened = false;
                closeAndOpenApp();
                ticker.setNotIdle();
            }
        }
        if (targetNode3 != null) {
            AccessibilityNodeInfo btn = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Yes, Logout", true, false);
            if (btn != null) {
                Rect outBounds = new Rect();
                btn.getBoundsInScreen(outBounds);
                performTap(outBounds.centerX(), outBounds.centerY());
                isTransaction = false;
                scrollOnce = false;
                isLogin = false;
                transaction = -1;
                totalBalanceIndex = -1;
                appReopened = false;
                closeAndOpenApp();
                btn.recycle();
                ticker.setNotIdle();
            }
        }
        if (targetNode4 != null) {
            AccessibilityNodeInfo btn = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "OK", true, false);
            if (btn != null) {
                Rect outBounds = new Rect();
                btn.getBoundsInScreen(outBounds);
                performTap(outBounds.centerX(), outBounds.centerY());
                isTransaction = false;
                scrollOnce = false;
                isLogin = false;
                transaction = -1;
                totalBalanceIndex = -1;
                btn.recycle();
                ticker.setNotIdle();
            }
        }
    }
}
