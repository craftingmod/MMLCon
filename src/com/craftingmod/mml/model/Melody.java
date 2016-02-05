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
    public long duration;
    public long eventTime;
    public Melody(NoteOn noteon,long dura,long time){
        note = noteon;
        duration = dura;
        eventTime = time;
        isNote = true;
    }
    public Melody(Tempo t,long time){
        eventTime = time;
        tempo = t;
        isNote = false;
    }
}
