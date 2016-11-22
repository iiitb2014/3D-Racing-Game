package mygame;
import ai.AICar;
import mygame.VehicleCamera;
import vehicles.*;
import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.bullet.BulletAppState;
import com.jme3.scene.CameraNode;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.shadow.BasicShadowRenderer;
import static com.jme3.bullet.PhysicsSpace.getPhysicsSpace;
import com.jme3.font.BitmapText;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.system.JmeContext;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.UtNetworking;
import network.UtNetworking.NetworkMessage;
import network.UtNetworking.PositionMessage;
import terrain.*;
public class Main extends SimpleApplication {
    private BulletAppState physicsEngine;
    private AICar bot;
    private Vehicle ferrari;
    LapManager lapManager;
    
    Client myClient;
    ConcurrentLinkedQueue<String> messageQueue;
    public static void main(String[] args) {
        UtNetworking.initialiseSerializables();
        Main app = new Main();
        app.start(JmeContext.Type.Display);
    }

    @Override
    public void simpleInitApp() {

        try {
            myClient = Network.connectToServer("localhost", 6143);
            myClient.start();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        messageQueue = new ConcurrentLinkedQueue<String>();
        myClient.addMessageListener(new NetworkMessageListener());
        physicsEngine = new BulletAppState();
        stateManager.attach(physicsEngine);
         if (settings.getRenderer().startsWith("LWJGL")) {
            BasicShadowRenderer bsr = new BasicShadowRenderer(assetManager, 512);
            bsr.setDirection(new Vector3f(-0.5f, -0.3f, -0.3f).normalizeLocal());
            viewPort.addProcessor(bsr);
        }
        cam.setFrustumFar(1000f);
        viewPort.setBackgroundColor(ColorRGBA.White);

        lapManager = new LapManager(new Vector3f(0.38055262f, 14.283572f, -25.188498f), 3);
        ferrari = new Ferrari (0.3f, new Vector3f(-19f, 18,-2f), 20f, 1000f,assetManager, ColorRGBA.Red);
        ferrari.initVehicle();
        VehicleControls Control= new VehicleControls("Car", ferrari ,2000f, inputManager);
        Control.setupKeys();
        bot = new AICar(0.5f, 2f, 1000f, assetManager);
        try {
            bot.initAICar();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Stage1 stage= new Stage1(new Vector3f(270f, -20f, 15f), 75f,assetManager);
        stage.init_stage1();
        
        VehicleCamera vcam = new VehicleCamera("Camera Node", new Vector3f(0f,4f,12f), new Vector3f(0f,22f,0f), cam);
        vcam.initCamera();
        ferrari.getCarNode().attachChild(vcam.getCamera());
        
        getPhysicsSpace().setGravity(new Vector3f(0, -20f, 0));
        
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.5f, -1f, -0.3f).normalizeLocal());
        rootNode.addLight(dl);

        rootNode.attachChild(ferrari.getCarNode());
        rootNode.attachChild(stage.get_Stage());
        rootNode.attachChild(bot.getCarNode());
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        String message = messageQueue.poll();
        if (message!=null) {
            fpsText.setText(message);
        } else {
            fpsText.setText("No Message");
        }
        
        listener.setLocation(cam.getLocation());
        listener.setRotation(cam.getRotation());
        lapManager.checkCompletion(ferrari.getCarNode().getLocalTranslation(), guiNode, guiFont, assetManager);
        
        //System.out.println(ferrari.getCarNode().getLocalTranslation().x + " " + ferrari.getCarNode().getLocalTranslation().y + " " + ferrari.getCarNode().getLocalTranslation().z);
        bot.AIUpdate();
    }
    
    @Override
    public void destroy() {
        myClient.close();
        super.destroy();
    }
    private class NetworkMessageListener implements MessageListener<Client> {

        @Override
        public void messageReceived(Client source, Message m) {
            if (m instanceof NetworkMessage) {
                NetworkMessage message = (NetworkMessage) m;
                messageQueue.add(message.getMessage());
            } else if (m instanceof PositionMessage) {
                final PositionMessage posMsg = (PositionMessage) m;
                Main.this.enqueue(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        ferrari.getCarNode().setLocalTranslation(posMsg.getPosition());
                        return null;
                    }
                });
            }
                
            }
        }
        
    }
}
