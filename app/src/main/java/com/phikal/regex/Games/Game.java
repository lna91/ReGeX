package com.phikal.regex.Games;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.phikal.regex.Activitys.GameActivity;
import com.phikal.regex.Utils.Task;
import com.phikal.regex.Utils.Word;

import java.util.List;

abstract public class Game {

    public final static String
            DIFF = GameActivity.DIFF,
            TASK = "task";

    Activity activity;
    SharedPreferences prefs;

    public Game(Activity activity) {
        this.activity = activity;
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    protected static int calcVal(String regex) {
        int v = 1;
        for (char c : regex.toCharArray())
            switch (c) {
                case '.':
                case '*':
                case '+':
                case '^':
                    v += 1;
                    break;
                case '[':
                case '(':
                case '{':
                case '?':
                    v += 2;
                    break;
                case '\\':
                    v += 4;
                    break;
            }
        return v;
    }

    public static int calcScore(String regex, Task task) {
        int len = regex.length(),
                max = task.getMax(),
                right = task.getRight().size(),
                wrong = task.getWrong().size();
        return Math.round(((max - len) / 2 + 1) * (1 / ((Math.abs(right - wrong) + 1)) + 3 * calcVal(regex)));
    }

    public static int calcMax(List<Word> right, List<Word> wrong, int diff) {
        int clen_right = 0;
        for (Word s : right) clen_right += s.length();
        return (int) Math.floor(clen_right + right.size() + diff / 5);
    }

    public static int calcDiff(int score, int nscore, int games) {
        return (int) Math.round(1.2 * Math.sqrt((nscore + score * 1.1 + 1) / (games + 1)));
    }

    public Task newTask(boolean force_new) {
        int diff = prefs.getInt(DIFF, 1);
        String task = prefs.getString(TASK, null);
        Task result;

        if (task == null || force_new)
            task = (result = genTask(diff)).toString();
        else
            result = Task.parseTask(task);

        prefs.edit().putString(TASK, task).apply();
        return result;
    }

    abstract public Task genTask(int diff);

    abstract public void submit(Task task, String re);

    abstract public String getName();

    abstract public String getError();

}