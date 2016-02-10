package com.craftingmod.mml;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import print.color.Ansi;
import print.color.Ansi.FColor;
import print.color.Ansi.BColor;
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
        cp.setForegroundColor(FColor.CYAN);
        cp.print(getCurrentTimeStamp());
        cp.print(" ");
        cp.setForegroundColor(FColor.YELLOW);
        cp.print(cls.getSimpleName());
        cp.setForegroundColor(FColor.GREEN);
        cp.println(" inited.");
        cp.clear();
    }
    public void i(Object ob){
        i(g.toJson(ob));
    }
    public void i(long ob){
        i(ob + "");
    }
    public void i(int ob){
        i(ob + "");
    }
    public void d(String msg){
        cp.setForegroundColor(FColor.CYAN);
        cp.print(getCurrentTimeStamp());
        cp.print(" ");
        cp.setForegroundColor(FColor.YELLOW);
        cp.print(cls.getSimpleName());
        cp.clear();
        cp.print(" : ");
        cp.println(msg);
        cp.clear();
    }
    public void d(Object ob){
        d(g.toJson(ob));
    }
    public void d(long ob){
        d(ob + "");
    }
    public void d(int ob){
        d(ob + "");
    }
    public void i(String msg){
        cp.clear();
        cp.setForegroundColor(FColor.CYAN);
        cp.print(getCurrentTimeStamp());
        cp.print(" ");
        cp.setForegroundColor(FColor.YELLOW);
        cp.print(cls.getSimpleName());
        cp.clear();
        cp.print(" : ");
        cp.println(msg);
        cp.clear();
    }
    public void line(){
        cp.clear();
        cp.setBackgroundColor(BColor.GREEN);
        cp.println("");
        cp.clear();
    }
    public void e(Exception ex){
        error(ex.getClass().getCanonicalName(),Throwables.getStackTraceAsString(ex));
    }
    public void e(String s){
        error("Exception","    " + s);
    }
    private void error(String excName,String msg){
        cp.setForegroundColor(FColor.CYAN);
        cp.print(getCurrentTimeStamp() + " ");
        cp.setForegroundColor(FColor.RED);
        cp.println(excName + " at " + cls.getCanonicalName());

        cp.setBackgroundColor(BColor.WHITE);
        cp.println("");

        cp.clear();
        cp.setForegroundColor(FColor.RED);
        cp.println(msg);
        cp.setBackgroundColor(BColor.WHITE);
        cp.println("");
        cp.clear();
    }
    public String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
        Date now = new Date();
        return sdfDate.format(now);
    }
}
