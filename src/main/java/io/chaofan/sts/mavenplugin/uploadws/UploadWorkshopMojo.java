package io.chaofan.sts.mavenplugin.uploadws;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "upload", defaultPhase = LifecyclePhase.DEPLOY)
public class UploadWorkshopMojo extends AbstractMojo {

    @Parameter(property = "stsInstallPath")
    public String stsInstallPath;

    @Parameter(property = "uploadModPath", required = true)
    public String uploadModPath;

    @Parameter(property = "targetJarPath", defaultValue = "target/${project.artifactId}.jar")
    public String targetJarPath;

    private Log log;
    private boolean isWindows;

    @Override
    public void execute() throws MojoFailureException {
        this.log = getLog();
        this.isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        log.info("Executing sts-upload-workshop:upload.");

        File modUploader = findModUploader();
        if (modUploader == null) {
            throw new MojoFailureException("Cannot find mod-uploader.jar.");
        }

        File targetJar = new File(targetJarPath);
        if (!targetJar.isFile()) {
            throw new MojoFailureException("targetJar not found: " + targetJarPath + ".");
        }

        File modPath = new File(uploadModPath);
        if (!modPath.isDirectory()) {
            throw new MojoFailureException("uploadModPath is not a directory.");
        }

        File modPathParent = modPath.getParentFile();
        File modPathContent = new File(modPath, "content");
        //noinspection ResultOfMethodCallIgnored
        modPathContent.mkdirs();

        try {
            log.info("Copying " + targetJarPath + " to " + modPathContent + ".");
            Files.copy(targetJar.toPath(), modPathContent.toPath().resolve(targetJar.getName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to copy " + targetJarPath + " to " + modPathContent + ".", e);
        }

        log.info("Running mod-uploader.jar");
        List<String> parameters = new ArrayList<>();
        parameters.add(new File(new File(System.getProperty("java.home")), "bin/java" + (isWindows ? ".exe" : "")).getPath());
        parameters.add("-Djava.awt.headless=true");
        parameters.add("-jar");
        parameters.add(modUploader.getPath());
        parameters.add("upload");
        parameters.add("-w");
        parameters.add(modPath.getName());

        runModUploader(parameters, modPathParent);

        log.info("Success!");
    }

    private File findModUploader() throws MojoFailureException {
        if (stsInstallPath == null) {
            if (isWindows) {
                log.info("stsInstallPath is null. Try C:\\Program Files (x86)\\Steam\\steamapps\\common\\SlayTheSpire.");
                stsInstallPath = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\SlayTheSpire";
            } else {
                throw new MojoFailureException("Please specify stsInstallPath.");
            }
        }

        File modUploader = new File(new File(stsInstallPath), "mod-uploader.jar");
        if (modUploader.isFile()) {
            return modUploader;
        }

        return null;
    }

    private void runModUploader(List<String> parameters, File workingDir) throws MojoFailureException {
        log.info("Run " + String.join(" ", parameters) + " at " + workingDir);

        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(parameters.toArray(new String[0]), null, workingDir);

            InputStream in = proc.getInputStream();
            InputStream err = proc.getErrorStream();
            BufferedReader inReader = new BufferedReader(new InputStreamReader(in));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(err));

            //noinspection Convert2Lambda
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        while ((line = inReader.readLine()) != null) {
                            log.info("[mod-uploader.jar] " + line);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }).start();

            //noinspection Convert2Lambda
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        while ((line = errReader.readLine()) != null) {
                            log.error("[mod-uploader.jar] " + line);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }).start();

            int exitValue = proc.waitFor();
            if (exitValue != 0) {
                throw new MojoFailureException("mod-uploader.jar exit with " + exitValue + ".");
            }

        } catch (IOException e) {
            throw new MojoFailureException("Failed to execute mod uploader.", e);

        } catch (InterruptedException e) {
            throw new MojoFailureException("Running mod uploader interrupted.", e);

        } finally {
            if (proc != null) {
                proc.destroy();
            }
        }
    }
}
