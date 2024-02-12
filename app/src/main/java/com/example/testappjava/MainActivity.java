package com.example.testappjava;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.example.YesBankScraper.R;
import com.example.testappjava.Repository.QueryUPIStatus;
import com.example.testappjava.Services.YesRecorderService;
import com.example.testappjava.Utils.Config;
import com.example.testappjava.Utils.SharedData;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private EditText editText1, editText2, editText3;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!isAccessibilityServiceEnabled(this, YesRecorderService.class)) {
            showAccessibilityDialog();
        }

        Intent serviceIntent = new Intent(this, YesRecorderService.class);
        startService(serviceIntent);

        editText1 = findViewById(R.id.editText1);
        editText2 = findViewById(R.id.editText2);
        editText3 = findViewById(R.id.editText3);

        sharedPreferences = getSharedPreferences(Config.packageName, MODE_PRIVATE);
        editText1.setText(sharedPreferences.getString("loginId", ""));
        editText2.setText(sharedPreferences.getString("loginPin", ""));
        editText3.setText(sharedPreferences.getString("bankLoginId", ""));

    }

    public void onAppFlowStarted(View view) {
        String text1 = editText1.getText().toString().trim();
        String text2 = editText2.getText().toString().trim();
        String text3 = editText3.getText().toString().trim();

        if (text1.isEmpty() || text2.isEmpty()) {
            Toast.makeText(this, "Both text fields must be filled.", Toast.LENGTH_SHORT).show();
            return;
        }

        Config.loginId = text1;
        Config.loginPin = text2;
        Config.bankLoginId = text3;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("loginId", text1);
        editor.putString("loginPin", text2);
        editor.putString("bankLoginId", text3);
        editor.apply();

        new QueryUPIStatus(() -> {

            SharedData.startedChecking = true;
            Intent intent = getPackageManager().getLaunchIntentForPackage(Config.packageName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                SharedData.startedChecking = true;
                runOnUiThread(() -> {
                    Intent serviceIntent;
                    if (!isAccessibilityServiceEnabled(this, YesRecorderService.class)) {
                        showAccessibilityDialog();
                    } else {
                        serviceIntent = new Intent(this, YesRecorderService.class);
                        startService(serviceIntent);
                    }
                });

            }, 1000);
        }, () -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Scrapper inactive", Toast.LENGTH_LONG).show();
            });
        }).evaluate();


    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> serviceClass) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null) {
            List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
            for (AccessibilityServiceInfo service : enabledServices) {
                ComponentName enabledServiceComponentName = new ComponentName(service.getResolveInfo().serviceInfo.packageName, service.getResolveInfo().serviceInfo.name);
                ComponentName expectedServiceComponentName = new ComponentName(context, serviceClass);
                if (enabledServiceComponentName.equals(expectedServiceComponentName)) {
                    Log.d("App", "Application has accessibility permissions");
                    return true;
                }
            }
        }
        Log.d("App", "Application does not have accessibility permissions");
        return false;
    }

    private void showAccessibilityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Accessibility Permission Required");
        builder.setMessage("To use this app, you need to enable Accessibility Service. Go to Settings to enable it?");
        builder.setPositiveButton("Settings", (dialog, which) -> openAccessibilitySettings());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        builder.show();
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    public List<AccessibilityNodeInfo> getNodesWithContentDescriptions(AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> nodesWithContentDescriptions = new ArrayList<>();

        if (rootNode == null) {
            return nodesWithContentDescriptions;
        }

        // Traverse the tree depth-first
        traverseTreeForContentDescriptions(rootNode, nodesWithContentDescriptions);

        return nodesWithContentDescriptions;
    }

    private void traverseTreeForContentDescriptions(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> nodesWithContentDescriptions) {
        if (node == null) {
            return;
        }

        // If the node has a non-empty content description, add it to the list
        CharSequence contentDescription = node.getContentDescription();
        if (!TextUtils.isEmpty(contentDescription)) {
            nodesWithContentDescriptions.add(node);
        }
        // Recursively traverse child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            traverseTreeForContentDescriptions(childNode, nodesWithContentDescriptions);
        }
    }

}