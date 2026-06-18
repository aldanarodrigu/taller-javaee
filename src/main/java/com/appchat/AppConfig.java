package com.appchat;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@MultipartConfig(maxFileSize = 5242880, maxRequestSize = 5242880)
@ApplicationPath("/api")
public class AppConfig extends Application {
}