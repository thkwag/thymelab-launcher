package com.github.thkwag.thymelab.launcher.ui.dialogs;

import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ActuatorInfoDialog extends JDialog {
    private static final int TIMEOUT_SECONDS = 5;
    private static final int REFRESH_INTERVAL = 1000;
    private final JTextArea textArea;
    private final int port;
    private final Timer refreshTimer;

    public ActuatorInfoDialog(Frame owner, int port) {
        super(owner, "Server Health Status", true);
        this.port = port;
        
        setSize(500, 400);
        setLocationRelativeTo(owner);
        
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        textArea.setMargin(new Insets(10, 10, 10, 10));
        textArea.setText("Loading system information...");
        
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(panel);

        refreshTimer = new Timer(REFRESH_INTERVAL, e -> refreshInfo());
        refreshTimer.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                refreshTimer.stop();
            }
        });

        refreshInfo();
    }

    private void refreshInfo() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return loadSystemInfo();
            }

            @Override
            protected void done() {
                try {
                    String result = get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    textArea.setText(result);
                } catch (TimeoutException e) {
                    textArea.setText("Error: Timeout while loading system information");
                } catch (Exception e) {
                    textArea.setText("Error loading system information: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private String loadSystemInfo() {
        try {
            StringBuilder output = new StringBuilder();
            
            try {
                JSONObject healthJson = fetchJson("health");
                output.append("=== Health Status ===\n");
                output.append("Status: ").append(healthJson.getString("status")).append("\n");
            } catch (Exception e) {
                output.append("=== Health Status ===\nError: ").append(e.getMessage()).append("\n");
            }
            
            output.append("\n=== Memory Usage ===\n");
            try {
                JSONObject memoryJson = fetchJson("metrics/jvm.memory.used");
                double memoryUsed = memoryJson.getJSONArray("measurements").getJSONObject(0).getDouble("value");
                output.append(String.format("Memory Used: %.1f MB\n", memoryUsed/1024/1024));
            } catch (Exception e) {
                output.append("Error: ").append(e.getMessage()).append("\n");
            }
            
            output.append("\n=== CPU Usage ===\n");
            try {
                JSONObject cpuJson = fetchJson("metrics/process.cpu.usage");
                double cpuUsage = cpuJson.getJSONArray("measurements").getJSONObject(0).getDouble("value");
                output.append(String.format("Process CPU Usage: %.1f%%\n", cpuUsage * 100));
            } catch (Exception e) {
                output.append("Error: ").append(e.getMessage()).append("\n");
            }
            
            output.append("\n=== Threads ===\n");
            try {
                JSONObject threadsJson = fetchJson("metrics/jvm.threads.live");
                int threadCount = (int) threadsJson.getJSONArray("measurements").getJSONObject(0).getDouble("value");
                output.append(String.format("Live Threads: %d\n", threadCount));
            } catch (Exception e) {
                output.append("Error: ").append(e.getMessage()).append("\n");
            }
            
            return output.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private JSONObject fetchJson(String endpoint) throws IOException {
        URL url = new URL("http://localhost:" + port + "/actuator/" + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String response = reader.lines().collect(Collectors.joining("\n"));
            return new JSONObject(response);
        } finally {
            conn.disconnect();
        }
    }
} 