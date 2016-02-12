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
                        Log.d("Tempo " + bpm + " At " + event.getTick() + " removed due to similar tempo.");
                        events.remove(i);
                        i -= 1;
                    }else{
                        Tempo lpT = (Tempo) event;
                        if(Math.floor(lpT.getTick()/120)*120 != lpT.getTick()){
                            lpT = new Tempo((long)Math.round(lpT.getTick()/240)*240,-1,lpT.getMpqn());
                        }
                        tempoes.add(lpT);
                        Log.d("Tempo: " + bpm + " At " + lpT.getTick());
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
    private ArrayList<String> exportMS2(ArrayList<String> results){
        ArrayList<String> out = new ArrayList<>();
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
        return out;
    }
    private ArrayList<String> exportMML(ArrayList<String> results){
        ArrayList<String> out = new ArrayList<>();
        out.add("[Settings]");
        out.add("Encoding=UTF-8");
        out.add("Title=");
        out.add("Source=");
        out.add("Memo=");
        for (int i=0;i<results.size();i+=1) {
            out.add("[Channel" + (i+1) + "]");
            out.add(results.get(i).toLowerCase());
        }
        return out;
    }
    public int exportAll(File dir, String name){
        ArrayList<String> results = new ArrayList<>();
        ArrayList<Integer> sizes = new ArrayList<>();
        for (MidiTrack track : tracks) {
            MmlScan scanner = new MmlScan(track);
            scanner.upOctave = this.upOctave;
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
        ArrayList<String> mml = exportMML(results);
        ArrayList<String> ms2 = exportMS2(results);
        save(mml,dir,name + ".mml");
        save(ms2,dir,name + ".ms2mml");
        int totalSize = 0;
        for(String say : results){
            totalSize += say.length();
        }
        return totalSize;
    }
    private void save(ArrayList<String> saves,File dir,String fullFname){
        try{
            Path expFile = Paths.get(dir.getCanonicalPath() + "/" + fullFname);
            Files.deleteIfExists(expFile);
            Files.write(expFile,saves,StandardCharsets.UTF_8);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
