package com.craftingmod.mml;

import com.google.common.collect.Lists;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by superuser on 16/2/5.
 */
public class MmlScan {
    private HashMap<Integer,Integer> times;
    protected final int timeFor4 = 480; // 4분음표 틱.
    protected final int timeMin = timeFor4/16;
    protected int defaultMelody = 8; //기본을 8분음표로 설정.

    private MidiTrack track;
    private ArrayList<MidiEvent> events;
    private Logger Log;
    private int maxV;

    private int lOctave;
    private int lVelo;
    private long lLength;
    private long lastTime;

    private ArrayList<String> out;

    public boolean upOctave = false;
    public MmlScan(MidiTrack tr,int maxVelocity){
        track = tr;
        Log = new Logger(this.getClass());
        times = new HashMap<>();
        times.put(4,timeFor4);
        times.put(8,timeFor4/2);
        times.put(1,timeFor4*4);
        maxV = maxVelocity;

        out = new ArrayList<>();
        lOctave = lVelo = -3;
        lLength = lastTime = 0L;
        scan();
    }
    private void channel(){

    }
    private void scan(){
        events = Lists.newArrayList(track.getEvents().iterator());
        int i = -1;
        for(MidiEvent event : events){
            i += 1;
            NoteOn note = null;
            Tempo tempo = null;
            long timeDelta = event.getTick() - lastTime;
            if(event instanceof NoteOn){
                note = (NoteOn) event;
            }else if(event instanceof NoteOff){
                NoteOff noff = (NoteOff) event;
                note = new NoteOn(noff.getTick(),noff.getChannel(),noff.getNoteValue(),0);
            }else if(event instanceof Tempo){
                tempo = (Tempo) event;
            }else{
                continue;
            }
            /**
             * Add Breaks
             */
            if(timeDelta > 0){
                Log.d("timeDelta: " + timeDelta);

            }else{

            }
            /**
             * Add Tempo
             */
            if(tempo != null){
                lastTime = tempo.getTick();
                return;
            }
            /* Power */
            final int power = getPower(note.getVelocity());
            if(power != lVelo){
                lVelo = power;
                out.add("V" + lVelo);
            }
            /* Octave */
            final int octave = getNativeOctave(note.getNoteValue()) + ((this.upOctave)?1:0);
            final String octaveString = getOctave(octave);
            if(octaveString != null){
                lOctave = octave;
                out.add(octaveString);
            }
            /* get end */
            long nextTime = -1;
            for(int c=i+1;c<events.size();c+=1){
                Object ev = events.get(c);
                if(ev instanceof NoteOn || ev instanceof NoteOff){
                    if(ev instanceof NoteOn){
                        NoteOn non = ((NoteOn)ev);
                        if(non.getNoteValue() == note.getNoteValue() && non.getVelocity() == 0){

                        }
                    }else{
                        NoteOff non = (NoteOff)ev;
                    }
                }
            }

        }
    }
    private int getPower(int pow){
        return (int)Math.min(15,Math.floor((pow/maxV)*15));
    }
    private String getOctave(int octaveI){
        int delta = octaveI - lOctave;
        if(delta == 1){
            return ">";
        }else if(delta == -1){
            return  "<";
        }else if(delta != 0){
            return  "O" + octaveI;
        }else{
            return null;
        }
    }
    private int getNativeOctave(int noteN){
        return (int) Math.floor(noteN/12);
    }
    private int getNote(int noteN){
        return (int) Math.floor(noteN%12);
    }
    private String getBreaks(long delta){

    }
}
