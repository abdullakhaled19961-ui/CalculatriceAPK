package com.example.calculatrice;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.math.BigDecimal;
import java.math.MathContext;

public class MainActivity extends Activity {
    private TextView display;
    private String current = "0";
    private double first = 0;
    private String op = "";
    private boolean fresh = true;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(24, 28, 38));
        getWindow().setNavigationBarColor(Color.rgb(24, 28, 38));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 28, 16, 16);
        root.setBackgroundColor(Color.rgb(24, 28, 38));

        display = new TextView(this);
        display.setText("0");
        display.setTextColor(Color.WHITE);
        display.setTextSize(42);
        display.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        display.setPadding(12, 20, 12, 20);
        root.addView(display, new LinearLayout.LayoutParams(-1, 0, 1.2f));

        String[][] keys = {
            {"AC", "±", "%", "÷"},
            {"7", "8", "9", "×"},
            {"4", "5", "6", "−"},
            {"1", "2", "3", "+"},
            {"⌫", "0", ".", "="}
        };
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setRowCount(5);
        for (String[] row : keys) {
            for (String key : row) {
                Button b = new Button(this);
                b.setText(key);
                b.setTextSize(22);
                b.setAllCaps(false);
                b.setTextColor(Color.WHITE);
                int bg = ("÷×−+=".contains(key) && key.length() == 1)
                    ? Color.rgb(239, 145, 54)
                    : (key.equals("AC") || key.equals("±") || key.equals("%") || key.equals("⌫")
                        ? Color.rgb(75, 84, 101) : Color.rgb(43, 50, 65));
                b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bg));
                GridLayout.LayoutParams p = new GridLayout.LayoutParams();
                p.width = 0; p.height = 0;
                p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                p.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                p.setMargins(4, 4, 4, 4);
                grid.addView(b, p);
                b.setOnClickListener(v -> press(key));
            }
        }
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 3f));
        setContentView(root);
    }

    private void show(String s) { current = s; display.setText(s); }

    private void press(String k) {
        if (k.equals("AC")) { op = ""; first = 0; fresh = true; show("0"); return; }
        if (k.equals("⌫")) {
            if (!fresh && current.length() > 1) show(current.substring(0, current.length()-1));
            else show("0");
            return;
        }
        if (k.equals("±")) {
            if (!current.equals("0")) show(current.startsWith("-") ? current.substring(1) : "-" + current);
            return;
        }
        if (k.equals("%")) {
            try { show(fmt(Double.parseDouble(current) / 100)); } catch (Exception e) { show("خطأ"); }
            return;
        }
        if ("÷×−+".contains(k) && k.length() == 1) {
            try { first = Double.parseDouble(current); op = k; fresh = true; } catch (Exception e) { show("خطأ"); }
            return;
        }
        if (k.equals("=")) {
            if (op.isEmpty()) return;
            try {
                double second = Double.parseDouble(current), result;
                switch (op) {
                    case "+": result = first + second; break;
                    case "−": result = first - second; break;
                    case "×": result = first * second; break;
                    case "÷":
                        if (second == 0) { show("لا يمكن القسمة على صفر"); op = ""; fresh = true; return; }
                        result = first / second; break;
                    default: return;
                }
                show(fmt(result)); op = ""; fresh = true;
            } catch (Exception e) { show("خطأ"); }
            return;
        }
        if (k.equals(".")) {
            if (fresh) { show("0."); fresh = false; }
            else if (!current.contains(".")) show(current + ".");
            return;
        }
        if (fresh || current.equals("0") || current.equals("خطأ")) { show(k); fresh = false; }
        else if (current.length() < 16) show(current + k);
    }

    private String fmt(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) return "خطأ";
        return BigDecimal.valueOf(d).round(new MathContext(12)).stripTrailingZeros().toPlainString();
    }
}
