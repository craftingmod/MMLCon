package com.craftingmod.mml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import print.color.Ansi;
import print.color.ColoredPrinter;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by superuser on 16/2/5.
 */
public class Logger {
    private Class cls;
    private Gson g;
    private static ColoredPrinter cp;

    public Logger(Class pm){
        cls = pm;
        g = new GsonBuilder().create();
        if(cp == null){
            cp = new ColoredPrinter.Builder(1,false).build();
        }
        cp.clear();
        cp.setForegroundColor(Ansi.FColor.CYAN);
        cp.print(getCurrentTimeStamp());
        cp.print(" ");
        cp.setForegroundColor(Ansi.FColor.YELLOW);
        cp.print(cls.getSimpleName());
        cp.setForegroundColor(Ansi.FColor.GREEN);
        cp.println(" inited.");
        cp.clear();
    }
    public void d(Object ob){
        d(g.toJson(ob));
    }
    public void d(String msg){
        cp.setForegroundColor(Ansi.FColor.CYAN);
        cp.print(getCurrentTimeStamp());
        cp.print(" ");
        cp.setForegroundColor(Ansi.FColor.YELLOW);
        cp.print(cls.getSimpleName());
        cp.clear();
        cp.print(" : ");
        cp.println(msg);
    }
    public String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }
}
