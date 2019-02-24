package team364_rpi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.cameraserver.CameraServer;

public class Camera {

    // Internals
    private String configFile = "";

    private static class CameraConfig {
        public String name = "";
        public String path = "";
        public JsonObject config = new JsonObject();
        public JsonElement streamConfig = new JsonElement(){
            @Override
            public JsonElement deepCopy() {
                return null;
            }
        };
    }
    private CameraConfig camConfig = new CameraConfig();

    // Constructors
    public Camera(String configFile) {
        if (!setConfigFile(configFile)) {
        }
    }

    // Internal methods
    private void parseError(String str) {
        System.err.println("config error in '" + configFile + "': " + str);
    }

    private boolean readCameraConfig() {

        // parse file
        JsonElement top;
        try {
        top = new JsonParser().parse(Files.newBufferedReader(Paths.get(configFile)));
        } catch (IOException ex) {
        System.err.println("could not open '" + configFile + "': " + ex);
        return false;
        }

        // top level must be an object
        if (!top.isJsonObject()) {
        parseError("must be JSON object");
        return false;
        }
        JsonObject obj = top.getAsJsonObject();
        
        // find the cameras
        JsonElement camerasElement = obj.get("cameras");
        if (camerasElement == null) {
        parseError("could not read cameras");
        return false;
        }
        JsonArray cameras = camerasElement.getAsJsonArray();

        if (cameras.size() == 0) {
            parseError("no cameras found in config file");
            return false;
        }
        JsonObject config = cameras.get(0).getAsJsonObject();
        CameraConfig cam = new CameraConfig();

        // name
        JsonElement nameElement = config.get("name");
        if (nameElement == null) {
            parseError("could not read camera name");
            return false;
        }
        cam.name = nameElement.getAsString();

        // path
        JsonElement pathElement = config.get("path");
        if (pathElement == null) {
            parseError("camera '" + cam.name + "': could not read path");
            return false;
        }
        cam.path = pathElement.getAsString();

        // stream properties
        cam.streamConfig = config.get("stream");

        // config
        cam.config = config;
       
        camConfig = cam;
        return true;
    }

    // Public methods
    /**
     * Sets path to config file and attempts to read it.
     * @param inConfig path to JSON config file. ex: "/home/pi/myconfig.json"
     * @return true if config file is valid, false for anything else
     */
    public boolean setConfigFile(String inConfig) {
        configFile = inConfig;
        if (!readCameraConfig()) {
            System.err.println("camera config invalid");
            return false;
        }
        return true;
    }
    
    /**
     * Starts capture on camera using read config file.
     * @return WPI VideoSource to be used in a VisionPipeline
     */
    public VideoSource startCamera() {
        System.out.println("Starting camera '" + camConfig.name + "' on " + camConfig.path);
        CameraServer inst = CameraServer.getInstance();
        UsbCamera camera = new UsbCamera(camConfig.name, camConfig.path);
        MjpegServer server = inst.startAutomaticCapture(camera);

        Gson gson = new GsonBuilder().create();

        camera.setConfigJson(gson.toJson(camConfig.config));
        camera.setConnectionStrategy(VideoSource.ConnectionStrategy.kKeepOpen);

        // Uncomment below to override config file
        //camera.setVideoMode(PixelFormat.kYUYV, 960, 720, 15); // 320x240(30), 1024x576(15), 640x480(30),
                                                                // 800x448(30/24/20/15/10), 352x288, 176x144, 160x120

        if (camConfig.streamConfig != null) {
            server.setConfigJson(gson.toJson(camConfig.streamConfig));
        }
        return camera;
    }

}