package com.craftingmod.gui;

import com.craftingmod.mml.Logger;
import com.craftingmod.mml.MmlCon;
import com.leff.midi.MidiFile;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

/**
 * Created by superuser on 16/2/11.
 */
public class MainActivity extends Application {
    private Logger Log;
    private Boolean upoctave;
    private File saveDir;
    @Override
    public void start(Stage stage) throws Exception {
        Log = new Logger(this.getClass());
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ui.fxml"));
        Parent root = loader.load();

        Preferences pref = Preferences.userNodeForPackage(this.getClass());

        stage.setTitle("midi");
        stage.setResizable(false);
        stage.setAlwaysOnTop(true);
        stage.setIconified(false);
        Scene scene = new Scene(root, 200, 238);
        final MainController controller = loader.getController();
        upoctave = pref.getBoolean("upoctave",false);
        if(!pref.get("savedir","null").equalsIgnoreCase("null")){
            saveDir = new File(pref.get("savedir",null));
            if(!saveDir.exists() || !saveDir.canWrite()){
                pref.remove("savedir");
                saveDir = null;
            }
        }

        controller.check.setSelected(upoctave);
        controller.check.selectedProperty().addListener((observable, oldValue, newValue) -> {
            upoctave = newValue;
            pref.putBoolean("upoctave",newValue);
        });
        controller.drag.setOnMouseClicked(event -> {
            pref.remove("savedir");
            saveDir = null;
        });

        scene.setOnDragOver(event -> {
            /* data is dragged over the target */
            //Log.d("onDragOver");
            /* accept it only if it is  not dragged from the same node
                 * and if it has a string data */
            if(event.getGestureSource() == scene){

            }else if(getMIDI(event.getDragboard()) != null){
                event.acceptTransferModes(TransferMode.COPY);
            }else if(getDIR(event.getDragboard()) != null){
                event.acceptTransferModes(TransferMode.LINK);
            }
            event.consume();
        });
        scene.setOnDragEntered(event -> {
            /* the drag-and-drop gesture entered the target */
            Log.d("onDragEntered");
            if(event.getGestureSource() == scene){

            }else if(getDIR(event.getDragboard()) != null){
                controller.folder.setVisible(true);
                controller.drag.setVisible(false);
            }else if(getMIDI(event.getDragboard()) != null){
                controller.music.setVisible(true);
                controller.drag.setVisible(false);
            }
            event.consume();
        });
        scene.setOnDragExited(event -> {
            /* mouse moved away, remove the graphical cues */
            controller.music.setVisible(false);
            controller.folder.setVisible(false);
            controller.drag.setVisible(true);
            event.consume();
        });
        scene.setOnDragDropped(event -> {
            final Dragboard drag = event.getDragboard();
            File midi = getMIDI(drag);
            File folder = getDIR(drag);
            if(event.getGestureSource() != scene){
                if(midi != null){
                    try{
                        MidiFile midiFile = new MidiFile(midi);
                        MmlCon con = new MmlCon(midiFile);
                        con.upOctave = upoctave;
                        con.filterTrack();
                        File save;
                        if(saveDir != null){
                            save = saveDir;
                        }else{
                            save = midi.getParentFile();
                        }
                        if(save.canWrite()){
                            int size = con.exportAll(save,midi.getName().replaceFirst("[.][^.]+$", ""));
                            controller.check.setText(size + " 글자");
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }else if(folder != null){
                    try {
                        saveDir = folder;
                        if(saveDir.canWrite()){
                            pref.put("savedir",saveDir.getCanonicalPath());
                        }else{
                            saveDir = null;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            /* let the source know whether the string was successfully
                 * transferred and used */
            event.setDropCompleted(true);
            event.consume();
        });
        stage.setScene(scene);
        stage.initStyle(StageStyle.DECORATED);

        stage.show();
    }
    private File getDIR(Dragboard drag){
        if(drag.hasFiles()){
            if(drag.getFiles().size() == 1){
                File file = drag.getFiles().get(0);
                if(file.isDirectory() && file.canWrite()){
                    return file;
                }
            }
        }
        return null;
    }

    private File getMIDI(Dragboard drag){
        if(drag.hasFiles()){
            if(drag.getFiles().size() == 1){
                File file = drag.getFiles().get(0);
                if(file.canRead() && (file.getName().endsWith(".mid") || file.getName().endsWith(".midi"))){
                    return file;
                }
            }
        }
        return null;
    }

}
