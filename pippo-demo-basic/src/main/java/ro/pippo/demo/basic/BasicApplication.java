/*
 * Copyright (C) 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ro.pippo.demo.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.Application;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.route.DirectoryHandler;
import ro.pippo.core.route.RouteContext;
import ro.pippo.core.route.RouteHandler;
import ro.pippo.demo.common.Contact;
import ro.pippo.metrics.Metered;

import java.io.File;

/**
 * @author Decebal Suiu
 */
public class BasicApplication extends Application {

    private final static Logger log = LoggerFactory.getLogger(BasicApplication.class);

    @Override
    protected void onInit() {
        getRouter().ignorePaths("/favicon.ico");

        // send files from a local folder (try a request like 'src/main/java/ro/pippo/demo/basic/BasicApplication.java')
        addFileResourceRoute("/src", "src");

        // serve a local folder (try a request like 'workdir/plain')
        DirectoryHandler directoryHandler = new DirectoryHandler("/workdir/plain", new File(""));
        GET(directoryHandler.getUriPattern(), directoryHandler);

        // serve a local folder (try a request like 'workdir/template')
        DirectoryHandler directoryHandler2 = new DirectoryHandler("/workdir/template", new File(""))
            .setDirectoryTemplate("files.ftl");
        GET(directoryHandler2.getUriPattern(), directoryHandler2);

        // send 'Hello World' as response
        GET("/", routeContext -> routeContext.send("Hello World")).named("Hello World handler");

        // send a file as response
        GET("/file", routeContext -> routeContext.send(new File("pom.xml")));

        // send a json as response
        GET("/json", routeContext -> {
            Contact contact = new Contact()
                .setId(12345)
                .setName("John")
                .setPhone("0733434435")
                .setAddress("Sunflower Street, No. 6");
            // you can use variant 1 or 2
//                response.contentType(HttpConstants.ContentType.APPLICATION_JSON); // 1
//                response.send(new Gson().toJson(contact)); // 1
            routeContext.json().send(contact); // 2
        });

        // send xml as response
        GET("/xml", routeContext -> {
            Contact contact = new Contact()
                .setId(12345)
                .setName("John")
                .setPhone("0733434435")
                .setAddress("Sunflower Street, No. 6");
            // you can use variant 1 or 2
//                response.contentType(HttpConstants.ContentType.APPLICATION_XML); // 1
//                response.send(new Xstream().toXML(contact)); // 1
            routeContext.xml().send(contact); // 2
        });

        // send text as response
        GET("/text", routeContext -> {
            Contact contact = new Contact()
                .setId(12345)
                .setName("John")
                .setPhone("0733434435")
                .setAddress("Sunflower Street, No. 6");
            routeContext.text().send(contact); // 2
        });

        // send an object and negotiate the Response content-type, default to XML
        GET("/negotiate", routeContext -> {
            Contact contact = new Contact()
                .setId(12345)
                .setName("John")
                .setPhone("0733434435")
                .setAddress("Sunflower Street, No. 6");
            routeContext.xml().negotiateContentType().send(contact);
        });

        // send an object, negotiate the Response content-type (default to XML), but allow override by suffix
        GET("/contact/{id: [0-9]+}(\\.(json|xml|yaml))?", routeContext -> {
            Contact contact = new Contact()
                .setId(routeContext.getParameter("id").toInt())
                .setName("Test Contact")
                .setPhone("0123456789")
                .setAddress("3rd Rock from the Sun");
            routeContext.xml().negotiateContentType().send(contact);
        });

        // send a template as response
        GET("/template", new RouteHandler() {

            @Metered
            @Override
            public void handle(RouteContext routeContext) {
                String message;

                String lang = routeContext.getParameter("lang").toString();
                if (lang == null) {
                    message = getMessages().get("pippo.greeting", routeContext);
                } else {
                    message = getMessages().get("pippo.greeting", lang);
                }

                routeContext.setLocal("greeting", message);
                routeContext.render("hello");
            }

        });

        GET("/issue-306", routeContext -> {
            routeContext.text().send("»»»»»1234567890");
        });

        // send an error as response
        GET("/error(\\.(json|xml|yaml))?", routeContext -> {
            int statusCode = routeContext.getParameter("code").toInt(HttpConstants.StatusCode.INTERNAL_ERROR);
            // do not commit the response
            // this delegates response representation to PippoFilter
            routeContext.status(statusCode);
        });

        // throw a programmatically exception
        GET("/exception", routeContext -> {
            throw new RuntimeException("My programmatically error");
        });

        // throw an exception that gets handled by a registered ExceptionHandler
        GET("/whoops", routeContext -> {
            throw new ForbiddenException("You didn't say the magic word!");
        });

        // use a finally filter (invoked even when exceptions were raised in previous routes)
        // test with route "/" and "/exception"
        ANY("/.*", routeContext -> log.info(">>> Cleanup here"))
            .named("cleanup filter")
            .runAsFinally();

        // register a custom ExceptionHandler
        getErrorHandler().setExceptionHandler(ForbiddenException.class, (e, routeContext) -> {
            log.info("Called custom exception handler");
            routeContext.setLocal("message", e.getMessage());
            getErrorHandler().handle(403, routeContext);
        });

        // register a request logger
        getRoutePreDispatchListeners().add((request, response) -> {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            System.out.println("requestPath = " + request.getPath());
            System.out.println("requestUri = " + request.getUri());
            System.out.println("requestUrl = " + request.getUrl());
            System.out.println("contextPath = " + request.getContextPath());
            System.out.println("applicationPath = " + request.getApplicationPath());
            System.out.println("applicationUri = " + request.getApplicationUri());
            System.out.println("applicationUriWithQuery = " + request.getApplicationUriWithQuery());
            System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        });
    }

    public static class ForbiddenException extends RuntimeException {

        public ForbiddenException(String message) {
            super(message);
        }

    }

}
