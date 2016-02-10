package com.craftingmod.mml.model;

import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;

/**
 * Created by superuser on 16/2/6.
 */
public class Melody {
    public NoteOn note;
    public Tempo tempo;
    public boolean isNote;
    public boolean connect;
    public long duration;
    public long eventTime;
    public Melody(NoteOn noteon,long end){
        note = noteon;
        duration = end - noteon.getTick();
        eventTime = noteon.getTick();
        isNote = true;
        connect = false;
    }
    public Melody(NoteOn noteon,long start,long end,boolean conti){
        this(noteon,start,end);
        connect = conti;
    }
    public Melody(NoteOn noteon,long start,long end){
        note = noteon;
        duration = end - start;
        eventTime = start;
        isNote = true;
        connect = false;
    }
    public Melody(Tempo t){
        tempo = t;
        eventTime = t.getTick();
        isNote = false;
        connect = false;
    }
}
