package com.anicks.googledriveupload;

import com.google.api.client.auth.oauth2.Credential;
import com.google.jenkins.plugins.credentials.domains.DomainRequirementProvider;
import com.google.jenkins.plugins.credentials.domains.RequiresDomain;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.google.common.base.Preconditions.checkNotNull;

@RequiresDomain(value = DriveScopeRequirement.class)
public final class GoogleDriveUploader extends Recorder {

    private final String credentialsId;
    private final String pattern;
    private final String driveLocation;

    @DataBoundConstructor
    public GoogleDriveUploader(String credentialsId, String pattern, String driveLocation) {
        this.credentialsId = checkNotNull(credentialsId);
        this.pattern = checkNotNull(pattern);
        this.driveLocation = checkNotNull(driveLocation);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        try {
            GoogleRobotCredentials credentials = GoogleRobotCredentials.getById(getCredentialsId());
            GoogleDriveManager driveManager = new GoogleDriveManager(authorize(credentials));

            String pattern = Util.replaceMacro(getPattern(), build.getEnvironment(listener));
            String workspace = build.getWorkspace().getRemote();
            String[] filesToUpload = listFiles(workspace, pattern);
            for (String file : filesToUpload) {
                listener.getLogger().println("Uploading file: " + file);
                driveManager.store(file, getDriveLocation());
            }
        } catch (GeneralSecurityException e) {
            build.setResult(Result.FAILURE);
            return false;
        }
        return true;
    }

    private String[] listFiles(String baseDir, String pattern) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(baseDir);
        scanner.setIncludes(new String[]{pattern});
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    private Credential authorize(GoogleRobotCredentials credentials) throws GeneralSecurityException {
        GoogleRobotCredentials googleRobotCredentials = credentials.forRemote(getRequirement());
        return googleRobotCredentials.getGoogleCredential(getRequirement());
    }

    private DriveScopeRequirement getRequirement() {
        return DomainRequirementProvider.of(getClass(), DriveScopeRequirement.class);
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getPattern() {
        return pattern;
    }

    public String getDriveLocation() {
        return driveLocation;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public String getDisplayName() {
            return "Upload to google Drive";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
    }
}
