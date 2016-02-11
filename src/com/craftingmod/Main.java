package com.craftingmod;

import com.craftingmod.gui.MainActivity;
import com.craftingmod.mml.Logger;
import com.craftingmod.mml.MmlCon;
import com.leff.midi.MidiFile;
import javafx.application.Application;

import java.io.File;
import java.io.IOException;

/**
 * Created by superuser on 16/2/5.
 */
public class Main {
    public static void main(String args[]){
        // Logger Log = new Logger(Main.class);
        //File input = new File("/Users/superuser/Documents/MuseScore2/Promise_of_Heaven.mid");
        // File input = new File("/Users/superuser/Documents/MuseScore2/014 - Heartache.mid");
        // File input = new File("/Users/superuser/Documents/MuseScore2/077 - Asgore.mid");

        Application.launch(MainActivity.class,args);

        /*
         File input = new File("/Users/superuser/Documents/MuseScore2/Revenge.mid");
        try {
            MidiFile midiFile = new MidiFile(input);
            MmlCon con = new MmlCon(midiFile);
            con.filterTrack();
            con.exportAll(input.getParentFile(),"test.ms2mml");
            for(int i=0;i<con.tracks.size();i+=1){
                con.scan(i);
            }
        } catch (IOException e) {
            Log.e(e);
        }
        */
    }
}
