package com.ohuang.filemanager.util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ExecUtil {

    public static class ExecManager {
        private final Process process;
        private final Thread[] threads;

        public ExecManager(Process process, Thread[] threads) {
            this.process = process;
            this.threads = threads;
        }

        public void stop() {
            process.destroy();
        }

        public void waitFor() {
            try {
                // 设置超时等待
                boolean exited = process.waitFor() == 1;
                for (int i = 0; i < threads.length; i++) {
                    threads[i].join();
                }

                process.destroy();
            } catch (Throwable e) {
                // Ignore
            }
        }
    }


    public interface OnProgress {
        void onProgress(String line);
    }


    public static ExecManager exec(String[] cmd, OnProgress log, OnProgress logError) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(cmd);

        // 异步读取标准输出
        Thread outThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (log != null) {
                        log.onProgress(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // 异步读取错误输出
        Thread errThread = new Thread(() -> {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    if (logError != null) {
                        logError.onProgress(errorLine);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        outThread.start();
        errThread.start();

        return new ExecManager(p, new Thread[]{outThread, errThread});


    }


}
