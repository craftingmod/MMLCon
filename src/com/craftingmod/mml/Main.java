package com.craftingmod.mml;

import com.craftingmod.mml.model.Melody;
import com.leff.midi.MidiFile;
import com.leff.midi.event.NoteOn;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Created by superuser on 16/2/5.
 */
public class Main {
    public static void main(String args[]){
        Logger Log = new Logger(Main.class);
        File input = new File("/Users/superuser/Documents/MuseScore2/100 - Megalovania.mid");
        // File input = new File("/Users/superuser/Documents/MuseScore2/098 - Battle Against a True Hero.mid");
        // File input = new File("/Users/superuser/Documents/MuseScore2/Undertale Midi File/077 - Asgore.mid");
        // File input = new File("/Users/superuser/Documents/MuseScore2/Undertale Midi File/098 - Battle Against a True Hero.mid");
        try {
            MidiFile midiFile = new MidiFile(input);
            MmlCon con = new MmlCon(midiFile);
            con.filterTrack();
            con.exportAll(input.getParentFile(),"test.ms2mml");
            /*
            for(int i=0;i<con.tracks.size();i+=1){
                con.scan(i);
            }
            */
        } catch (IOException e) {
            Log.e(e);
        }
    }
}
