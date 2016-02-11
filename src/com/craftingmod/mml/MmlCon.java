package com.craftingmod.mml;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by superuser on 16/2/5.
 */
public class MmlCon  {
    private MidiFile midi;
    public ArrayList<MidiTrack> tracks;

    private ArrayList<Tempo> tempoes;

    private Logger Log;

    public Boolean upOctave = false;
    public MmlCon(MidiFile file)  {
        midi = file;
        tracks = new ArrayList<>();
        Log = new Logger(this.getClass());
    }
    public void filterTrack(){
        scanTempo();
        for(int i=0;i<midi.getTrackCount();i+=1){
            MidiTrack track = midi.getTracks().get(i);
            Iterator<MidiEvent> it = track.getEvents().iterator();
            Boolean isPiano = false;
            while (it.hasNext()){
                MidiEvent event = it.next();
                if(event instanceof NoteOn){
                    isPiano = true;
                }else if(event instanceof NoteOff){
                    isPiano = true;
                }
            }
            if(isPiano){
                Log.d("Channel " + i + " Added");
                tracks.add(track);
            }
        }
    }
    public void scanTempo(){
        //HashMap<Long,Tempo> tempo = new HashMap<>();
        tempoes = new ArrayList<>();
        for(int k=0;k<midi.getTrackCount();k+=1){
            MidiTrack track = midi.getTracks().get(k);
            Iterator<MidiEvent> it = track.getEvents().iterator();
            ArrayList<MidiEvent> events = Lists.newArrayList(track.getEvents().iterator());
            int lastBPM = -1;
            boolean tempoExists = false;
            for(int i=0;i<events.size();i+=1){
                MidiEvent event = events.get(i);
                if(event instanceof Tempo){
                    int bpm =  Math.round(((Tempo) event).getBpm());
                    // lastBPM/20 -> Ex: 120 -> 6 / 180 -> 9 / 60 -> 3
                    if(Math.abs(lastBPM - bpm) < (lastBPM/15) && lastBPM != -1){
                        Log.d("Tempo " + bpm + " removed due to similar tempo.");
                        events.remove(i);
                        i -= 1;
                    }else{
                        Tempo lpT = (Tempo) event;
                        tempoes.add(lpT);
                        Log.d("Tempo: " + bpm);
                        lastBPM = bpm;
                        tempoExists = true;
                    }
                }
            }
            if(tempoExists){
                break;
            }
        }
    }
    public void exportAll(File dir, String name){
        ArrayList<String> out = new ArrayList<>();
        ArrayList<String> results = new ArrayList<>();
        ArrayList<Integer> sizes = new ArrayList<>();
        for (MidiTrack track : tracks) {
            MmlScan scanner = new MmlScan(track);
            scanner.putTempo(tempoes);
            scanner.scanTracks();
            ArrayList<String> outs = scanner.getResults();
            for (int j=0;j<outs.size();j+=1) {
                String outlp = outs.get(j);
                results.add(outlp);
                sizes.add(scanner.getSizes().get(j));
            }
        }
        Ordering<String> sortKeys = new Ordering<String>() {
            @Override
            public int compare(String s, String t1) {
                if(s.length() > t1.length()){
                    return 1;
                }else if(s.length() == t1.length()){
                    return 0;
                }else{
                    return -1;
                }
            }
        };
        Collections.sort(results, sortKeys); // sort
        Collections.reverse(results);

        for(int size : sizes){
            Log.i("Size: " + size);
        }
        out.add("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        out.add("<ms2>");
        if(results.size() >= 1){
            out.add("<melody>");
            out.add("<![CDATA[");
            out.add(results.get(0));
            out.add("]]>");
            out.add("</melody>");
        }
        for (int i=1;i<results.size();i+=1) {
            out.add("<chord index=\"" + i + "\">");
            out.add("<![CDATA[");
            out.add(results.get(i));
            out.add("]]>");
            out.add("</chord>");
        }
        out.add("</ms2>");
        try{
            Path expFile = Paths.get(dir.getCanonicalPath() + "/" + name);
            Files.deleteIfExists(expFile);
            Files.write(expFile,out,StandardCharsets.UTF_8);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void scan(int channel){
        //MmlScan scanner = new MmlScan(tracks.get(channel));
    }
}
