package com.phikal.regex.Games.Match;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.phikal.regex.Activities.GameActivity;
import com.phikal.regex.Games.TaskGenerationException;
import com.phikal.regex.R;
import com.phikal.regex.Utils.Task;
import com.phikal.regex.Utils.Word;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Get tasks and contribute to REDB

public class REDBGenerator extends RandomGenerator {

    public static final String stdAddr = "redb.org.uk";

    private final REDB conn;
    private final SharedPreferences prefs;
    private final Context ctx;

    public REDBGenerator(Activity activity) {
        ctx = activity;
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        conn = new REDB(prefs.getString(GameActivity.REDB_SERVER, stdAddr));
    }

    @Override
    public int calcMax(Task t, int lvl) {
        return -1;
    }

    @Override
    public Task genTask(int lvl) throws TaskGenerationException {
        try {
            return conn.requestTask(lvl);
        } catch (NoSuchElementException nsee) {
            throw new TaskGenerationException(ctx.getString(R.string.redb_error));
        }
    }

    private class REDB extends AsyncTask<Void, Void, Void> {
        final String ipaddr;
        final LinkedList<Line> lines = new LinkedList<>();
        private final Pattern
                linep = Pattern.compile("^(.)(?: (.+))?$");
        private final char
                INFO = '@', ERROR = '!', INPUT = ':',
                MATCH = '+', DMATCH = '-', ANSWR = '>';
        PrintWriter writer;
        BufferedReader reader;

        protected REDB(String ipaddr) {
            this.ipaddr = ipaddr;
        }

        @Override
        protected Void doInBackground(Void[] params) {
            try {
                Socket conn = new Socket(ipaddr, 25921);
                writer = new PrintWriter(conn.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                synchronized (this) {
                    Matcher m;
                    Line l;
                    boolean run = true;
                    while (run) {
                        m = linep.matcher(reader.readLine());
                        if (!m.matches())
                            break;
                        lines.addFirst(l = new Line(m.group(1).charAt(0), m.group(2)));

                        switch (l.type) {
                            case INPUT:
                            case ANSWR:
                                notify();
                                wait();
                                break;
                            case INFO:
                                Log.i("redb error", l.msg);
                                break;
                            case ERROR:
                                Log.e("redb error", l.msg);
                                run = false;
                                break;
                        }
                    }
                }
                conn.close();
            } catch (IOException | InterruptedException ioe) {
                ioe.printStackTrace();
            }
            return null;
        }

        private char getState() {
            return lines.getFirst().type;
        }

        public synchronized Task requestTask(int lvl) {
            if (getState() != INPUT)
                return null;
            try {
                synchronized (this) {
                    notify();
                    writer.println(lvl);
                    wait();
                }
                List<Word> match = new LinkedList<>(), dmatch = new LinkedList<>();

                for (Line l : lines) {
                    if (l.type == INPUT) break;
                    switch (l.type) {
                        case MATCH:
                            match.add(new Word(l.msg));
                            break;
                        case DMATCH:
                            dmatch.add(new Word(l.msg));
                            break;
                    }
                }

                return new Task(match, dmatch, (t, s) -> submitSolution(s));
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            return null;
        }

        public synchronized void submitSolution(String s) {
            if (getState() != ANSWR)
                return;
            synchronized (this) {
                notify();
                writer.println(s);
            }
        }

        class Line {
            public char type;
            public String msg;

            public Line(char t, String m) {
                type = t;
                msg = m;
            }
        }
    }
}
