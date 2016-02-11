package com.craftingmod.mml;

import com.craftingmod.mml.model.Melody;
import com.craftingmod.mml.model.SimpleMelody;
import com.google.common.base.Joiner;
import com.google.common.collect.*;
import com.google.common.primitives.Longs;
import com.google.gson.GsonBuilder;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;
import com.sun.istack.internal.Nullable;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.*;

/**
 * Created by superuser on 16/2/5.
 */
public class MmlScan extends MmlSplit{
    protected int defaultMelody = 8; //기본을 8분음표로 설정.

    private ArrayList<String> out;
    private ArrayList<Integer> size;

    /**
     * Channel
     */

    public boolean upOctave = false;
    public boolean reduceV = true;
    public MmlScan(MidiTrack tr){
        super(tr);
    }
    public void scanTracks(){
        out = Lists.newArrayList();
        size = Lists.newArrayList();
        split();
        for (HashMultimap<Long, Melody> channel : channels) {
            String scan = scan(channel);
            out.add(scan);
            size.add(scan.length());
        }
    }
    public ArrayList<String> getResults(){
        return out;
    }
    public ArrayList<Integer> getSizes(){
        return size;
    }
    private String scan(HashMultimap<Long,Melody> channel){
        long lastTime = 0L;
        Ordering<Long> sortKeys = new Ordering<Long>() {
            @Override
            public int compare(Long aLong, Long t1) {
                return Longs.compare(aLong,t1);
            }
        };

        ArrayList<SimpleMelody> aligned = Lists.newArrayList(); // output
        ArrayList<Long> keys = Lists.newArrayList(channel.keySet().iterator()); // eventTime keys
        Collections.sort(keys, sortKeys); // sort

        Iterator<Long> it = channels.get(0).keySet().iterator();
        while(it.hasNext()){
            ArrayList<Melody> melodies = Lists.newArrayList(channels.get(0).get(it.next()));
            for(Melody mld : melodies){
                if(!mld.isNote){
                    Log.d("Melodies: " + g.toJson(mld));
                }
            }
        }

        ArrayList<Melody> melodies;
        for(int i=0;i<keys.size();i+=1){
            melodies = Lists.newArrayList(channel.get(keys.get(i)));
            Melody melody;
            Melody m_Tempo = null;
            Melody m_Listen = null;
            for (Melody lp_mld : melodies) {
                if (!lp_mld.isNote) {
                    m_Tempo = lp_mld;
                    //output.add("T" + (int) lp_mld.tempo.getBpm());
                } else {
                    m_Listen = lp_mld;
                }
            }
            if(m_Tempo != null){
                // There is a tempo
                melody = m_Tempo;
            }else{
                if(m_Listen != null){
                    melody = m_Listen;
                }else{
                    continue;
                }
            }
            if(melody.eventTime - lastTime > 0){
                aligned.add(new SimpleMelody(melody.eventTime- lastTime, lastTime));
                Log.d("Pushed break. " + lastTime + " / " + melody.eventTime);
            }
            if(!melody.isNote){
                Log.d("SELECT TEMPO " + melody.tempo.getBpm() + " AT " + melody.eventTime);
                aligned.add(new SimpleMelody(melody));
                lastTime = melody.eventTime;
                channel.get(keys.get(i)).remove(melody);
                i -= 1;
                continue;
            }
            Melody nextMelody = null;
            for(int k=i+1;k<keys.size();k+=1){
                Boolean lp_found = false;
                melodies = Lists.newArrayList(channel.get(keys.get(k)));
                for(Melody lp_mldK : melodies){
                    /*
                   if(lp_mldK.isNote){
                       nextMelody = lp_mldK;
                       break;
                   }else{
                   }
                   */
                    nextMelody = lp_mldK;
                    break;
                }
                if(nextMelody != null){
                    break;
                }
            }
            if(nextMelody != null){
                long totalDu = nextMelody.eventTime - melody.eventTime;
                long afterDu = totalDu - melody.duration;
                long origDu = melody.duration;
                Log.d("Start At " + melody.eventTime);
                Log.d("Original duration: " + melody.duration + " break: " + afterDu);
                if(Math.floor(divide(totalDu,30)) != divide(totalDu,30)){
                    Log.d("Tick: " + melody.note.getTick());
                    Log.d("NextTick: " + nextMelody.note.getTick());
                    if(afterDu > times.get(8)){
                        melody.duration = (long)Math.ceil(divide(melody.duration,times.get(8)))*times.get(8);
                    }else{
                        melody.duration = totalDu;
                    }
                }else if(afterDu > Math.min(times.get(4),melody.duration/8)){
                    if(afterDu > times.get(8)){
                        melody.duration = reduceDuration(melody.duration,afterDu,times.get(8));
                    }else{
                        melody.duration = reduceDuration(melody.duration,afterDu,timeMin);
                    }
                    if(melody.duration - origDu > origDu/4 || melody.duration < 0){
                        long reduceN;
                        if(origDu > times.get(2)){
                            reduceN = times.get(8);
                        }else if(origDu > times.get(4)){
                            reduceN = times.get(16);
                        }else{
                            reduceN = times.get(32);
                        }
                        melody.duration = (long) Math.ceil(divide(origDu,reduceN))*reduceN;
                    }
                }else{
                    melody.duration = totalDu;
                }
                long breaks = totalDu-melody.duration;
                SimpleMelody sMel = new SimpleMelody(melody);
                sMel.octave -= 1;
                if(upOctave){
                    sMel.octave += 1;
                }
                aligned.add(sMel);
                Log.d("Melody " + melody.duration);
                if(breaks > 0){
                    Log.d("Breaks " + breaks);
                    aligned.add(new SimpleMelody(breaks,melody.eventTime+melody.duration));
                }
                lastTime = melody.eventTime + melody.duration + breaks;
                Log.d("Total " + totalDu + "  LastTime " + lastTime);
                //Log.d("LastTime: " + lastTime);
                //Log.d((nextMelody.eventTime - melody.eventTime) + " / " + melody.duration + " / "+ (nextMelody.eventTime - melody.eventTime-melody.duration));
                Log.line();
            }else{
                melody.duration = (long) Math.ceil(divide(melody.duration,times.get(4))) * times.get(4);
                Log.d("LastMelody du: " + melody.duration);
                SimpleMelody sMel = new SimpleMelody(melody);
                if(upOctave){
                    sMel.octave += 1;
                }
                aligned.add(sMel);
            }
        }
        ArrayList<SimpleMelody> visualMelodies = Lists.newArrayList();
        long checkTime = 0L;
        long sigmaN = 0L; // 횟수
        long sigmaV = 0L; // E(X)
        long sigmaV2 = 0L; // E(X^2)
        /**
         * Check wrong timing
         * and get V(X)
         */
        for (SimpleMelody melody : aligned) {
            if(checkTime != melody.eventTime){
                Log.e("Time Exception: Think " + checkTime + " But " + melody.eventTime + "!");
                return null;
            }else{
                checkTime = checkTime + melody.duration;
            }
            if(!melody.isTempo){
                visualMelodies.add(melody);
            }
            if(!melody.isTempo && !melody.isBreak && !melody.isBridge){
                sigmaV2 += (long) Math.pow(melody.power,2);
                sigmaV += melody.power;
                sigmaN += 1;
            }
            if(melody.isTempo){
                Log.d("Tempo Event " + melody.bpm);
            }else if(melody.isBreak){
                Log.d("Break Event " + melody.duration);
            }else{
                Log.d("Melody Event " + melody.duration + " octave " + melody.octave + " power " + melody.power + " note " + melody.getNote());
            }
        }
        // 1.04 : 35%
        int average = (int) Math.round(sigmaV/sigmaN + Math.sqrt(sigmaV2/sigmaN - Math.pow(sigmaV/sigmaN,2))*1.04);
        Log.d("상위 85%: " + average);

        /* ------------------------------------------ */

        int lastOctave = -3;
        int lastVelo = -3;
        long lastDuration = 0;
        ArrayList<String> out = Lists.newArrayList();
        int i = 0;
        for(SimpleMelody melody : aligned){
            /**
             * Tempo
             */
            if(melody.isTempo){
                out.add("T" + (int)Math.floor(melody.bpm));
                continue;
            }

            SimpleMelody nextMelody = null;
            SimpleMelody nextElement = null;
            int j = i+1;
            while (j<visualMelodies.size()-1){
                SimpleMelody me = visualMelodies.get(j);
                if(nextElement == null && !me.isTempo){
                    nextElement = me;
                }
                if(!me.isTempo && !me.isBreak && !me.isBridge){
                    nextMelody = me;
                    break;
                }
                j += 1;
            }
            if(nextElement != null){
                Log.d("SimpleDu: " + getSimpleDuration(melody.duration) + " / last: " + lastDuration + " / nextDu: " + nextElement.duration);
            }

            if(getSimpleDuration(melody.duration) != lastDuration){ // 간단하게 나타낸 길이가 최근 길이랑 다르면
                if(getSingleLength(melody.duration) != null){ // 간단하게 음표를 표시할수 있으면
                    while(nextMelody != null){
                        String duS = getSimpleLength(nextElement.duration); // 간단하게 줄인 문자
                        if(duS != null && duS.equalsIgnoreCase(getSimpleLength(melody.duration))){ // 다음 음의 간단문자랑 일치하면
                            out.add("L" + duS); // 간단문자 추가
                            lastDuration = getSimpleDuration(melody.duration);
                        }
                        break;
                    }
                }
            }
            // 도트 필요 여부
            boolean reqireDot = getSimpleDuration(melody.duration) == lastDuration && melody.duration != lastDuration;

            //Breaks
            if(melody.isBreak){
                String append = getLoopDuration(melody.duration,"R",lastDuration,true,null);
                if(append == null){
                    Log.e("추가할게 없음.");
                    continue;
                }
                if(append.startsWith("L") && getSimpleLength(lastDuration) != null){
                    lastDuration = times.get(4)*6;
                }
                out.add(append);
                i += 1;
                continue;
            }
            // No Break.
            //Octave
            if(lastOctave != melody.octave){
                int melodyD = melody.octave - lastOctave;
                if(melodyD == 1){
                    out.add(">");
                }else if(melodyD == -1){
                    out.add("<");
                }else{
                    out.add("O" + melody.octave);
                }
                lastOctave = melody.octave;
            }
            //Velocity
            melody.power = Math.max(0,5+Math.min(10,Math.round(divide(melody.power,average)*10)));
            if(lastVelo != melody.power){
                Log.d("Velocity: " + melody.power);
                if(!reduceV || Math.abs(lastVelo - melody.power) >= 4){
                    out.add("V" + melody.power);
                }
                lastVelo = melody.power;
            }
            String append = getLoopDuration(melody.duration,melody.getNativeNote(),lastDuration,false,out);
            if(append == null){
                Log.e("추가할게 없음.");
                continue;
            }
            if(append.startsWith("L")){
                lastDuration = times.get(4)*6;
            }
            if(melody.isBridge){
                append = "&" + append;
            }
            out.add(append);

            i += 1;
        }
        Log.d("Result");
        return Joiner.on("").join(out);
    }
    /**
     * 애매한 공백과 길이를 그럭저럭 원래 의도대로 돌려놓는 역할
     * @param orgDu 음의 길이
     * @param orgBreak 쉼표길이
     * @param reducer 쪼갤것.
     * @return 바뀐 음의 길이
     */
    private long reduceDuration(long orgDu,long orgBreak,int reducer){
        final long lengthAll = orgDu + orgBreak;
        final long original_Duration = orgDu;
        final long original_Break = orgBreak;
        while(true){
            orgDu += 1;
            orgBreak -= 1;
            if(orgDu <= 0 || orgBreak <= 0){
                Log.e("Reduce FALLED.\n" + original_Duration + " / " + original_Break);
                break;
            }
            long target;
            long dest;
            if(orgDu > orgBreak){
                target = orgDu;
                dest = orgBreak;
            }else if(orgDu == orgBreak){
                return orgDu;
            }else{
                target = orgBreak;
                dest = orgDu;
            }
            if((simpleDivide(orgDu,orgBreak) || simpleDivide(orgBreak,orgDu)) && simpleDivide(target,reducer)){
                return orgDu;
            }
        }
        return -1;
    }
    private boolean simpleDivide(long num,long pNum){
        return Math.floor(divide(num,pNum)) == divide(num,pNum);
    }
    private float divide(long num,int pNum){
        return ((float)num)/pNum;
    }
    private float divide(long num,long pNum){
        return ((float)num)/pNum;
    }
    private int getNativeOctave(int noteN){
        return (int) Math.floor(noteN/12);
    }
    private int getNote(int noteN){
        return (int) Math.floor(noteN%12);
    }

