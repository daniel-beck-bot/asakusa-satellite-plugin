package org.codefirst;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.User;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.Mailer;
import hudson.scm.ChangeLogSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

public class AsakusaSatelliteNotifier extends Notifier {
    private String appkey;
    private String baseUrl;
    private String roomNumber;
    private String message;

    private static final Logger LOG = Logger.getLogger(AsakusaSatelliteNotifier.class);

    @DataBoundConstructor
    public AsakusaSatelliteNotifier(String appkey, String baseUrl, String roomNumber, String message) {
        this.appkey = appkey;
        this.baseUrl = baseUrl;
        this.roomNumber = roomNumber;
        this.message = message;
    }

    /**
     * @return the appkey
     */
    public String getAppkey() {
        return appkey;
    }

    /**
     * @return the baseUrl
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @return the roomNumber
     */
    public String getRoomNumber() {
        return roomNumber;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        String message = generatedMessage(build);
        String apiUrl = (baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/v1/message.json");
        String postData = "room_id=" + roomNumber + "&message=" + URLEncoder.encode(message, "UTF-8") + "&api_key=" + appkey;

        URL url = new URL(apiUrl);
        URLConnection connection = null;
        OutputStream os = null;
        OutputStreamWriter osw = null;
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        try {
            connection = url.openConnection();
            connection.setDoOutput(true);

            os = connection.getOutputStream();
            osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(postData);
            IOUtils.closeQuietly(osw);
            osw = null;
            IOUtils.closeQuietly(os);
            os = null;

            is = connection.getInputStream();
            isr = new InputStreamReader(is);
            reader = new BufferedReader(isr);
            String s;
            while ((s = reader.readLine()) != null) {
                // do nothing
            }
        } catch (IOException e) {
            // ignore because notification failures make build failures!
            LOG.error("[AsakusaSatellite] Failed to notification", e);
        } finally {
            IOUtils.closeQuietly(osw);
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(isr);
            IOUtils.closeQuietly(is);
            connection = null;
        }
        return true;
    }

    private String generatedMessage(AbstractBuild<?, ?> build) {
        StringBuilder userBuilder = new StringBuilder();
        for (User user : build.getCulprits()) {
            userBuilder.append(user.getFullName() + " ");
        }
        StringBuilder changeSetBuilder = new StringBuilder();
        for(ChangeLogSet.Entry entry : build.getChangeSet()) {
            changeSetBuilder.append(entry.getAuthor() + " : " + entry.getMsg() + "\n");
        }
        String replacedMessage = message.replace("${user}", userBuilder.toString());
        replacedMessage = replacedMessage.replace("${result}", build.getResult().toString());
        replacedMessage = replacedMessage.replace("${project}", build.getProject().getName());
        replacedMessage = replacedMessage.replace("${number}", String.valueOf(build.number));
        replacedMessage = replacedMessage.replace("${url}", Mailer.descriptor().getUrl() + build.getUrl());
        replacedMessage = replacedMessage.replace("${changeSet}", changeSetBuilder.toString());

        return replacedMessage;

    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/asakusa-satellite-plugin/AsakusaSatelliteNotifier.html";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> project) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "AsakusaSatellite";
        }
    }
}
