package com.craftingmod.mml;

import com.leff.midi.MidiFile;

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
        try {
            MidiFile midiFile = new MidiFile(input);
            MmlCon con = new MmlCon(midiFile);
            con.filterTrack();
            con.scan(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
