package com.craftingmod.mml;

import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by superuser on 16/2/5.
 */
public class MmlCon  {
    private MidiFile midi;
    private ArrayList<MidiTrack> tracks;

    private Logger Log;
    private int maxV;

    public Boolean upOctave = false;
    public MmlCon(MidiFile file)  {
        midi = file;
        maxV = 0;
        tracks = new ArrayList<>();
        Log = new Logger(this.getClass());
    }
    public void filterTrack(){
        for(int i=0;i<midi.getTrackCount();i+=1){
            MidiTrack track = midi.getTracks().get(i);
            Iterator<MidiEvent> it = track.getEvents().iterator();
            Boolean isPiano = false;
            while (it.hasNext()){
                MidiEvent event = it.next();
                if(event instanceof NoteOn){
                    NoteOn note = (NoteOn) event;
                    if(maxV < note.getVelocity()){
                        maxV = note.getVelocity();
                    }
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
        Log.d("MaxV: " + maxV);
    }
    public void scan(int channel){
        new MmlScan(tracks.get(channel),150);
        //MmlScan scanner = new MmlScan(tracks.get(channel));
    }
}
