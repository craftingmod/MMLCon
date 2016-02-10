package com.craftingmod.mml.model;

import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;

/**
 * Created by superuser on 16/2/7.
 */
public class SimpleMelody {
    protected String[] NOTES = new String[]{"C","C+","D","D+","E","F","F+","G","G+","A","A+","B"};
    protected String[] NOTES_DISPLAY = new String[]{"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};

    public boolean isTempo;
    public float bpm;

    public boolean isBreak;
    public boolean isBridge;
    public int octave;
    public int note;
    public int code;
    public int power;
    public long duration;
    public long eventTime;

    public SimpleMelody(Melody melody){
        if(melody.isNote){
            construct(melody.note,melody.duration,melody.connect,melody.eventTime);
        }else{
            construct(melody.tempo,melody.eventTime);
        }
    }
    public SimpleMelody(long Length,long eTime){
        isTempo = false;
        isBreak = true;
        isBridge = false;
        eventTime = eTime;
        duration = Length;
    }
    private void construct(Tempo temp,long et){
        isTempo = true;
        isBreak = false;
        isBridge = false;
        bpm = temp.getBpm();
        eventTime = et;
    }
    private void construct(NoteOn _note,long du,boolean isB,long eTime){
        isTempo = false;
        isBreak = false;
        code = _note.getNoteValue();
        octave = getNativeOctave(code);
        note = getNote(code);
        duration = du;
        isBridge = isB;
        eventTime = eTime;
        power = _note.getVelocity();
    }
    public void upTave(){
        octave += 1;
    }
    private int getNativeOctave(int noteN){
        return (int) Math.floor(noteN/12);
    }
    private int getNote(int noteN){
        return (int) Math.floor(noteN%12);
    }
    public String getNote(){
        if(isTempo){
            return null;
        }else{
            return NOTES_DISPLAY[note];
        }
    }
    public String getNativeNote(){
        if(isTempo){
            return null;
        }else{
            return NOTES[note];
        }
    }

    @Override
    public String toString() {
        if(!isTempo && !isBreak){
            return "{type:Tempo, octave:" + octave + ", note:" + NOTES_DISPLAY[note] +
                    ", time:"+eventTime + ", duration:" + duration + "connection:" + ((isBridge)?"true":"false") + "}";
        }
        if(isBreak){
            return "{type:Break, time:"+eventTime + ", duration:" + duration + "}";
        }
        return "{type:Tempo, time:"+eventTime + ", bpm:" + bpm + "}";
    }
}
