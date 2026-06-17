package com.example.filemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class FileManagerApplication {

    public static void main(String[] args) {
        // 解析命令行参数
        for (String arg : args) {
            if (arg.startsWith("--path=") || arg.startsWith("--storage=")) {
                String path = arg.substring(arg.indexOf('=') + 1);
                if (!path.isEmpty()) {
                    ResPath.DownloadPath = path;
                }
            }
        }
        
        // 兼容旧版：第一个参数直接作为路径（不含 -- 开头时）
        if (args.length >= 1 && !args[0].startsWith("--")) {
            if (!args[0].isEmpty()) {
                ResPath.DownloadPath = args[0];
            }
        }
        
        // 确保存储目录存在
        File file = new File(ResPath.DownloadPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        
        System.out.println("文件存储路径: " + ResPath.DownloadPath);
        
        SpringApplication.run(FileManagerApplication.class, args);
    }

}
