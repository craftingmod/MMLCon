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
    public boolean useNormalV = false;
    public boolean sustain = true;
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
                    try {
                        Log.d("Tick: " + melody.note.getTick());
                        Log.d("NextTick: " + nextMelody.note.getTick());
                    }catch (Exception e){
                    }
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
         * Module: Reducer
         * to fix 761/399 like exception
         */
        for(int i=0;i<aligned.size()-1;i+=1){
            SimpleMelody melody = aligned.get(i);
            SimpleMelody nextMelody = null;
            SimpleMelody nextTempo = null;
            int k=i+1;
            int mk = -1;
            int tk = -1;
            if(melody.isTempo){
                continue;
            }
            while(k < aligned.size()){
                if(!aligned.get(k).isTempo){
                    if(nextMelody == null){
                        nextMelody = aligned.get(k);
                        mk = k;
                    }
                }else{
                    if(nextTempo == null){
                        nextTempo = aligned.get(k);
                        tk = k;
                    }
                }
                if(nextMelody != null && nextTempo != null) break;
                k += 1;
            }
            if(nextMelody == null){
                break;
            }
            long sigma = melody.duration+nextMelody.duration;
            if(!simpleDivide(melody.duration,10) && !simpleDivide(nextMelody.duration,10) &&
                    simpleDivide(sigma,30)){
                long rDuration = reduceDuration(melody.duration,nextMelody.duration,times.get(16));
                long aDuration = sigma-rDuration;
                if(rDuration - melody.duration > melody.duration/10){
                    rDuration = Math.round(melody.duration/timeMin)*timeMin;
                    aDuration = sigma-rDuration;
                }
                Log.d("Reducer : " + melody.duration + " / " + nextMelody.duration);
                melody.duration = rDuration;
                nextMelody.eventTime = melody.eventTime + rDuration;
                nextMelody.duration = aDuration;
                if(nextTempo != null && nextTempo.eventTime == nextMelody.eventTime){
                    Log.d("Reducer-Tempo: " + nextTempo.eventTime);
                    nextTempo.eventTime = nextMelody.eventTime;
                    aligned.set(tk,nextTempo);
                }
                aligned.set(i,melody);
                aligned.set(mk,nextMelody);
                Log.d("Reducer : " + rDuration + " / " + aDuration);
                i = mk;
            }
        }

        /**
         * Check wrong timing
         * and get V(X)
         */

        for (SimpleMelody melody : aligned) {
            Log.d("Duration",""+melody.duration);
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
        long lastDuration = -3;
        ArrayList<String> out = Lists.newArrayList();
        int i = 0;
        for(SimpleMelody melody : aligned){
            /**
             * Tempo
             */
            if(melody.isTempo){
                // 32 ~ 255 가 한계
                int reduceTempo = Math.round(Math.max(32,Math.min(255,melody.bpm)));
                out.add("T" + reduceTempo);
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
            if(melody.duration >= times.get(4)*12 && lastDuration != times.get(4)*6){
                lastDuration = times.get(4)*6;
                out.add("L1.");
            }else if(nextElement != null && lastDuration != melody.duration && getSingleLength(melody.duration) != null) {
                if(nextElement.duration == melody.duration){
                    // match next melody
                    lastDuration = melody.duration;
                    out.add("L" + getSingleLength(lastDuration));
                }else if(getSimpleDuration(melody.duration) == lastDuration){
                    //Just use .
                }else if(lastDuration != getSimpleDuration(melody.duration) && getSimpleDuration(melody.duration) == getSimpleDuration(nextElement.duration)){
                    // bridged with . ? X
                    lastDuration = getSimpleDuration(melody.duration);
                    out.add("L" + getSimpleLength(melody.duration));
                }
            }
            // 도트 필요 여부
            boolean reqireDot = getSimpleDuration(melody.duration) == lastDuration && melody.duration != lastDuration;

            //Breaks
            if(melody.isBreak){
                String append = this.getBreakDuration(melody.duration,lastDuration);
                if(append == null){
                    Log.e("추가할게 없음.");
                    continue;
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
            if(!useNormalV){
                melody.power = Math.max(0,7+Math.min(8,Math.round(divide(melody.power,average)*8)));
            }else{
                melody.power = Math.max(0,5+Math.min(10,Math.round(divide(melody.power,160)*10)));
            }
            if(lastVelo != melody.power){
                Log.d("Velocity: " + melody.power);
                if(!reduceV || Math.abs(lastVelo - melody.power) >= 4){
                    out.add("V" + melody.power);
                }
                lastVelo = melody.power;
            }
            String append = getMelodyDuration(melody.duration,melody.getNativeNote(),lastDuration);
            if(append == null){
                Log.e("추가할게 없음.");
                continue;
            }
            /*
            if(melody.isBridge){
                append = "&" + append;
            }
            */
            Log.d("Append",append);
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
    private String getMelodyDuration(long duration,String note,long defaultDuration){
        return getLoopDuration(duration,note,defaultDuration,false);
    }
    private String getBreakDuration(long duration,long defaultDuration){
        return getLoopDuration(duration,"R",defaultDuration,true);
    }
    private String getLoopDuration(long du,String note,long defaultD,boolean useB){
        String out = getSingleLength(du);
        ArrayList<String> in = new ArrayList<>();
        if(out == null){
            // 간단히 표시가 안될 때
            // require loop
            long leftover = du; // 루프용 수
            String[] sts = new String[]{
                    "1.","1","2.","2","4.","4","8.","8","16.","16","32.","32"
            };
            double[] dus = new double[]{
                    6,4,3,2,1.5,1,0.75,0.5,0.375,0.25,0.1875,0.125
            };
            // 제거용 길이들
            while(getSingleLength(leftover) == null){
                String addS = null;
                for(int i=0;i<dus.length;i+=1){
                    if(leftover > times.get(4)*dus[i]){
                        addS = sts[i];
                        leftover = leftover - (long)(times.get(4)*dus[i]);
                        break;
                    }
                }
                if(addS == null){
                    Log.e("getLoopDuration FALLED. " + du);
                    return null;
                }
                in.add(note + addS);
            }
            in.add(note + getSingleLength(leftover));
        }else{
            in.add(note + out);
        }
        String bridgeS = (useB) ? "" : "&";
        String defaultS = getSingleLength(defaultD);
        Log.d("getLoopDuration",du + " / " + Joiner.on(bridgeS).join(in));
        if(defaultS != null){
            defaultS = note + defaultS;
            for(int i=0;i<in.size();i+=1){
                String part = in.get(i);
                if(part.equalsIgnoreCase(defaultS)){
                    // L1. 하고 C1. -> L1. / C
                    in.set(i,note);
                }else{
                    if(!defaultS.endsWith(".") && part.equalsIgnoreCase(defaultS + ".")){
                        in.set(i,note+".");
                    }
                }
            }
        }
        return Joiner.on(bridgeS).join(in);
    }
}