    @Nullable
    private String getSingleLength(long du){
        if(du <= 0){
            return null;
        }
        float divideC = divide(times.get(1),du);
        if(Math.floor(divideC) == divideC){
            return "" + (long)divideC;
        }
        du = du/3*2;
        float divideDot = divide(times.get(1),du);
        if(Math.floor(divideDot) == divideDot){
            return (long)divideDot + ".";
        }
        return null;
    }
    private String getSimpleLength(long du){
        return getSingleLength(getSimpleDuration(du));
    }
    private long getSimpleDuration(long du){
        if(simpleDivide(times.get(1),du)){
            return du;
        }
        if(simpleDivide(times.get(1),du/3*2)){
            return du/3*2;
        }
        return du;
    }
    private String getLoopDuration(long du,String note,long defaultD,boolean useB,ArrayList<String> ino){
        String out = getSingleLength(du);
        if(out == null){
            // require loop
            ArrayList<String> in = new ArrayList<>();
            long leftover = du;
            int add4 = 0;
            String[] sts = new String[]{
                    "1","2.","2","4.","4","8.","8","16.","16","32.","32"
            };
            double[] dus = new double[]{
                    4,3,2,1.5,1,0.75,0.5,0.375,0.25,0.1875,0.125
            };
            while(getSingleLength(leftover) == null){
                String addS = null;
                if(leftover > times.get(4)*6){
                    add4 += 1;
                    addS = "1.";
                    leftover = leftover - times.get(4)*6;
                }else{
                    for(int i=0;i<dus.length;i+=1){
                        if(leftover > times.get(4)*dus[i]){
                            addS = sts[i];
                            leftover = leftover - (long)(times.get(4)*dus[i]);
                            break;
                        }
                    }
                }
                if(addS == null){
                    Log.e("getLoopDuration FALLED.");
                    return null;
                }
                in.add(note + addS);
            }
            in.add(note + getSingleLength(leftover));
            String result = "";
            if(add4 >= 2){
                if(ino != null){
                    ino.add("L" + getSingleLength(times.get(4)*6));
                }else{
                    result += "L" + getSingleLength(times.get(4)*6);
                }
                defaultD = times.get(4)*6;
            }
            String result_lp;
            String replace_lp;
            if(useB) {
                replace_lp = "";
            }else {
                replace_lp = "&";
            }
            result_lp = Joiner.on(replace_lp).join(in);
            if(getSingleLength(defaultD) != null){
                result_lp = result_lp.replace(getSingleLength(defaultD),"");
            }
            result += result_lp;

            return result;
        }else{
            // don't require loop
            out = note + out;
            if(getSingleLength(defaultD) != null){
                out = out.replace(getSingleLength(defaultD),"");
            }
            return out;
        }
    }
}
