package com.craftingmod.mml;

import com.craftingmod.mml.model.Melody;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by superuser on 16/2/6.
 */
public class MmlSplit {
    protected HashMap<Integer,Integer> times;
    protected final int timeFor4 = 480; // 4분음표 틱.
    protected final int timeMin = timeFor4/16;
    protected Logger Log;
    protected MidiTrack track;

    protected ArrayList<HashMultimap<Long,Melody>> channels;
    private HashMultimap<Long,Melody> baseChannel;
    private ArrayList<Long> channelUsage;

    private HashMultimap<Long,Melody> tempo;

    public MmlSplit(MidiTrack tr){
        track = tr;
        Log = new Logger(this.getClass());
        tempo = HashMultimap.create();
        times = new HashMap<>();
        times.put(4,timeFor4);
        times.put(8,timeFor4/2);
        times.put(16,timeFor4/4);
        times.put(32,timeFor4/8);
        times.put(1,timeFor4*4);
        times.put(2,timeFor4*2);
    }
    private void putTempo(Tempo tmpo){
        //baseChannel.put(tmpo.getTick(),new Melody(tmpo));
        for(int i=0;i<channels.size();i+=1){
            channels.get(i).put(tmpo.getTick(),new Melody(tmpo));
        }
    }
    public void putTempoes(HashMap<Long,Tempo> t){
        tempo = HashMultimap.create();
        for (Map.Entry<Long,Tempo> entry : t.entrySet()) {
            Long key = entry.getKey();
            Tempo value = entry.getValue();
            Log.d("Tempo added " + key);
            tempo.put(value.getTick(),new Melody(value));
            //tempo.add(value);
        }
    }
    private HashMultimap<Long, Melody> mkChannel(){
        if(channels == null){
            channels = new ArrayList<>();
            channelUsage = new ArrayList<>();
            baseChannel = HashMultimap.create(tempo);
        }
        channels.add(HashMultimap.create(tempo));
        channelUsage.add(0L);
        final int pos = channels.size()-1;
        Log.d("Channel created at " + pos);
        return channels.get(pos);
    }
    protected final void split(){
        channels = null;
        ArrayList<MidiEvent> events = Lists.newArrayList(track.getEvents().iterator());
        if(tempo != null){
            for(int k=0;k<events.size();k+=1){
                if(events.get(k) instanceof Tempo){
                    Log.d("Remove");
                    events.remove(k);
                    k -= 1;
                }
            }
        }
        mkChannel();
        HashMultimap<Long,Melody> map = HashMultimap.create();
        /**
         * Duplicate tempo remover
         */


        for(int i=0;i<events.size();i+=1){
            MidiEvent event = events.get(i);
            NoteOn note = null;
            Tempo tempo = null;
            if(event instanceof NoteOn){
                note = (NoteOn) event;
                if(note.getVelocity() == 0){
                    continue;
                }
            }else if(event instanceof Tempo){
                tempo = (Tempo) event;
            }else{
                continue;
            }
            Melody melody;
            if(note != null){
                long endTime = -1;
                ArrayList<Long> lp_Atempo = new ArrayList<>();
                for(int k=i+1;k<events.size();k+=1){
                    if(events.get(k) instanceof  NoteOn){
                        NoteOn lp_note = (NoteOn) events.get(k);
                        if(lp_note.getNoteValue() == note.getNoteValue() && lp_note.getVelocity() == 0){
                            endTime = lp_note.getTick();
                            break;
                        }
                    }else if(events.get(k) instanceof NoteOff){
                        NoteOff lp_note = (NoteOff) events.get(k);
                        if(lp_note.getNoteValue() == note.getNoteValue()){
                            endTime = lp_note.getTick();
                            break;
                        }
                    }else if(events.get(k) instanceof Tempo){
                        Tempo lp_tempo = (Tempo) events.get(k);
                        lp_Atempo.add(lp_tempo.getTick());
                        //Log.d("Tempo found");
                        //break;
                    }
                }
                if(endTime == -1){
                    Log.e("Unknown endTime");
                    return;
                }
                HashMultimap<Long,Melody> channel = null;
                int posCh;
                for(posCh=0;posCh<channels.size();posCh+=1){
                    if(channelUsage.get(posCh) <= note.getTick()){
                        channel = channels.get(posCh);
                        break;
                    }
                    if(posCh == channels.size()-1){
                        channel = mkChannel();
                        posCh += 1;
                        break;
                    }
                }
                assert channel != null;
                boolean continuos = false;
                if(lp_Atempo.size() >= 1){
                    long lp_last = note.getTick();
                    for (Long lp_Ttempo : lp_Atempo) {
                        map.put(lp_last,new Melody(note,lp_last,lp_Ttempo,continuos));
                        continuos = true;
                        lp_last = lp_Ttempo;
                    }
                    channel.put(lp_last,new Melody(note,lp_last,endTime,continuos));
                }else{
                    melody = new Melody(note,endTime);
                    channel.put(melody.eventTime,melody);
                }
                channelUsage.set(posCh,endTime);
            }else{
                putTempo(tempo);
            }
        }
        /*
        Iterator<Long> it = channels.get(0).keySet().iterator();
        while(it.hasNext()){
            ArrayList<Melody> melodies = Lists.newArrayList(channels.get(0).get(it.next()));
            //Log.d(melodies.size());
        }
        */
    }
}
