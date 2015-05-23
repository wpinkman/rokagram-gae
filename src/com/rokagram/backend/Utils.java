package com.rokagram.backend;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.rokagram.backend.velocity.VelocityService;

public class Utils {
    private static final Logger log = Logger.getLogger("rokagram");

    private static Boolean devServer = null;
    private static Boolean productionServer = null;
    private static String applicationId = SystemProperty.applicationId.get();
    private static final String USER_AGENT_REF_NAME = "browserUserAgent";

    private static VelocityService vs = null;// VelocityService.getInstance();

    public static boolean isDevServer() {
        if (devServer == null) {
            devServer = SystemProperty.environment.value() == SystemProperty.Environment.Value.Development;
        }
        return devServer;
    }

    public static boolean isProductionServer() {
        if (productionServer == null) {
            productionServer = SystemProperty.environment.value() == SystemProperty.Environment.Value.Production;
        }
        return productionServer;
    }

    public static void d(String dbgString) {
        if (isDevServer()) {
            System.out.println(dbgString);
        }
    }

    public static void info(String dbgString) {
        log.info(dbgString);
    }

    public static void warn(String dbgString) {
        log.warning(dbgString);
    }

    static void severe(String dbgString) {
        log.severe(dbgString);
    }

    public static String getApplicationId() {
        return applicationId;
    }

    public static String createRandomStateString() {
        return new BigInteger(130, new SecureRandom()).toString(32);
    }

    public static void resolveAndRender(String templateName, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Alert.checkAndInject(req);

        resolveAndRender(templateName, req, resp, false);
    }

    public static void resolveAndRender(String templateName, HttpServletRequest req, HttpServletResponse resp,
            boolean inject) throws IOException {

        Map<String, Object> velocityInect = extractVelocityInjectables(req);

        resolveAndRender(templateName, velocityInect, null, resp);

    }

    public static Map<String, Object> extractVelocityInjectables(HttpServletRequest req) {
        Map<String, Object> velocityInect = new HashMap<String, Object>();
        @SuppressWarnings("rawtypes")
        Enumeration attributeNames = req.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            Object o = attributeNames.nextElement();
            if (o instanceof String) {
                String name = (String) o;

                Object value = req.getAttribute(name);
                velocityInect.put(name, value);

            }

        }

        String userAgent = req.getHeader("User-Agent");
        if (!velocityInect.containsKey(USER_AGENT_REF_NAME)) {
            velocityInect.put(USER_AGENT_REF_NAME, userAgent);
        }
        return velocityInect;
    }

    public static void resolveAndRender(String templateName, Map<String, Object> velocityInject,
            Map<String, Object> jsonInject, HttpServletResponse resp) throws IOException {

        if (vs == null) {
            vs = VelocityService.getInstance();
        }
        Template t = vs.getTemplate(templateName + ".html");
        VelocityContext context = new VelocityContext();

        for (Entry<String, Object> entry : velocityInject.entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }

        // if (jsonInject != null) {
        // context.put("__json__", generateJsonObject(jsonInject));
        // }

        context.put("templateName", templateName);
        StringWriter writer = new StringWriter();
        t.merge(context, writer);

        // must be called before resp.getWriter()
        resp.setContentType("text/html; charset=UTF-8");

        PrintWriter out = resp.getWriter();
        String body = writer.toString();
        out.println(body);
        out.flush();
    }

    public static String createHtmlBody(String templateName, Map<String, Object> velocityInject) throws IOException {

        if (vs == null) {
            vs = VelocityService.getInstance();
        }
        Template t = vs.getTemplate(templateName + ".html");
        VelocityContext context = new VelocityContext();

        for (Entry<String, Object> entry : velocityInject.entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }

        context.put("templateName", templateName);
        StringWriter writer = new StringWriter();
        t.merge(context, writer);

        return writer.toString();
    }

    public static void sendAdminEmai(String subject, String body) {
        MailService mailService = MailServiceFactory.getMailService();
        MailService.Message message = new MailService.Message();
        message.setSender("andy@rokagram.com");

        message.setSubject(subject);
        message.setTextBody(body);

        message.setHtmlBody(body);
        try {
            mailService.sendToAdmins(message);

        } catch (IOException e) {
            Utils.warn("failed to send admin email");
        }

    }

}
